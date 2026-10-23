import pg from 'pg';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {issue,collect} from '../lib/finance.mjs';
if(!process.env.DATABASE_URL)throw new Error('DATABASE_URL required');
const c=new pg.Client({connectionString:process.env.DATABASE_URL.trim()});await c.connect();
try{
 await c.query('begin');
 const a=randomUUID(),b=randomUUID(),oa=randomUUID(),ob=randomUUID();
 await c.query("insert into private.users(id,email,password_hash,name) values($1,$2,'not-a-login-hash','Cloud test A'),($3,$4,'not-a-login-hash','Cloud test B')",[a,a+'@example.test',b,b+'@example.test']);
 await c.query("insert into organizations(id,name) values($1,'Rollback test A'),($2,'Rollback test B')",[oa,ob]);
 await c.query("insert into private.memberships values($1,$2,'admin'),($3,$4,'admin')",[oa,a,ob,b]);
 await c.query("select set_config('app.user_id',$1,true)",[a]);
 const p=(await c.query("insert into properties(org_id,name) values($1,'Cloud test tower') returning id",[oa])).rows[0];
 await c.query('set local role towerflow_client');
 assert.equal((await c.query('select count(*)::int n from properties where id=$1',[p.id])).rows[0].n,1);
 await c.query('reset role');await c.query("select set_config('app.user_id',$1,true)",[b]);await c.query('set local role towerflow_client');
 assert.equal((await c.query('select count(*)::int n from properties where id=$1',[p.id])).rows[0].n,0);
 await c.query('reset role');await c.query("select set_config('app.user_id',$1,true)",[a]);
 for(const [code,kind]of [['1100','asset'],['1200','asset'],['4000','income'],['5000','expense']])await c.query('insert into accounts(org_id,code,name,kind) values($1,$2,$2,$3)',[oa,code,kind]);
 const party=(await c.query("insert into parties(org_id,name,kind) values($1,'Cloud tenant','tenant') returning id",[oa])).rows[0];
 const invoice=(await c.query("insert into invoices(org_id,party_id,number,description,amount,due_date) values($1,$2,'CLOUD-ROLLBACK','Cloud test',100.25,current_date) returning id",[oa,party.id])).rows[0];
 await issue(c,oa,a,invoice.id);await collect(c,oa,a,invoice.id,'40.10','CLOUD-1');await collect(c,oa,a,invoice.id,'60.15','CLOUD-2');
 assert.equal((await c.query('select status from invoices where id=$1',[invoice.id])).rows[0].status,'paid');
 const balance=(await c.query('select sum(debit)-sum(credit) delta from journal_lines where org_id=$1',[oa])).rows[0];assert.equal(Number(balance.delta),0);
 console.log('PASS: Neon live RLS isolation, invoice issue, partial/final receipts and ledger balance.');
}finally{await c.query('rollback');await c.end();console.log('Cloud test transaction rolled back; no test data retained.');}
