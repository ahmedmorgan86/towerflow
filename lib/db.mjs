import pg from 'pg';
let pool;
export function database(){
 if(globalThis.towerflowLocalDB) return globalThis.towerflowLocalDB;
 if(!process.env.DATABASE_URL) throw Object.assign(new Error('Database is not configured'),{status:503});
 pool ??= new pg.Pool({connectionString:process.env.DATABASE_URL.trim(),max:3});
 return {async transaction(fn){const c=await pool.connect();try{await c.query('begin');const r=await fn(c);await c.query('commit');return r;}catch(e){await c.query('rollback');throw e;}finally{c.release();}}};
}
export async function asUser(userId,fn){return database().transaction(async c=>{
 await c.query("select set_config('app.user_id',$1,true)",[userId]);
 await c.query('set local role towerflow_client');
 return fn(c);
});}
