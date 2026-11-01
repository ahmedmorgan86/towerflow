import {mkdir,cp,readFile,writeFile} from 'node:fs/promises';
import {existsSync} from 'node:fs';
import pg from 'pg';

await mkdir('dist',{recursive:true});
await cp('web','dist',{recursive:true});
if (existsSync('release/TowerFlow-latest.apk')) {
  await cp('release/TowerFlow-latest.apk', 'dist/TowerFlow-latest.apk');
}

// Refresh sql-bundle.mjs to keep runtime migrations self-contained
const files = ['schema.sql','002_workflows.sql','003_read_permissions.sql','004_platform.sql','005_identity.sql','006_owner_push.sql','007_master_system.sql'];
const bundle = {};
for (const f of files) {
  bundle[f] = await readFile(new URL('../db/'+f, import.meta.url), 'utf8');
}
await writeFile(new URL('../lib/sql-bundle.mjs', import.meta.url), '// Auto-generated bundle of database migrations\nexport const sqlBundle = ' + JSON.stringify(bundle, null, 2) + ';\n', 'utf8');

if (process.env.DATABASE_URL) {
  const {migrate} = await import('../lib/migrations.mjs');
  const c = new pg.Client({connectionString: process.env.DATABASE_URL.trim()});
  await c.connect();
  try {
    await migrate(c);
    console.log('Production database migrations verified.');
  } finally {
    await c.end();
  }
}

console.log('TowerFlow web built. No AI services or client secrets.');
