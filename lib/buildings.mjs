import {permission} from './finance.mjs';
import {countries} from '../web/countries.js';
const fail=(s,status=400)=>Object.assign(new Error(s),{status});
export function buildingConfig(b){
 const name=String(b.name||'').trim(),country=String(b.country||'').toUpperCase(),currency=String(b.currency||'').toUpperCase();
 let region;try{region=new Intl.DisplayNames(['en'],{type:'region'}).of(country);}catch{}
 if(!name||name.length>160||!countries.includes(country)||!region||region===country||!Intl.supportedValuesOf('currency').includes(currency))throw fail('Building name, country and valid currency required');
 return {name,country,currency};
}
export async function createBuilding(c,user,b){
 const config=buildingConfig(b),floors=b.floors||[];
 if(!Array.isArray(floors)||floors.length>100)throw fail('Maximum 100 floors');
 let total=0;const numbers=new Set();
 for(const f of floors){if(!Number.isInteger(f.floor)||f.floor< -10||f.floor>200||!Array.isArray(f.units))throw fail('Invalid floor');for(const n of f.units){const v=String(n).trim();if(!v||v.length>80||numbers.has(v))throw fail('Unit numbers must be unique');numbers.add(v);if(++total>1000)throw fail('Maximum 1000 units per request');}}
 const org=(await c.query('insert into organizations(name,country,currency) values($1,$2,$3) returning *',[config.name,config.country,config.currency])).rows[0];
 await c.query("insert into private.memberships values($1,$2,'admin') on conflict(org_id,user_id) do nothing",[org.id,user]);
 for(const a of [['1100','Cash','asset'],['1200','Receivables','asset'],['2200','Approved expenses payable','liability'],['4000','Maintenance income','income'],['5000','Operating expenses','expense']])await c.query('insert into accounts(org_id,code,name,kind,treasury_kind) values($1,$2,$3,$4,$5)',[org.id,...a,a[0]==='1100'?'cash':null]);
 const property=(await c.query('insert into properties(org_id,name,address,kind) values($1,$2,$3,$4) returning *',[org.id,config.name,String(b.address||'').slice(0,4000),'tower'])).rows[0];
 for(const f of floors)for(const n of f.units)await c.query('insert into units(org_id,property_id,name,floor) values($1,$2,$3,$4)',[org.id,property.id,String(n).trim(),String(f.floor)]);
 return {organization:org,property,units_created:total};
}
export async function configureBuilding(c,org,b){
 await permission(c,org,'manage');await c.query('select id from organizations where id=$1 for update',[org]);
 if(typeof b.archived==='boolean'){await c.query('update organizations set archived_at=case when $2 then now() else null end where id=$1',[org,b.archived]);await c.query("insert into audit_logs(org_id,actor_id,entity,entity_id,action) values($1,private.uid(),'building_lifecycle',$1,$2)",[org,b.archived?'archive':'reactivate']);return {archived:b.archived};}
 const config=buildingConfig(b);return (await c.query('update organizations set name=$2,country=$3,currency=$4 where id=$1 returning *',[org,config.name,config.country,config.currency])).rows[0];
}
