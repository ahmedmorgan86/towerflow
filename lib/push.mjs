import {GoogleAuth} from 'google-auth-library';
import {database} from './db.mjs';
import {hashToken} from './auth.mjs';
let auth;
const fail=s=>Object.assign(new Error(s),{status:400});
export function pushReady(){return !!process.env.FIREBASE_SERVICE_ACCOUNT;}
export async function registerDevice(c,user,token,remove=false){
 if(typeof token!=='string'||!/^[A-Za-z0-9:._-]{32,4096}$/.test(token))throw fail('Invalid device token');
 const hash=hashToken(token),old=(await c.query('select id,user_id from private.push_devices where token_hash=$1 for update',[hash])).rows[0];
 if(old&&(remove||old.user_id!==user)){if(remove&&old.user_id!==user)return {ok:true};await c.query("update private.push_outbox set status='cancelled' where device_id=$1 and status in ('pending','processing')",[old.id]);}
 if(remove){await c.query('update private.push_devices set enabled=false where token_hash=$1 and user_id=$2',[hash,user]);return {ok:true};}
 const id=(await c.query("insert into private.push_devices(user_id,token_hash,token) values($1,$2,$3) on conflict(token_hash) do update set user_id=excluded.user_id,enabled=true,updated_at=now() returning id",[user,hash,token])).rows[0].id;
 await c.query('update private.push_devices set enabled=false where user_id=$1 and id not in(select id from private.push_devices where user_id=$1 and enabled order by updated_at desc limit 5)',[user]);return {ok:true,id};
}
export async function enqueuePush(c,org,notification,user){const r=await c.query('insert into private.push_outbox(org_id,notification_id,device_id) select $1,$2,id from private.push_devices where user_id=$3 and enabled on conflict(notification_id,device_id) do nothing returning id',[org,notification,user]);return r.rows.length;}
export async function pushStatus(c,org){const r=(await c.query('select status,count(*)::int count from private.push_outbox where org_id=$1 group by status',[org])).rows;return {configured:pushReady(),...Object.fromEntries(r.map(x=>[x.status,x.count]))};}
export async function sendFCM(job){
 const credentials=JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);auth??=new GoogleAuth({credentials,scopes:['https://www.googleapis.com/auth/firebase.messaging']});const access=await auth.getAccessToken();
 const split=job.message.indexOf('\n\n'),title=(split>=0?job.message.slice(0,split):'TowerFlow').slice(0,120),body=(split>=0?job.message.slice(split+2):job.message).slice(0,600);
 const r=await fetch('https://fcm.googleapis.com/v1/projects/'+encodeURIComponent(credentials.project_id)+'/messages:send',{method:'POST',headers:{Authorization:'Bearer '+access,'Content-Type':'application/json'},signal:AbortSignal.timeout(10000),body:JSON.stringify({message:{token:job.token,notification:{title,body},data:{notification_id:job.notification_id,org_id:job.org_id},android:{priority:'high',ttl:'86400s',notification:{channel_id:'towerflow_residents',tag:job.notification_id,visibility:'PRIVATE',default_sound:true}}}})});
 if(r.ok)return {ok:true};const result=await r.json().catch(()=>({}));const unregistered=result.error?.details?.some(x=>x.errorCode==='UNREGISTERED');return {ok:false,permanent:!!unregistered,error:unregistered?'UNREGISTERED':'FCM_'+r.status};
}
export async function processPush(org,{sender=sendFCM,budgetMs=250000}={}){
 if(sender===sendFCM&&!pushReady())return {configured:false};const deadline=Date.now()+budgetMs;let sent=0;
 while(Date.now()<deadline){const jobs=await database().transaction(async c=>{const rows=(await c.query("select q.id,q.notification_id,q.org_id,q.device_id,q.attempts,d.token,n.message from private.push_outbox q join private.push_devices d on d.id=q.device_id and d.enabled join notifications n on n.id=q.notification_id and n.user_id=d.user_id where q.org_id=$1 and ((q.status='pending' and q.available_at<=now()) or(q.status='processing' and q.lease_until<now())) and q.attempts<5 order by q.available_at for update of q skip locked limit 20",[org])).rows;for(const row of rows)await c.query("update private.push_outbox set status='processing',attempts=attempts+1,lease_until=now()+interval '30 seconds' where id=$1",[row.id]);return rows;});
 if(!jobs.length)break;
 await Promise.all(jobs.map(async job=>{let result;try{result=await sender(job);}catch{result={ok:false,error:'NETWORK_ERROR'};}
 await database().transaction(async c=>{if(result.ok){sent++;await c.query("update private.push_outbox set status='sent',delivered_at=now(),lease_until=null,last_error=null where id=$1 and status='processing'",[job.id]);}else{const attempts=job.attempts+1;await c.query("update private.push_outbox set status=$2,last_error=$3,available_at=now()+make_interval(secs=>$4),lease_until=null where id=$1 and status='processing'",[job.id,result.permanent||attempts>=5?'failed':'pending',result.error||'PROVIDER_ERROR',Math.min(180,5**attempts)]);if(result.permanent)await c.query('update private.push_devices set enabled=false where id=$1',[job.device_id]);}});
 }));
 }return {configured:true,sent};
}
