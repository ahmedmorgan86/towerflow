/**
 * Firebase / Google Cloud SQL PostgreSQL Migration & Verification Tool
 * Project: heroic-psyche-512522-v8
 * 
 * Usage:
 *   # Verify connection and run migrations:
 *   node scripts/firebase-cloudsql.mjs
 * 
 *   # Migrate data from local PGlite backup:
 *   TARGET_DB_URL=... node scripts/firebase-cloudsql.mjs --migrate-local
 * 
 *   # Migrate data from remote PostgreSQL URL:
 *   SOURCE_DB_URL=... TARGET_DB_URL=... node scripts/firebase-cloudsql.mjs --migrate-data
 */

import pg from 'pg';
import {existsSync} from 'node:fs';
import {readFile} from 'node:fs/promises';
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
   npm run db:cloudsql
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

  console.log('Applying all migrations (schema.sql -> 008_units_ownership_and_fixes.sql)...');
  await migrate(client);
  console.log('✓ All 8 migrations installed and verified.');

  const res = await client.query(`
    select 
      (select count(*) from organizations) as orgs,
      (select count(*) from properties) as properties,
      (select count(*) from units) as units,
      (select count(*) from accounts) as accounts
  `);
  console.log('Database Status:', res.rows[0]);

  // Data Migration from Local Backup JSON
  if (process.argv.includes('--migrate-local') || process.argv.includes('--import-backup')) {
    const backupPath = 'data/local-backup.json';
    if (existsSync(backupPath)) {
      console.log(`\nImporting local backup from ${backupPath} to Firebase Cloud SQL...`);
      const dump = JSON.parse(await readFile(backupPath, 'utf8'));
      const schemas = dump.schemas || {};

      const tableOrder = [
        'private.users', 'public.organizations', 'private.memberships', 'private.platform_owners',
        'public.accounts', 'public.properties', 'public.money_accounts', 'public.parties', 'public.units',
        'public.leases', 'public.ownerships', 'public.assets', 'public.technicians', 'public.tickets',
        'public.ticket_comments', 'public.maintenance_plans', 'public.recurring_charges',
        'public.journals', 'public.journal_lines', 'public.invoices', 'public.invoice_lines',
        'public.receipts', 'public.expenses', 'public.payment_proofs', 'public.fines',
        'public.credit_balances', 'public.credit_transactions', 'public.vendor_contracts',
        'public.contract_versions', 'public.budgets', 'public.budget_items', 'public.expense_distributions',
        'public.distribution_shares', 'public.account_reconciliations', 'public.management_transfers',
        'public.delegations', 'public.support_tickets', 'public.support_messages', 'public.support_grants',
        'public.announcements', 'public.notifications', 'public.audit_logs', 'private.sessions',
        'private.login_limits', 'private.oauth_accounts', 'private.oauth_states', 'private.push_devices',
        'private.push_outbox', 'private.document_sequences'
      ];

      // Flatten and sort by dependency order
      const allEntries = [];
      for (const [schemaName, tables] of Object.entries(schemas)) {
        for (const [tableName, rows] of Object.entries(tables)) {
          if (Array.isArray(rows) && rows.length > 0) {
            allEntries.push({ fullName: `${schemaName}.${tableName}`, schemaName, tableName, rows });
          }
        }
      }

      allEntries.sort((a, b) => {
        const idxA = tableOrder.indexOf(a.fullName);
        const idxB = tableOrder.indexOf(b.fullName);
        return (idxA === -1 ? 999 : idxA) - (idxB === -1 ? 999 : idxB);
      });

      for (const { fullName, schemaName, tableName, rows } of allEntries) {
        console.log(`- Importing ${rows.length} records into ${fullName}...`);
        for (const row of rows) {
          const cols = Object.keys(row);
          const vals = Object.values(row);
          const placeholders = cols.map((_, i) => '$' + (i + 1)).join(',');
          try {
            await client.query(
              `insert into ${schemaName}.${tableName} (${cols.map(c => '"' + c + '"').join(',')}) values (${placeholders}) on conflict do nothing`,
              vals
            );
          } catch (e) {
            console.warn(`  [Warning on ${fullName}]:`, e.message);
          }
        }
      }
      console.log('✓ Local data migration to Firebase Cloud SQL complete.');
    } else {
      console.warn(`⚠ ${backupPath} not found. Run 'node scripts/export-local-db.mjs' first.`);
    }
  }

  // Data Migration from Source DB URL
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
