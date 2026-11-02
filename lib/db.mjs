import pg from 'pg';
import {migrate} from './migrations.mjs';

let pool;
let migrationDone = false;
let migrationRunning = null;

async function checkAndMigrate(c) {
  if (migrationDone) return;
  if (migrationRunning) return migrationRunning;
  migrationRunning = (async () => {
    try {
      await migrate(c);
      migrationDone = true;
    } catch (e) {
      console.error('[Auto-Migration Warning]:', e.message);
    } finally {
      migrationRunning = null;
    }
  })();
  return migrationRunning;
}

export function database(){
  if(globalThis.towerflowLocalDB) return globalThis.towerflowLocalDB;
  const connectionUrl = (process.env.TARGET_DB_URL || process.env.DATABASE_URL || '').trim();
  if(!connectionUrl && !process.env.CLOUDSQL_CONNECTION_NAME) {
    throw Object.assign(new Error('Database is not configured'),{status:503});
  }

  if (!pool) {
    const config = {
      max: Number(process.env.DB_POOL_MAX || 5),
      connectionTimeoutMillis: 10000,
      idleTimeoutMillis: 30000
    };

    if (process.env.CLOUDSQL_CONNECTION_NAME) {
      // Unix domain socket for Google Cloud SQL (e.g. Cloud Run / App Engine)
      config.host = `/cloudsql/${process.env.CLOUDSQL_CONNECTION_NAME}`;
      config.user = process.env.DB_USER || 'towerflow';
      config.password = process.env.DB_PASSWORD || '';
      config.database = process.env.DB_NAME || 'towerflow';
    } else {
      config.connectionString = connectionUrl;
      // Configure SSL for Cloud SQL / Neon
      if (connectionUrl.includes('sslmode=') || !connectionUrl.includes('localhost')) {
        config.ssl = process.env.DB_SSL_REJECT_UNAUTHORIZED === 'true' ? true : { rejectUnauthorized: false };
      }
    }

    pool = new pg.Pool(config);
  }

  return {
    async transaction(fn){
      const c = await pool.connect();
      try{
        await checkAndMigrate(c);
        await c.query('begin');
        const r = await fn(c);
        await c.query('commit');
        return r;
      }catch(e){
        await c.query('rollback');
        throw e;
      }finally{
        c.release();
      }
    }
  };
}

export async function asUser(userId,fn){
  return database().transaction(async c=>{
    await c.query("select set_config('app.user_id',$1,true)",[userId]);
    await c.query('set local role towerflow_client');
    return fn(c);
  });
}
