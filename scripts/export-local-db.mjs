import { PGlite } from '@electric-sql/pglite';
import { writeFile, mkdir } from 'node:fs/promises';

async function exportAll() {
  const db = new PGlite('local-data/postgres');
  const tablesRes = await db.query(`
    SELECT table_schema, table_name 
    FROM information_schema.tables 
    WHERE table_schema IN ('public', 'private')
    ORDER BY table_schema, table_name;
  `);

  const dump = {
    exported_at: new Date().toISOString(),
    schemas: {}
  };

  for (const {table_schema, table_name} of tablesRes.rows) {
    try {
      const rowsRes = await db.query(`SELECT * FROM ${table_schema}.${table_name}`);
      if (!dump.schemas[table_schema]) dump.schemas[table_schema] = {};
      dump.schemas[table_schema][table_name] = rowsRes.rows;
      if (rowsRes.rows.length > 0) {
        console.log(`Exported ${table_schema}.${table_name}: ${rowsRes.rows.length} rows`);
      }
    } catch {
      // ignore
    }
  }
  await db.close();
  await mkdir('data', { recursive: true });
  await writeFile('data/local-backup.json', JSON.stringify(dump, null, 2), 'utf8');
  console.log('Successfully saved data/local-backup.json');
}

exportAll().catch(console.error);
