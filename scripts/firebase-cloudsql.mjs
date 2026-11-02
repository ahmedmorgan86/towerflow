/**
 * Firebase / Google Cloud SQL PostgreSQL Migration & Verification Tool
 * Project: heroic-psyche-512522-v8
 * 
 * Usage:
 *   # Verify connection and run migrations:
 *   node scripts/firebase-cloudsql.mjs
 * 
 *   # Migrate data from existing database:
 *   SOURCE_DB_URL=... TARGET_DB_URL=... node scripts/firebase-cloudsql.mjs --migrate-data
 */

import pg from 'pg';
import {migrate} from '../lib/migrations.mjs';

const connectionString = (process.env.TARGET_DB_URL || process.env.DATABASE_URL || '').trim();

if (!connectionString) {
  console.log(`
========================================================================
🔥 Firebase / Google Cloud SQL Configuration Guide
Project ID: heroic-psyche-512522-v8
========================================================================

To connect TowerFlow to Google Cloud SQL for PostgreSQL under your Firebase project:

1. In Google Cloud Console (under project heroic-psyche-512522-v8):
   - Go to Cloud SQL -> Instances -> Create Instance -> PostgreSQL (v15 or v16).
   - Set instance ID (e.g., 'towerflow-db') and an admin password.
   - Under Connections:
     * Add Authorized Networks (your IP or Vercel outbound IPs: 0.0.0.0/0 with password & SSL enabled).
     * Or use Google Cloud SQL Auth Proxy.

2. Create Database & User:
   - Database name: 'towerflow'
   - User: 'towerflow' with a secure password.

3. Set your DATABASE_URL in .env.local:
   DATABASE_URL=postgres://towerflow:<PASSWORD>@<CLOUD_SQL_PUBLIC_IP>:5432/towerflow?sslmode=require

4. Run this script to automatically install all 7 migrations and verify:
   npm run db:setup
========================================================================
`);
  process.exit(0);
}

console.log('Connecting to Firebase Cloud SQL PostgreSQL database...');
const client = new pg.Client({
  connectionString,
  ssl: { rejectUnauthorized: false }
});

try {
  await client.connect();
  console.log('✓ Successfully connected to PostgreSQL engine.');

  console.log('Applying all migrations (schema.sql -> 007_master_system.sql)...');
  await migrate(client);
  console.log('✓ All 7 migrations installed and verified.');

  const res = await client.query(`
    select 
      (select count(*) from organizations) as orgs,
      (select count(*) from properties) as properties,
      (select count(*) from units) as units,
      (select count(*) from accounts) as accounts
  `);
  console.log('Database Status:', res.rows[0]);

  // Optional Data Migration from Source DB
  if (process.argv.includes('--migrate-data') && process.env.SOURCE_DB_URL) {
    console.log('\nMigrating data from SOURCE_DB_URL to Firebase Cloud SQL...');
    const sourceClient = new pg.Client({
      connectionString: process.env.SOURCE_DB_URL.trim(),
      ssl: { rejectUnauthorized: false }
    });
    await sourceClient.connect();

    const tables = [
      'organizations', 'private.users', 'private.memberships', 'accounts',
      'properties', 'money_accounts', 'units', 'parties', 'leases',
      'ownerships', 'journals', 'journal_lines', 'invoices', 'invoice_lines',
      'receipts', 'expenses', 'tickets', 'ticket_comments', 'assets',
      'technicians', 'announcements', 'notifications', 'fines', 'credit_balances',
      'payment_proofs', 'vendor_contracts', 'budgets', 'support_tickets'
    ];

    for (const table of tables) {
      try {
        const rows = (await sourceClient.query(`select * from ${table}`)).rows;
        if (rows.length > 0) {
          console.log(`- Migrating ${rows.length} records from ${table}...`);
          for (const row of rows) {
            const cols = Object.keys(row);
            const vals = Object.values(row);
            const placeholders = cols.map((_, i) => '$' + (i + 1)).join(',');
            await client.query(
              `insert into ${table} (${cols.map(c => '"' + c + '"').join(',')}) values (${placeholders}) on conflict do nothing`,
              vals
            );
          }
        }
      } catch (err) {
        console.warn(`  [Notice on ${table}]:`, err.message);
      }
    }
    await sourceClient.end();
    console.log('✓ Data migration complete.');
  }

  console.log('\n✅ Firebase Cloud SQL PostgreSQL is ready for TowerFlow.');
} catch (e) {
  console.error('❌ Cloud SQL connection or migration failed:', e.message);
  process.exit(1);
} finally {
  await client.end();
}
