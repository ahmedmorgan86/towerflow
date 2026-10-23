import pg from 'pg';
import {readFile} from 'node:fs/promises';
if(!process.env.DATABASE_URL) throw new Error('Set DATABASE_URL securely before database setup');
const client = new pg.Client({connectionString:process.env.DATABASE_URL});
await client.connect();
try {
 const initial = await client.query("select to_regclass('public.organizations') is not null as installed");
 if(!initial.rows[0].installed) await client.query(await readFile(new URL('../db/schema.sql',import.meta.url),'utf8'));
 const workflow = await client.query("select to_regprocedure('private.sync_occupancy()') is not null as installed");
 if(!workflow.rows[0].installed) await client.query(await readFile(new URL('../db/002_workflows.sql',import.meta.url),'utf8'));
 console.log('Schema and workflow migration verified');
}
finally {await client.end();}
