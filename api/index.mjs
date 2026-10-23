import {database,asUser} from '../lib/db.mjs';
import {catalog,readable,clean,cents} from '../lib/catalog.mjs';
import {passwordHash,passwordMatches,createSession,session,tokenFrom,hashToken,throttle} from '../lib/auth.mjs';
import {permission,journal,issue,collect,approve} from '../lib/finance.mjs';
const fail=(message,status=400)=>Object.assign(new Error(message),{status});
async function body(req){if(req.body&&typeof req.body==='object')return req.body;let data='';for await(const chunk of req){data+=chunk;if(Buffer.byteLength(data)>64000)throw fail('Request too large',413);}try{return data?JSON.parse(data):{};}catch{throw fail('Invalid JSON');}}
const cookie=(token,secure)=>`tf_session=${token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=${token?43200:0}${secure?'; Secure':''}`;
async function context(user,fn){return database().transaction(async c=>{await c.query("select set_config('app.user_id',$1,true)",[user.id]);return fn(c);});}
export default async function handler(req,res){
 res.setHeader('Content-Type','application/json; charset=utf-8');res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');
 const send=(value,status=200)=>{res.statusCode=status;res.end(JSON.stringify(value));};
 try{
 const u=new URL(req.url,'http://localhost');const route=u.pathname.replace(/^\/api\/?/,'').replace(/\/$/,'');const method=req.method;
 if(route==='health'){if(method!=='GET')throw fail('Method not allowed',405);const configured=!!(process.env.DATABASE_URL||globalThis.towerflowLocalDB);if(configured)await database().transaction(c=>c.query('select 1'));return send({service:'TowerFlow',database:configured?'ready':'unconfigured',ai:false},configured?200:503);}
 if(!['GET','HEAD'].includes(method)){
 const expected=process.env.APP_ORIGIN||(process.env.VERCEL_URL?'https://'+process.env.VERCEL_URL:'http://localhost:3000');
 if(req.headers.origin&&req.headers.origin!==expected)throw fail('Origin not allowed',403);
 if(!req.headers.origin&&!req.headers.authorization&&req.headers['x-towerflow-client']!=='android'&&process.env.NODE_ENV==='production')throw fail('Origin required',403);
 }
 if(['auth/register','auth/login'].includes(route)&&method==='POST'){
 const b=await body(req);const email=String(b.email||'').trim().toLowerCase();const pass=String(b.password||'');
 if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)||email.length>254||pass.length<12||pass.length>128)throw fail('Valid email and password of 12–128 characters required');
 await database().transaction(c=>throttle(c,email));
 const result=await database().transaction(async c=>{
 let user;
 if(route==='auth/register'){
 if(!String(b.name||'').trim()||!String(b.organization||'').trim())throw fail('Name and organization required');
 user=(await c.query('insert into private.users(email,password_hash,name) values($1,$2,$3) returning id,email,name',[email,await passwordHash(pass),String(b.name).slice(0,160)])).rows[0];
 const org=(await c.query('insert into organizations(name) values($1) returning id',[String(b.organization).slice(0,160)])).rows[0];
 await c.query("insert into private.memberships values($1,$2,'admin')",[org.id,user.id]);
 await c.query("select set_config('app.user_id',$1,true)",[user.id]);
 for(const a of [['1100','Cash','asset'],['1200','Receivables','asset'],['4000','Rental income','income'],['5000','Operating expenses','expense']])await c.query('insert into accounts(org_id,code,name,kind) values($1,$2,$3,$4)',[org.id,...a]);
 }else{
 const row=(await c.query('select * from private.users where email=$1',[email])).rows[0];
 const valid=await passwordMatches(pass,row?.password_hash||'00000000000000000000000000000000:'+ '00'.repeat(64));
 if(!row||!valid)throw fail('Invalid email or password',401);user={id:row.id,email:row.email,name:row.name};
 }
 return {user,token:await createSession(c,user.id)};
 });res.setHeader('Set-Cookie',cookie(result.token,!!process.env.VERCEL||process.env.APP_ORIGIN?.startsWith('https:')));return send(req.headers['x-towerflow-client']==='android'?result:{user:result.user},route==='auth/register'?201:200);
 }
 const user=await session(req);if(!user)throw fail('Sign in required',401);
 if(route==='auth/logout'&&method==='POST'){await database().transaction(c=>c.query('delete from private.sessions where token_hash=$1',[hashToken(tokenFrom(req))]));res.setHeader('Set-Cookie',cookie('',!!process.env.VERCEL));return send({ok:true});}
 if(route==='me'&&method==='GET'){const orgs=await database().transaction(c=>c.query('select o.*,m.role from organizations o join private.memberships m on m.org_id=o.id where m.user_id=$1 order by o.created_at',[user.id]));return send({user,organizations:orgs.rows});}
 const org=u.searchParams.get('org');if(!/^[a-f0-9-]{36}$/.test(org||''))throw fail('Organization required');
 if(route==='members'&&method==='GET')return send(await context(user,async c=>{await permission(c,org,'manage');return (await c.query('select u.id,u.name,u.email,m.role from private.memberships m join private.users u on u.id=m.user_id where m.org_id=$1',[org])).rows;}));
 if(route==='members'&&method==='POST'){
 const b=await body(req);if(!['admin','manager','accountant','technician','resident','viewer'].includes(b.role))throw fail('Invalid role');
 return send(await context(user,async c=>{if(!(await c.query("select 1 from private.memberships where org_id=$1 and user_id=$2 and role='admin'",[org,user.id])).rows.length)throw fail('Administrator required',403);
 const target=(await c.query('select id from private.users where email=$1',[String(b.email).trim().toLowerCase()])).rows[0];if(!target)throw fail('User must create an account first',404);if(target.id===user.id)throw fail('Cannot change your own role');
 await c.query('insert into private.memberships values($1,$2,$3) on conflict(org_id,user_id) do update set role=excluded.role',[org,target.id,b.role]);
 if(b.party_id){const changed=await c.query('update parties set user_id=$1 where org_id=$2 and id=$3 returning id',[target.id,org,b.party_id]);if(!changed.rows.length)throw fail('Party not found',404);}
 await c.query("insert into audit_logs(org_id,actor_id,entity,entity_id,action) values($1,$2,'memberships',$3,$4)",[org,user.id,target.id,'role:'+b.role]);return {ok:true};}));
 }
 if(route==='organizations'&&method==='POST'){const b=await body(req);if(!String(b.name||'').trim())throw fail('Name required');return send(await context(user,async c=>{const o=(await c.query('insert into organizations(name) values($1) returning *',[String(b.name).slice(0,160)])).rows[0];await c.query("insert into private.memberships values($1,$2,'admin')",[o.id,user.id]);for(const a of [['1100','Cash','asset'],['1200','Receivables','asset'],['4000','Rental income','income'],['5000','Operating expenses','expense']])await c.query('insert into accounts(org_id,code,name,kind) values($1,$2,$3,$4)',[o.id,...a]);return o;}),201);}
 const [entity,id,action]=route.split('/');
 if(entity==='notifications'&&id&&action==='read'&&method==='POST')return send(await context(user,async c=>{const r=await c.query('update notifications set read_at=now() where id=$1 and org_id=$2 and user_id=$3 returning id',[id,org,user.id]);if(!r.rows.length)throw fail('Notification not found',404);return {ok:true};}));
 if(entity==='journals'&&method==='POST')return send(await context(user,async c=>{await permission(c,org,'finance');const b=await body(req);return {id:await journal(c,org,user.id,String(b.description||'Journal').slice(0,4000),b.lines)};}),201);
 if(entity==='invoices'&&id&&action&&method==='POST'){const b=await body(req);return send(await context(user,c=>action==='issue'?issue(c,org,user.id,id):action==='collect'?collect(c,org,user.id,id,b.amount,b.reference):Promise.reject(fail('Unknown action',404))));}
 if(entity==='expenses'&&id&&action==='decide'&&method==='POST'){const b=await body(req);return send(await context(user,c=>approve(c,org,user.id,id,b.decision)));}
 if(readable.includes(entity)&&method==='GET')return send(await asUser(user.id,async c=>(await c.query(`select * from public.${entity} where org_id=$1 ${id?'and id=$2':''} limit 500`,id?[org,id]:[org])).rows));
 if(catalog[entity]&&['POST','PATCH'].includes(method)&&!action){
 if(id&&['invoices','expenses'].includes(entity))throw fail('Financial documents cannot be edited here',405);
 const data=clean(entity,await body(req),method==='PATCH');const keys=Object.keys(data);if(!keys.length)throw fail('No fields supplied');
 if(data.amount)cents(data.amount);
 const work=async c=>{
 if(['invoices','expenses'].includes(entity))await permission(c,org,'finance');
 if(entity==='tickets'&&id&&data.status){
 const previous=(await c.query('select status from tickets where org_id=$1 and id=$2 for update',[org,id])).rows[0];
 if(!previous)throw fail('Ticket not found',404);
 const next={open:['assigned','in_progress'],assigned:['in_progress','open'],in_progress:['resolved','assigned'],resolved:['closed','in_progress'],closed:[]};
 if(data.status!==previous.status&&!next[previous.status].includes(data.status))throw fail('Invalid ticket status transition',409);
 if(data.status==='closed')await permission(c,org,'manage');
 }
 const r=id?await c.query(`update public.${entity} set ${keys.map((k,i)=>`${k}=$${i+3}`).join(',')} where org_id=$1 and id=$2 returning *`,[org,id,...Object.values(data)]):await c.query(`insert into public.${entity}(org_id,${keys.join(',')}) values($1,${keys.map((_,i)=>'$'+(i+2)).join(',')}) returning *`,[org,...Object.values(data)]);
 if(!r.rows.length)throw fail('Record not found or permission denied',404);return r.rows[0];};
 return send(await (['invoices','expenses'].includes(entity)?context(user,work):asUser(user.id,work)),id?200:201);
 }
 throw fail('Not found',404);
 }catch(e){const status=e.status||({'23505':409,'23503':400,'23514':400,'22P02':400,'22007':400,'42501':403,'P0001':400})[e.code]||500;send({error:status===500?'Internal error':e.message,code:e.code||null},status);}
}
