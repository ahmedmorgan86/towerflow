import pg from 'pg';import {migrate} from '../lib/migrations.mjs';
if(!process.env.DATABASE_URL)throw new Error('DATABASE_URL required');
const c=new pg.Client({connectionString:process.env.DATABASE_URL.trim()});await c.connect();
try{await migrate(c);console.log('All platform migrations verified');}finally{await c.end();}
