import {test,before,after} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {PGlite} from '@electric-sql/pglite';
import {createTowerWithZeroBalance,generateUnitsBatch,transferWithinTower,submitPaymentProof,reviewPaymentProof,createExpenseDistribution,collectMulti,applyCreditToInvoice,imposeFine,exemptFine,initiateTowerTransfer,respondTowerTransfer,archiveTower,deleteTowerSafely,exportTowerBackup,importTowerBackup,reconcileMoneyAccount,reopenPeriod} from '../lib/tower-operations.mjs';
import {createSupportTicket,addSupportMessage,requestSupportAccess,approveSupportAccess,revokeSupportAccess} from '../lib/support-system.mjs';
import {issue} from '../lib/finance.mjs';
import {createPeriod,closePeriod} from '../lib/platform.mjs';

let db, orgId, adminId, managerId, residentId, tower1Id, tower2Id, partyId, unitId;
const q = (s, p) => db.query(s, p);
const insert = async (s, p) => (await q(s, p)).rows[0];

async function as(userId, fn) {
  return db.transaction(async tx => {
    await tx.query("select set_config('app.user_id', $1, true)", [userId]);
    return fn({query: (s, p) => tx.query(s, p)});
  });
}

before(async () => {
  db = new PGlite();
  await db.waitReady;
  for (const f of [
    'schema.sql','002_workflows.sql','003_read_permissions.sql','004_platform.sql',
    '005_identity.sql','006_owner_push.sql','007_master_system.sql','008_units_ownership_and_fixes.sql','009_unit_number.sql','010_cameras.sql','011_mulakhub_sandok_instapay.sql'
  ]) {
    await db.exec(await readFile(new URL('../db/' + f, import.meta.url), 'utf8'));
  }

  // Seed users
  adminId = (await insert("insert into private.users(email,password_hash,name) values('admin@towerflow.test','unused','Admin') returning id")).id;
  managerId = (await insert("insert into private.users(email,password_hash,name) values('manager@towerflow.test','unused','Manager') returning id")).id;
  residentId = (await insert("insert into private.users(email,password_hash,name) values('resident@towerflow.test','unused','Resident') returning id")).id;

  // Seed Organization
  orgId = (await insert("insert into organizations(name, currency) values('Global Tower Org', 'EGP') returning id")).id;
  await q("insert into private.memberships values($1,$2,'admin')", [orgId, adminId]);
  await q("insert into private.memberships values($1,$2,'manager')", [orgId, managerId]);
  await q("insert into private.memberships values($1,$2,'resident')", [orgId, residentId]);
});

after(async () => await db.close());

test('1. Tower setup begins with strictly zero money balances', async () => {
  const tower = await as(adminId, c => createTowerWithZeroBalance(c, orgId, adminId, {
    name: 'Al-Noor Tower',
    address: 'Cairo, Egypt',
    currency: 'EGP',
    country: 'EG',
    floors_count: 10,
    units_per_floor: 4
  }));
  assert.ok(tower.id);
  tower1Id = tower.id;

  const moneyAccs = (await q('select * from money_accounts where property_id=$1 and org_id=$2', [tower1Id, orgId])).rows;
  assert.equal(moneyAccs.length, 4); // cash, bank, wallet, maintenance_fund
  for (const ma of moneyAccs) {
    assert.equal(Number(ma.balance), 0, `Account ${ma.name} must start with zero balance`);
  }
});

test('2. Automatic batch generation of units without duplicate names', async () => {
  const result = await as(adminId, c => generateUnitsBatch(c, orgId, adminId, tower1Id, {
    floors: 3,
    units_per_floor: 2,
    prefix: 'N-',
    default_area: 120
  }));
  assert.equal(result.created, 6); // 3 * 2 = 6 units
  unitId = result.units[0].id;

  // Trying to generate again should not duplicate
  const reResult = await as(adminId, c => generateUnitsBatch(c, orgId, adminId, tower1Id, {
    floors: 3,
    units_per_floor: 2,
    prefix: 'N-'
  }));
  assert.equal(reResult.created, 0);
});

test('3. Enforce unit uniqueness inside the same tower', async () => {
  await assert.rejects(
    as(adminId, c => c.query("insert into units(org_id, property_id, name) values($1, $2, 'N-101')", [orgId, tower1Id])),
    /unique/i
  );
});

test('4. Multiple towers remain isolated and transfer between different towers is blocked', async () => {
  const tower2 = await as(adminId, c => createTowerWithZeroBalance(c, orgId, adminId, {
    name: 'Al-Amal Tower',
    currency: 'EGP'
  }));
  tower2Id = tower2.id;

  const accs1 = (await q("select id, balance from money_accounts where property_id=$1 and kind='cash'", [tower1Id])).rows[0];
  const accs2 = (await q("select id, balance from money_accounts where property_id=$1 and kind='cash'", [tower2Id])).rows[0];

  // Give tower 1 some cash to test transfer
  await q('update money_accounts set balance=1000 where id=$1', [accs1.id]);

  // Transfer between Tower 1 and Tower 2 must fail
  await assert.rejects(
    as(adminId, c => transferWithinTower(c, orgId, adminId, tower1Id, accs1.id, accs2.id, 200, 'Cross-tower transfer')),
    /forbidden|different/i
  );

  // Transfer within Tower 1 (Cash to Bank) must succeed
  const bank1 = (await q("select id from money_accounts where property_id=$1 and kind='bank'", [tower1Id])).rows[0];
  const tfr = await as(adminId, c => transferWithinTower(c, orgId, adminId, tower1Id, accs1.id, bank1.id, 250, 'Cash to Bank'));
  assert.ok(tfr.ok);

  const updatedCash = (await q('select balance from money_accounts where id=$1', [accs1.id])).rows[0].balance;
  const updatedBank = (await q('select balance from money_accounts where id=$1', [bank1.id])).rows[0].balance;
  assert.equal(Number(updatedCash), 750);
  assert.equal(Number(updatedBank), 250);
});

test('5. Tenant and Owner party records, active lease, and ownership tracking', async () => {
  partyId = (await insert(
    "insert into parties (org_id, name, kind, user_id) values ($1, 'Ahmed Tenant', 'tenant', $2) returning id",
    [orgId, residentId]
  )).id;

  const lease = (await insert(
    "insert into leases (org_id, unit_id, party_id, start_date, end_date, rent, status) values ($1, $2, $3, '2026-01-01', '2026-12-31', 2000, 'active') returning id",
    [orgId, unitId, partyId]
  )).id;
  assert.ok(lease);

  // Unit occupancy should now be updated to occupied automatically
  const unit = (await q('select status from units where id=$1', [unitId])).rows[0];
  assert.equal(unit.status, 'occupied');
});

test('6. Expense distribution among units with equal shares and exemptions funded by maintenance fund', async () => {
  // Give maintenance fund some starting balance
  const mFund = (await q("select id from money_accounts where property_id=$1 and kind='maintenance_fund'", [tower1Id])).rows[0];
  await q('update money_accounts set balance=5000 where id=$1', [mFund.id]);

  const units = (await q('select id from units where property_id=$1 order by name', [tower1Id])).rows;
  const exemptUnitId = units[0].id;

  const distResult = await as(adminId, c => createExpenseDistribution(c, orgId, adminId, {
    property_id: tower1Id,
    title: 'Water pump repair',
    total_amount: 1000,
    method: 'equal',
    exemptions: {
      [exemptUnitId]: {is_exempt: true, source: 'fund'}
    }
  }));

  assert.ok(distResult.distribution.id);
  assert.ok(distResult.invoices_count > 0);

  // Check that maintenance fund covered the exempt share
  const fundAfter = (await q('select balance from money_accounts where id=$1', [mFund.id])).rows[0].balance;
  assert.ok(Number(fundAfter) < 5000, 'Maintenance fund balance should decrease by exempt share');
});

test('7. Payment Proof lifecycle: submit, review, verify receipt and avoid replay', async () => {
  // Create an issued invoice for tenant
  const inv = (await insert(
    "insert into invoices (org_id, party_id, unit_id, number, description, amount, due_date, status) values ($1, $2, $3, 'INV-TEST-001', 'Monthly service', 500, '2026-11-15', 'draft') returning id",
    [orgId, partyId, unitId]
  )).id;
  await as(adminId, c => issue(c, orgId, adminId, inv));

  // Resident submits proof
  const bank1 = (await q("select id from money_accounts where property_id=$1 and kind='bank'", [tower1Id])).rows[0];
  const proof = await as(residentId, c => submitPaymentProof(c, orgId, residentId, {
    property_id: tower1Id,
    unit_id: unitId,
    party_id: partyId,
    invoice_id: inv,
    amount: 500,
    reference: 'BANK-TRF-998811',
    target_account_id: bank1.id
  }));
  assert.equal(proof.status, 'pending');

  // Duplicate proof reference must be rejected
  await assert.rejects(
    as(residentId, c => submitPaymentProof(c, orgId, residentId, {
      property_id: tower1Id,
      unit_id: unitId,
      party_id: partyId,
      amount: 500,
      reference: 'BANK-TRF-998811'
    })),
    /duplicate/i
  );

  // Manager reviews and approves
  const approvedProof = await as(managerId, c => reviewPaymentProof(c, orgId, managerId, proof.id, 'approved'));
  assert.equal(approvedProof.status, 'approved');
  assert.ok(approvedProof.receipt_id);

  // Invoice should now be marked as paid
  const updatedInv = (await q('select status from invoices where id=$1', [inv])).rows[0];
  assert.equal(updatedInv.status, 'paid');

  // Trying to review again must fail
  await assert.rejects(
    as(managerId, c => reviewPaymentProof(c, orgId, managerId, proof.id, 'approved')),
    /already reviewed/i
  );
});

test('8. Fines imposition, tracking and authorized exemption with reason', async () => {
  const fine = await as(adminId, c => imposeFine(c, orgId, adminId, {
    property_id: tower1Id,
    unit_id: unitId,
    party_id: partyId,
    title: 'Late payment fee',
    amount: 50
  }));
  assert.equal(fine.status, 'pending');

  const exempted = await as(adminId, c => exemptFine(c, orgId, adminId, fine.id, 'First-time courtesy waiver'));
  assert.equal(exempted.status, 'exempt');
  assert.equal(exempted.exemption_reason, 'First-time courtesy waiver');
});

test('9. Multi-allocation payment collection and excess credit balance recording', async () => {
  const inv1 = (await insert(
    "insert into invoices (org_id, party_id, unit_id, number, description, amount, due_date, status) values ($1, $2, $3, 'INV-M1', 'Electric', 200, '2026-11-15', 'draft') returning id",
    [orgId, partyId, unitId]
  )).id;
  await as(adminId, c => issue(c, orgId, adminId, inv1));

  // Collect 300 (200 pays inv1, 100 excess goes to resident credit balance)
  const result = await as(adminId, c => collectMulti(c, orgId, adminId, {
    property_id: tower1Id,
    party_id: partyId,
    amount: 300,
    allocations: [{invoice_id: inv1, amount: 200}]
  }));

  assert.ok(result.ok);
  assert.equal(result.receipts.length, 1);
  assert.equal(result.excess_credited, '100.00');

  // Verify credit balance exists
  const cred = (await q('select * from credit_balances where property_id=$1 and party_id=$2', [tower1Id, partyId])).rows[0];
  assert.equal(Number(cred.balance), 100);
});

test('10. Applying resident credit balance to settle invoice without new cash receipt', async () => {
  const cred = (await q('select * from credit_balances where property_id=$1 and party_id=$2', [tower1Id, partyId])).rows[0];
  
  const inv2 = (await insert(
    "insert into invoices (org_id, party_id, unit_id, number, description, amount, due_date, status) values ($1, $2, $3, 'INV-M2', 'Water share', 50, '2026-11-15', 'draft') returning id",
    [orgId, partyId, unitId]
  )).id;
  await as(adminId, c => issue(c, orgId, adminId, inv2));

  const cashBefore = (await q("select balance from money_accounts where property_id=$1 and kind='cash'", [tower1Id])).rows[0].balance;

  const applyResult = await as(adminId, c => applyCreditToInvoice(c, orgId, adminId, cred.id, inv2, 50));
  assert.ok(applyResult.ok);
  assert.equal(applyResult.remaining_credit, '50.00');

  // Verify cash balance did NOT increase
  const cashAfter = (await q("select balance from money_accounts where property_id=$1 and kind='cash'", [tower1Id])).rows[0].balance;
  assert.equal(Number(cashAfter), Number(cashBefore), 'Cash balance must not change when applying credit');
});

test('11. Tower management transfer with mutual agreement and assistant suspension', async () => {
  const newManagerId = (await insert("insert into private.users(email,password_hash,name) values('newmgr@towerflow.test','unused','New Manager') returning id")).id;
  await q("insert into private.memberships values($1,$2,'manager')", [orgId, newManagerId]);

  // Add an assistant delegation to Tower 1
  await as(adminId, c => c.query(
    "insert into delegations (org_id, property_id, user_id, role_title, approval_limit, active, granted_by) values ($1, $2, $3, 'Assistant', 2000, true, $4)",
    [orgId, tower1Id, managerId, adminId]
  ));

  // Initiate transfer
  const tfr = await as(adminId, c => initiateTowerTransfer(c, orgId, adminId, tower1Id, 'newmgr@towerflow.test', 'Handover for 2027'));
  assert.equal(tfr.status, 'pending');

  // Respond accept
  const completed = await as(newManagerId, c => respondTowerTransfer(c, orgId, newManagerId, tfr.id, true));
  assert.equal(completed.status, 'accepted');

  // Check property manager updated
  const prop = (await q('select manager_user_id from properties where id=$1', [tower1Id])).rows[0];
  assert.equal(prop.manager_user_id, newManagerId);

  // Assistant delegations for this property should now be suspended (active=false)
  const del = (await q('select active from delegations where property_id=$1', [tower1Id])).rows[0];
  assert.equal(del.active, false, 'Delegations must be deactivated on management handover');
});

test('12. Tower archiving and safe deletion guard (cannot delete tower with financial records)', async () => {
  // Tower 1 has financial records (invoices, receipts, journals)
  await assert.rejects(
    as(adminId, c => deleteTowerSafely(c, orgId, adminId, tower1Id)),
    /financial records/i
  );

  // Archiving Tower 1 must succeed
  const archived = await as(adminId, c => archiveTower(c, orgId, adminId, tower1Id));
  assert.equal(archived.status, 'archived');
  assert.ok(archived.archived_at);

  // Tower 2 has no financial transactions, so it can be deleted safely
  const delResult = await as(adminId, c => deleteTowerSafely(c, orgId, adminId, tower2Id));
  assert.ok(delResult.ok);
});

test('13. Comprehensive Tower backup export', async () => {
  const backup = await as(adminId, c => exportTowerBackup(c, orgId, adminId, tower1Id));
  assert.equal(backup.version, '1.0');
  assert.equal(backup.property.name, 'Al-Noor Tower');
  assert.ok(backup.units.length > 0);
  assert.ok(backup.money_accounts.length > 0);
  assert.ok(backup.leases.length > 0);
});

test('14. Support tickets and temporary support access grant workflow', async () => {
  const supportUserId = (await insert("insert into private.users(email,password_hash,name) values('support@towerflow.test','unused','Support Rep') returning id")).id;

  // Resident opens support ticket
  const ticket = await as(residentId, c => createSupportTicket(c, residentId, {
    org_id: orgId,
    property_id: tower1Id,
    title: 'Question regarding online payment',
    description: 'How do I upload the bank transfer slip?',
    category: 'billing',
    edition: 'free'
  }));
  assert.ok(ticket.ticket_number);
  assert.equal(ticket.status, 'open');

  // Support rep replies
  const reply = await as(supportUserId, c => addSupportMessage(c, supportUserId, ticket.id, 'You can upload the slip under Receipts tab.', true));
  assert.ok(reply.id);

  // Support rep requests temporary access to inspect property
  const grant = await as(supportUserId, c => requestSupportAccess(c, supportUserId, ticket.id, orgId, tower1Id, 2));
  assert.equal(grant.status, 'requested');

  // Building manager approves temporary access
  const approvedGrant = await as(adminId, c => approveSupportAccess(c, adminId, grant.id, false));
  assert.equal(approvedGrant.status, 'approved');

  // Manager can revoke access anytime
  const revoked = await as(adminId, c => revokeSupportAccess(c, adminId, grant.id));
  assert.equal(revoked.status, 'revoked');
});

test('15. Area-based expense distribution calculates exact pro-rata shares', async () => {
  const towerArea = await as(adminId, c => createTowerWithZeroBalance(c, orgId, adminId, {
    name: 'Al-Farouq Tower',
    currency: 'EGP'
  }));
  const u1 = (await insert("insert into units(org_id, property_id, name, area) values($1, $2, 'A1', 100) returning id", [orgId, towerArea.id])).id;
  const u2 = (await insert("insert into units(org_id, property_id, name, area) values($1, $2, 'A2', 300) returning id", [orgId, towerArea.id])).id;

  const dist = await as(adminId, c => createExpenseDistribution(c, orgId, adminId, {
    property_id: towerArea.id,
    title: 'Roof waterproofing',
    total_amount: 1000,
    method: 'area'
  }));

  assert.equal(dist.shares.length, 2);
  const share1 = dist.shares.find(s => s.unit_id === u1);
  const share2 = dist.shares.find(s => s.unit_id === u2);
  assert.equal(Number(share1.amount), 250); // 100 / 400 * 1000 = 250
  assert.equal(Number(share2.amount), 750); // 300 / 400 * 1000 = 750
});

test('16. Percentage-based expense distribution validates shares total 100%', async () => {
  const towerPct = await as(adminId, c => createTowerWithZeroBalance(c, orgId, adminId, {
    name: 'Al-Bustan Tower',
    currency: 'EGP'
  }));
  const u1 = (await insert("insert into units(org_id, property_id, name) values($1, $2, 'B1') returning id", [orgId, towerPct.id])).id;
  const u2 = (await insert("insert into units(org_id, property_id, name) values($1, $2, 'B2') returning id", [orgId, towerPct.id])).id;

  // Failing case: shares don't sum to 100
  await assert.rejects(
    as(adminId, c => createExpenseDistribution(c, orgId, adminId, {
      property_id: towerPct.id,
      title: 'Elevator upgrade',
      total_amount: 2000,
      method: 'percentage',
      shares: {[u1]: 40, [u2]: 50}
    })),
    /100%/i
  );

  // Success case: 40% and 60%
  const dist = await as(adminId, c => createExpenseDistribution(c, orgId, adminId, {
    property_id: towerPct.id,
    title: 'Elevator upgrade',
    total_amount: 2000,
    method: 'percentage',
    shares: {[u1]: 40, [u2]: 60}
  }));

  const share1 = dist.shares.find(s => s.unit_id === u1);
  const share2 = dist.shares.find(s => s.unit_id === u2);
  assert.equal(Number(share1.amount), 800); // 40% of 2000
  assert.equal(Number(share2.amount), 1200); // 60% of 2000
});

test('17. Bank and Cash Account reconciliation detects balance discrepancies', async () => {
  const bankAcc = (await q("select id, balance from money_accounts where property_id=$1 and kind='bank'", [tower1Id])).rows[0];
  
  // Reconcile with mismatch
  const reconMismatch = await as(adminId, c => reconcileMoneyAccount(c, orgId, adminId, {
    property_id: tower1Id,
    money_account_id: bankAcc.id,
    statement_date: '2026-11-01',
    statement_balance: 300,
    notes: 'Bank interest not yet recorded'
  }));
  assert.equal(reconMismatch.status, 'discrepancy');
  assert.equal(Number(reconMismatch.difference), Number((300 - Number(bankAcc.balance)).toFixed(2)));

  // Reconcile with exact match
  const reconMatch = await as(adminId, c => reconcileMoneyAccount(c, orgId, adminId, {
    property_id: tower1Id,
    money_account_id: bankAcc.id,
    statement_date: '2026-11-01',
    statement_balance: Number(bankAcc.balance),
    notes: 'Monthly statement verified'
  }));
  assert.equal(reconMatch.status, 'matched');
  assert.equal(Number(reconMatch.difference), 0);
});

test('18. Reopening closed accounting period requires reason and leaves immutable audit trail', async () => {
  const period = await as(adminId, c => createPeriod(c, orgId, {
    name: 'Q1-2026',
    start_date: '2026-01-01',
    end_date: '2026-03-31'
  }));
  assert.equal(period.status, 'open');

  await as(adminId, c => closePeriod(c, orgId, adminId, period.id));
  const closed = (await q('select status from accounting_periods where id=$1', [period.id])).rows[0];
  assert.equal(closed.status, 'closed');

  // Reopening without reason must fail
  await assert.rejects(
    as(adminId, c => reopenPeriod(c, orgId, adminId, period.id, '')),
    /reason/i
  );

  // Reopening with reason
  const reopened = await as(adminId, c => reopenPeriod(c, orgId, adminId, period.id, 'Auditor requested correction of tax depreciation'));
  assert.equal(reopened.status, 'open');

  // Verify audit log entry exists
  const audit = (await q("select * from audit_logs where entity='accounting_periods' and entity_id=$1 and action ilike 'reopen%'", [period.id])).rows[0];
  assert.ok(audit, 'Audit log must record the reopening event');
  assert.match(audit.action, /Auditor requested/i);
});

test('19. Inter-edition migration roundtrip: export tower and import into isolated workspace', async () => {
  // Export backup of Tower 1
  const backup = await as(adminId, c => exportTowerBackup(c, orgId, adminId, tower1Id));
  assert.ok(backup.property);

  // Create a separate organization (representing Commercial edition database)
  const commercialOrgId = (await insert("insert into organizations(name, currency) values('Commercial TowerFlow Org', 'EGP') returning id")).id;
  await q("insert into private.memberships values($1,$2,'admin')", [commercialOrgId, adminId]);

  // Import into commercial organization
  const imported = await as(adminId, c => importTowerBackup(c, commercialOrgId, adminId, backup));
  assert.ok(imported.ok);
  assert.equal(imported.property.name, 'Al-Noor Tower');
  assert.equal(imported.property.org_id, commercialOrgId);

  // Verify units in commercial org
  const importedUnits = (await q('select * from units where property_id=$1 and org_id=$2', [imported.property.id, commercialOrgId])).rows;
  assert.ok(importedUnits.length > 0);
  assert.equal(importedUnits.length, backup.units.length);
});
