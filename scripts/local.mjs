import {PGlite} from '@electric-sql/pglite';import {mkdir} from 'node:fs/promises';import {migrate} from '../lib/migrations.mjs';
await mkdir('local-data',{recursive:true});const db=new PGlite('local-data/postgres');await db.waitReady;await migrate(db);
globalThis.towerflowLocalDB={transaction:fn=>db.transaction(tx=>fn({query:(sql,params)=>tx.query(sql,params)}))};
console.log('Local PostgreSQL ready at port '+(process.env.PORT||3000));await import('../server.mjs');
