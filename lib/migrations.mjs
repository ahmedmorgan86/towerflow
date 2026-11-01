import {readFile} from 'node:fs/promises';
import {sqlBundle} from './sql-bundle.mjs';

export const migrations=[
  ['schema.sql',"to_regclass('public.organizations')"],
  ['002_workflows.sql',"to_regprocedure('private.sync_occupancy()')"],
  ['003_read_permissions.sql',"to_regprocedure('private.can_read(uuid,text)')"],
  ['004_platform.sql',"to_regclass('public.accounting_periods')"],
  ['005_identity.sql',"to_regclass('private.oauth_accounts')"],
  ['006_owner_push.sql',"to_regclass('private.platform_owners')"],
  ['007_master_system.sql',"to_regclass('public.money_accounts')"]
];

export async function migrate(client){
  for(const [file,marker] of migrations){
    const r=await client.query(`select ${marker} is not null installed`);
    if(!r.rows[0].installed){
      let sql;
      try{
        sql = await readFile(new URL('../db/'+file,import.meta.url),'utf8');
      }catch{
        sql = sqlBundle[file];
      }
      if(!sql) throw new Error('Migration SQL not found: '+file);
      if(client.exec)await client.exec(sql);
      else await client.query(sql);
    }
  }
}
