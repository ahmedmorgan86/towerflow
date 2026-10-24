import {hashToken} from './auth.mjs';
import {enqueuePush} from './push.mjs';
import {randomUUID} from 'node:crypto';
import {permission} from './finance.mjs';
const fail=(s,status=400)=>Object.assign(new Error(s),{status});
export async function recipients(c,org,property,unitIds=null){
 await permission(c,org,'manage');if(!/^[a-f0-9-]{36}$/.test(property||''))throw fail('Choose a building');
 if(!(await c.query('select 1 from properties where id=$1 and org_id=$2',[property,org])).rows.length)throw fail('Building not found',404);
 if(unitIds!==null){if(!Array.isArray(unitIds)||!unitIds.length||unitIds.length>500||unitIds.some(x=>!/^[a-f0-9-]{36}$/.test(x))||new Set(unitIds).size!==unitIds.length)throw fail('Choose valid units');const n=(await c.query('select count(*)::int n from units where org_id=$1 and property_id=$2 and id=any($3::uuid[])',[org,property,unitIds])).rows[0].n;if(n!==unitIds.length)throw fail('Units must belong to the selected building',404);}
 const rows=(await c.query("select distinct p.id,p.user_id,m.user_id member_id from leases l join units u on u.id=l.unit_id and u.org_id=l.org_id join parties p on p.id=l.party_id and p.org_id=l.org_id left join private.memberships m on m.user_id=p.user_id and m.org_id=l.org_id where l.org_id=$1 and u.property_id=$2 and l.status='active' and l.start_date<=current_date and l.end_date>=current_date and ($3::uuid[] is null or u.id=any($3))",[org,property,unitIds])).rows;
 const users=[...new Set(rows.filter(x=>x.member_id).map(x=>x.user_id))];return {users,registered:users.length,unlinked:rows.filter(x=>!x.member_id).length};
}
export async function broadcast(c,org,user,b){
 await permission(c,org,'manage');const title=String(b.title||'').trim(),body=String(b.body||'').trim();
 if(!title||title.length>160||!body||body.length>4000||! /^[a-f0-9-]{36}$/.test(b.request_id||''))throw fail('Title, message and unique request ID required');
 const unitIds=b.scope==='units'?b.unit_ids:null;if(b.scope==='units'&&(!Array.isArray(unitIds)||!unitIds.length))throw fail('Choose at least one unit');const signature=hashToken(JSON.stringify({body,units:unitIds?[...unitIds].sort():null}));
 await c.query('select id from organizations where id=$1 for update',[org]);
 const previous=(await c.query("select action from audit_logs where id=$1 and org_id=$2 and entity='resident_broadcasts'",[b.request_id,org])).rows[0];
 if(previous){const saved=JSON.parse(previous.action);if(saved.actor_id!==user||saved.property_id!==b.property_id||saved.title!==title||saved.signature!==signature)throw fail('Request ID already used',409);return {...saved,repeated:true};}
 const target=await recipients(c,org,b.property_id,unitIds);if(!target.registered)throw fail('No registered residents with current leases; link residents to their tenant accounts first',409);
 const id=randomUUID();let queued=0;for(const recipient of target.users){const n=(await c.query('insert into notifications(org_id,user_id,message) values($1,$2,$3) returning id',[org,recipient,title+'\n\n'+body])).rows[0];queued+=await enqueuePush(c,org,n.id,recipient);}
 const result={id,actor_id:user,property_id:b.property_id,title,sent:target.registered,unlinked:target.unlinked,channel:'in_app',queued,signature,scope:b.scope==='units'?'units':'building',unit_count:unitIds?.length||null};
 await c.query("insert into audit_logs(id,org_id,actor_id,entity,entity_id,action) values($1,$2,$3,'resident_broadcasts',$4,$5)",[b.request_id,org,user,id,JSON.stringify(result)]);return result;
}
