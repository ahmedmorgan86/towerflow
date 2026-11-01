import {cents} from './catalog.mjs';
import {permission,journal} from './finance.mjs';
import {isoDate} from './platform.mjs';

const bad = (s, status = 400) => Object.assign(new Error(s), {status});

// Helper for formatting money
export const toFixedMoney = (c) => (Number(c) / 100).toFixed(2);

// Document sequence numbering
export async function getNextDocumentNumber(c, org, docType, fiscalYear = 0) {
  const row = (await c.query(
    `insert into private.document_sequences (org_id, document_type, fiscal_year, last_number)
     values ($1, $2, $3, 1)
     on conflict (org_id, document_type, fiscal_year)
     do update set last_number = private.document_sequences.last_number + 1
     returning last_number`,
    [org, docType, fiscalYear]
  )).rows[0];
  const num = String(row.last_number).padStart(5, '0');
  return fiscalYear > 0 ? `${docType}-${fiscalYear}-${num}` : `${docType}-${num}`;
}

// 1. Tower Setup & Batch Unit Generation
export async function createTowerWithZeroBalance(c, org, user, data) {
  await permission(c, org, 'manage');
  if (!data.name || !String(data.name).trim()) throw bad('Tower name required');
  const currency = data.currency || 'EGP';
  const country = data.country || 'EG';
  
  const prop = (await c.query(
    `insert into properties (org_id, name, address, kind, currency, country, manager_user_id, floors_count, units_per_floor)
     values ($1, $2, $3, 'tower', $4, $5, $6, $7, $8)
     returning *`,
    [
      org,
      String(data.name).slice(0, 160),
      String(data.address || '').slice(0, 400),
      currency,
      country,
      user,
      Number(data.floors_count || 0),
      Number(data.units_per_floor || 0)
    ]
  )).rows[0];

  // Initialize standard GL accounts if not already present
  const accMap = {};
  for (const [code, accName, kind] of [
    ['1100', 'Cash', 'asset'],
    ['1110', 'Bank Accounts', 'asset'],
    ['1120', 'Electronic Wallets', 'asset'],
    ['1130', 'Maintenance Fund', 'asset'],
    ['1140', 'Fines Fund', 'asset'],
    ['1200', 'Receivables', 'asset'],
    ['2100', 'Tax payable', 'liability'],
    ['2200', 'Resident Credit Balances', 'liability'],
    ['4000', 'Rental & Maintenance income', 'income'],
    ['4100', 'Fines income', 'income'],
    ['5000', 'Operating expenses', 'expense']
  ]) {
    const acc = (await c.query(
      `insert into accounts (org_id, code, name, kind)
       values ($1, $2, $3, $4)
       on conflict (org_id, code) do update set name=excluded.name
       returning id`,
      [org, code, accName, kind]
    )).rows[0];
    accMap[code] = acc.id;
  }

  // Set up standard money accounts for this specific tower with strictly ZERO balance
  const standardMoneyAccounts = [
    {name: 'خزنة البرج النقدية (Cash Box)', kind: 'cash', account_id: accMap['1100']},
    {name: 'الحساب البنكي الرئيسي (Bank Account)', kind: 'bank', account_id: accMap['1110']},
    {name: 'المحفظة الإلكترونية (Wallet)', kind: 'wallet', account_id: accMap['1120']},
    {name: 'صندوق الصيانة (Maintenance Fund)', kind: 'maintenance_fund', account_id: accMap['1130']}
  ];

  for (const m of standardMoneyAccounts) {
    await c.query(
      `insert into money_accounts (org_id, property_id, account_id, name, kind, balance)
       values ($1, $2, $3, $4, $5, 0)`,
      [org, prop.id, m.account_id, m.name, m.kind]
    );
  }

  return prop;
}

export async function generateUnitsBatch(c, org, user, propertyId, options) {
  await permission(c, org, 'manage');
  const floors = Math.max(1, Math.min(100, Number(options.floors || 1)));
  const unitsPerFloor = Math.max(1, Math.min(50, Number(options.units_per_floor || 1)));
  const prefix = String(options.prefix || '').trim();
  const defaultArea = options.default_area ? Number(options.default_area) : null;
  const startFloor = Number(options.start_floor ?? 1);

  const created = [];
  for (let f = startFloor; f < startFloor + floors; f++) {
    for (let u = 1; u <= unitsPerFloor; u++) {
      const unitNum = `${prefix}${f}${String(u).padStart(2, '0')}`;
      const existing = (await c.query(
        'select 1 from units where org_id=$1 and property_id=$2 and name=$3',
        [org, propertyId, unitNum]
      )).rows.length;
      if (!existing) {
        const row = (await c.query(
          `insert into units (org_id, property_id, name, floor, area, status, payer_responsibility)
           values ($1, $2, $3, $4, $5, 'vacant', 'tenant')
           returning id, name, floor, area, status`,
          [org, propertyId, unitNum, f, defaultArea]
        )).rows[0];
        created.push(row);
      }
    }
  }
  return {created: created.length, units: created};
}

// 2. Intra-Tower Money Transfers & Inter-Tower Transfer Prohibition
export async function transferWithinTower(c, org, user, propertyId, fromMoneyAccId, toMoneyAccId, amount, note = '') {
  await permission(c, org, 'finance');
  if (fromMoneyAccId === toMoneyAccId) throw bad('Source and destination accounts must be different');
  const n = cents(amount);

  const fromAcc = (await c.query(
    'select * from money_accounts where org_id=$1 and id=$2 and property_id=$3 for update',
    [org, fromMoneyAccId, propertyId]
  )).rows[0];
  const toAcc = (await c.query(
    'select * from money_accounts where org_id=$1 and id=$2 and property_id=$3 for update',
    [org, toMoneyAccId, propertyId]
  )).rows[0];

  if (!fromAcc || !toAcc) throw bad('Money accounts not found or belong to different towers', 404);
  if (fromAcc.property_id !== toAcc.property_id || fromAcc.property_id !== propertyId) {
    throw bad('Inter-tower money transfers are strictly forbidden', 403);
  }
  if (cents(fromAcc.balance) < n) throw bad('Insufficient funds in source account');

  // Create balanced journal tagged to the tower directly upon insertion
  const jId = await journal(
    c, org, user,
    `Transfer: ${fromAcc.name} -> ${toAcc.name} (${note || 'Intra-tower transfer'})`,
    [
      {account_id: toAcc.account_id, debit: amount},
      {account_id: fromAcc.account_id, credit: amount}
    ],
    null,
    null,
    propertyId
  );

  // Update account balances
  await c.query('update money_accounts set balance=balance-$1 where id=$2 and org_id=$3', [amount, fromMoneyAccId, org]);
  await c.query('update money_accounts set balance=balance+$1 where id=$2 and org_id=$3', [amount, toMoneyAccId, org]);

  return {ok: true, journal_id: jId};
}

export function assertNoCrossTowerTransfer(propA, propB) {
  if (propA && propB && propA !== propB) {
    throw bad('Financial transfers between different towers are prohibited', 403);
  }
}

// 3. Payment Proofs (إثباتات الدفع من السكان)
export async function submitPaymentProof(c, org, user, data) {
  isoDate(data.date || new Date().toISOString().slice(0, 10));
  cents(data.amount);
  if (!data.property_id || !data.unit_id || !data.party_id) {
    throw bad('Property, unit, and party required');
  }
  const ref = String(data.reference || '').trim();
  if (!ref) throw bad('Payment reference required');

  const existing = (await c.query(
    'select 1 from payment_proofs where org_id=$1 and reference=$2',
    [org, ref]
  )).rows.length;
  if (existing) throw bad('Duplicate payment reference', 409);

  return (await c.query(
    `insert into payment_proofs (org_id, property_id, unit_id, party_id, invoice_id, amount, payment_method, target_account_id, reference, image_url, status, created_by)
     values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'pending', $11)
     returning *`,
    [
      org,
      data.property_id,
      data.unit_id,
      data.party_id,
      data.invoice_id || null,
      data.amount,
      data.payment_method || 'bank_transfer',
      data.target_account_id || null,
      ref,
      String(data.image_url || '').slice(0, 2000),
      user
    ]
  )).rows[0];
}

export async function reviewPaymentProof(c, org, user, proofId, decision, rejectionReason = '') {
  await permission(c, org, 'finance');
  if (!['approved', 'rejected'].includes(decision)) throw bad('Invalid decision');
  
  const proof = (await c.query(
    'select * from payment_proofs where id=$1 and org_id=$2 for update',
    [proofId, org]
  )).rows[0];
  if (!proof) throw bad('Proof not found', 404);
  if (proof.status !== 'pending') throw bad('Proof is already reviewed', 409);

  if (decision === 'rejected') {
    return (await c.query(
      `update payment_proofs
       set status='rejected', rejection_reason=$1, reviewed_by=$2, reviewed_at=now()
       where id=$3 and org_id=$4
       returning *`,
      [String(rejectionReason || 'Rejected by manager').slice(0, 500), user, proofId, org]
    )).rows[0];
  }

  // If approved:
  let receiptId = null;
  if (proof.invoice_id) {
    // Collect on invoice
    const inv = (await c.query(
      'select * from invoices where id=$1 and org_id=$2 for update',
      [proof.invoice_id, org]
    )).rows[0];
    if (inv && inv.status === 'issued') {
      const paid = (await c.query('select coalesce(sum(amount),0) total from receipts where invoice_id=$1', [inv.id])).rows[0].total;
      const remaining = cents(inv.amount) - (Number(paid) === 0 ? 0 : cents(paid));
      const payAmt = Math.min(cents(proof.amount), remaining);
      if (payAmt > 0) {
        const docNum = await getNextDocumentNumber(c, org, 'REC', new Date().getFullYear());
        const accs = Object.fromEntries(
          (await c.query("select code, id from accounts where org_id=$1 and code in ('1100','1200')", [org])).rows.map(x => [x.code, x.id])
        );
        const jId = await journal(
          c, org, user,
          `Receipt for Proof ${proof.reference}`,
          [
            {account_id: accs['1100'], debit: toFixedMoney(payAmt)},
            {account_id: accs['1200'], credit: toFixedMoney(payAmt)}
          ],
          null,
          null,
          proof.property_id
        );
        const r = (await c.query(
          `insert into receipts (org_id, invoice_id, amount, reference, journal_id)
           values ($1, $2, $3, $4, $5)
           returning id`,
          [org, inv.id, toFixedMoney(payAmt), docNum, jId]
        )).rows[0];
        receiptId = r.id;
        if (payAmt === remaining) {
          await c.query("update invoices set status='paid' where id=$1", [inv.id]);
        }
      }
    }
  }

  // Update money account balance if specified
  if (proof.target_account_id) {
    await c.query(
      'update money_accounts set balance=balance+$1 where id=$2 and org_id=$3',
      [proof.amount, proof.target_account_id, org]
    );
  }

  return (await c.query(
    `update payment_proofs
     set status='approved', reviewed_by=$1, reviewed_at=now(), receipt_id=$2
     where id=$3 and org_id=$4
     returning *`,
    [user, receiptId, proofId, org]
  )).rows[0];
}

// 4. Expense Distribution Engine (توزيع المصروفات الفعلية على الشقق)
export async function createExpenseDistribution(c, org, user, data) {
  await permission(c, org, 'finance');
  isoDate(data.expense_date || new Date().toISOString().slice(0, 10));
  const totalCents = cents(data.total_amount);
  const method = data.method || 'equal'; // equal, area, percentage, custom
  const propertyId = data.property_id;
  if (!propertyId) throw bad('Property required');

  // Fetch active units of the property
  const units = (await c.query(
    'select id, name, area, floor from units where org_id=$1 and property_id=$2 and status != \'maintenance\' order by floor, name',
    [org, propertyId]
  )).rows;
  if (!units.length) throw bad('No eligible units in property to distribute expense');

  // Default party for vacant units without a lease
  const defaultParty = (await c.query("select id from parties where org_id=$1 and kind in ('owner','tenant') limit 1", [org])).rows[0]?.id;

  // Exemptions map: unit_id -> {is_exempt: boolean, source: 'fund'|'redistribute'}
  const exemptions = data.exemptions || {};

  const dist = (await c.query(
    `insert into expense_distributions (org_id, property_id, title, total_amount, method, expense_date, notes, created_by)
     values ($1, $2, $3, $4, $5, $6, $7, $8)
     returning *`,
    [
      org,
      propertyId,
      String(data.title || 'Expense distribution').slice(0, 200),
      toFixedMoney(totalCents),
      method,
      data.expense_date || new Date().toISOString().slice(0, 10),
      String(data.notes || '').slice(0, 1000),
      user
    ]
  )).rows[0];

  // Calculate shares based on method
  let nonExemptUnits = units.filter(u => !exemptions[u.id]?.is_exempt);
  if (!nonExemptUnits.length) throw bad('Cannot distribute: all units are exempt');

  let shares = [];
  if (method === 'equal') {
    const shareCents = Math.floor(totalCents / nonExemptUnits.length);
    let remainder = totalCents - (shareCents * nonExemptUnits.length);
    for (const u of nonExemptUnits) {
      const extra = remainder > 0 ? 1 : 0;
      if (remainder > 0) remainder--;
      shares.push({unit_id: u.id, cents: shareCents + extra});
    }
  } else if (method === 'area') {
    const totalArea = nonExemptUnits.reduce((s, u) => s + Number(u.area || 100), 0);
    let distributed = 0;
    for (let i = 0; i < nonExemptUnits.length; i++) {
      const u = nonExemptUnits[i];
      if (i === nonExemptUnits.length - 1) {
        shares.push({unit_id: u.id, cents: totalCents - distributed});
      } else {
        const uCents = Math.round((Number(u.area || 100) / totalArea) * totalCents);
        distributed += uCents;
        shares.push({unit_id: u.id, cents: uCents});
      }
    }
  } else if (method === 'percentage') {
    const pShares = data.shares || data.percentages || {};
    const sumPct = nonExemptUnits.reduce((s, u) => s + Number(pShares[u.id] || 0), 0);
    if (Math.abs(sumPct - 100) > 0.01) {
      throw bad('Distribution percentage shares must sum to exactly 100%');
    }
    let distributed = 0;
    for (let i = 0; i < nonExemptUnits.length; i++) {
      const u = nonExemptUnits[i];
      if (i === nonExemptUnits.length - 1) {
        shares.push({unit_id: u.id, cents: totalCents - distributed});
      } else {
        const uCents = Math.round((Number(pShares[u.id] || 0) / 100) * totalCents);
        distributed += uCents;
        shares.push({unit_id: u.id, cents: uCents});
      }
    }
  } else {
    // Custom fixed amounts per unit
    const customShares = data.custom_shares || data.shares || {};
    let distributed = 0;
    for (const u of nonExemptUnits) {
      const cAmt = cents(customShares[u.id] || 0);
      distributed += cAmt;
      shares.push({unit_id: u.id, cents: cAmt});
    }
  }

  // Insert distribution shares and generate invoices for non-exempt shares
  const resultInvoices = [];
  for (const s of shares) {
    const activeLease = (await c.query(
      'select party_id from leases where org_id=$1 and unit_id=$2 and status=\'active\' limit 1',
      [org, s.unit_id]
    )).rows[0];
    const ownerParty = (await c.query(
      'select party_id from ownerships where org_id=$1 and unit_id=$2 limit 1',
      [org, s.unit_id]
    )).rows[0];
    const targetPartyId = activeLease?.party_id || ownerParty?.party_id || defaultParty;

    if (targetPartyId && s.cents > 0) {
      const invNum = await getNextDocumentNumber(c, org, 'INV', new Date().getFullYear());
      const inv = (await c.query(
        `insert into invoices (org_id, party_id, unit_id, number, description, amount, due_date, status)
         values ($1, $2, $3, $4, $5, $6, $7, 'draft')
         returning id, number, amount`,
        [
          org,
          targetPartyId,
          s.unit_id,
          invNum,
          `${dist.title} - Unit share`,
          toFixedMoney(s.cents),
          data.due_date || new Date().toISOString().slice(0, 10)
        ]
      )).rows[0];

      await c.query(
        `insert into distribution_shares (org_id, distribution_id, unit_id, party_id, share_amount, is_exempt, invoice_id)
         values ($1, $2, $3, $4, $5, false, $6)`,
        [org, dist.id, s.unit_id, targetPartyId, toFixedMoney(s.cents), inv.id]
      );
      resultInvoices.push(inv);
    }
  }

  // Handle exempt units:
  for (const u of units.filter(u => exemptions[u.id]?.is_exempt)) {
    const exInfo = exemptions[u.id];
    if (exInfo.source === 'fund') {
      // Check maintenance fund balance
      const fund = (await c.query(
        'select * from money_accounts where org_id=$1 and property_id=$2 and kind=\'maintenance_fund\' for update',
        [org, propertyId]
      )).rows[0];
      if (fund && Number(fund.balance) >= (totalCents / units.length / 100)) {
        await c.query(
          'update money_accounts set balance=balance-$1 where id=$2 and org_id=$3',
          [toFixedMoney(Math.floor(totalCents / units.length)), fund.id, org]
        );
      }
    }
    const targetParty = (await c.query('select party_id from ownerships where org_id=$1 and unit_id=$2 limit 1', [org, u.id])).rows[0]?.party_id ||
                        defaultParty;
    if (targetParty) {
      await c.query(
        `insert into distribution_shares (org_id, distribution_id, unit_id, party_id, share_amount, is_exempt, exemption_source)
         values ($1, $2, $3, $4, 0, true, $5)`,
        [org, dist.id, u.id, targetParty, exInfo.source || 'redistribute']
      );
    }
  }

  return {
    distribution: dist,
    invoices_count: resultInvoices.length,
    invoices: resultInvoices,
    shares: shares.map(s => ({unit_id: s.unit_id, amount: toFixedMoney(s.cents)}))
  };
}

// 5. Fines and Fine Policies (الغرامات)
export async function imposeFine(c, org, user, data) {
  await permission(c, org, 'finance');
  cents(data.amount);
  if (!data.property_id || !data.unit_id || !data.party_id || !data.title) {
    throw bad('Property, unit, party and title required');
  }

  return (await c.query(
    `insert into fines (org_id, property_id, unit_id, party_id, invoice_id, title, amount, status, destination)
     values ($1, $2, $3, $4, $5, $6, $7, 'pending', $8)
     returning *`,
    [
      org,
      data.property_id,
      data.unit_id,
      data.party_id,
      data.invoice_id || null,
      String(data.title).slice(0, 200),
      data.amount,
      data.destination || 'maintenance_fund'
    ]
  )).rows[0];
}

export async function exemptFine(c, org, user, fineId, reason) {
  await permission(c, org, 'manage');
  if (!reason || !String(reason).trim()) throw bad('Exemption reason required');
  const fine = (await c.query('select * from fines where id=$1 and org_id=$2 for update', [fineId, org])).rows[0];
  if (!fine) throw bad('Fine not found', 404);
  if (fine.status !== 'pending') throw bad('Only pending fines can be exempted', 409);

  return (await c.query(
    `update fines
     set status='exempt', exemption_reason=$1, exempted_by=$2
     where id=$3 and org_id=$4
     returning *`,
    [String(reason).slice(0, 500), user, fineId, org]
  )).rows[0];
}

// 6. Multi-Allocation Payment Collection (توزيع الدفعة على المستحقات والغرامات)
export async function collectMulti(c, org, user, data) {
  await permission(c, org, 'finance');
  const totalCents = cents(data.amount);
  const propertyId = data.property_id;
  const partyId = data.party_id;
  const allocations = data.allocations || []; // [{invoice_id, amount}, {fine_id, amount}]
  const ref = data.reference || await getNextDocumentNumber(c, org, 'REC', new Date().getFullYear());

  let allocatedCents = 0;
  for (const a of allocations) {
    allocatedCents += cents(a.amount);
  }
  if (allocatedCents > totalCents) {
    throw bad('Allocated amount exceeds total payment amount');
  }

  // Get GL accounts
  const accs = Object.fromEntries(
    (await c.query("select code, id from accounts where org_id=$1 and code in ('1100','1200','4100')", [org])).rows.map(x => [x.code, x.id])
  );

  const jId = await journal(
    c, org, user,
    `Multi-collection ${ref}`,
    [
      {account_id: accs['1100'], debit: toFixedMoney(totalCents)},
      {account_id: accs['1200'], credit: toFixedMoney(totalCents)}
    ],
    null,
    null,
    propertyId
  );

  const generatedReceipts = [];
  for (const a of allocations) {
    if (a.invoice_id) {
      const inv = (await c.query('select * from invoices where id=$1 and org_id=$2 for update', [a.invoice_id, org])).rows[0];
      if (inv && inv.status === 'issued') {
        const rc = (await c.query(
          `insert into receipts (org_id, invoice_id, amount, reference, journal_id)
           values ($1, $2, $3, $4, $5)
           returning *`,
          [org, inv.id, a.amount, `${ref}-${inv.number}`, jId]
        )).rows[0];
        generatedReceipts.push(rc);

        const paid = (await c.query('select coalesce(sum(amount),0) total from receipts where invoice_id=$1', [inv.id])).rows[0].total;
        if (cents(paid) >= cents(inv.amount)) {
          await c.query("update invoices set status='paid' where id=$1", [inv.id]);
        }
      }
    }
    if (a.fine_id) {
      await c.query(
        "update fines set status='collected' where id=$1 and org_id=$2 and status='pending'",
        [a.fine_id, org]
      );
    }
  }

  // If there's unallocated excess, add to resident credit balance!
  const excessCents = totalCents - allocatedCents;
  if (excessCents > 0 && propertyId && partyId) {
    const credBal = (await c.query(
      `insert into credit_balances (org_id, property_id, party_id, balance)
       values ($1, $2, $3, $4)
       on conflict (org_id, property_id, party_id)
       do update set balance = credit_balances.balance + excluded.balance
       returning id, balance`,
      [org, propertyId, partyId, toFixedMoney(excessCents)]
    )).rows[0];

    await c.query(
      `insert into credit_transactions (org_id, credit_balance_id, amount, action, journal_id, notes, created_by)
       values ($1, $2, $3, 'credit_in', $4, 'Excess collection added to credit', $5)`,
      [org, credBal.id, toFixedMoney(excessCents), jId, user]
    );
  }

  return {ok: true, reference: ref, receipts: generatedReceipts, excess_credited: toFixedMoney(excessCents)};
}

// 7. Advance Payments & Applying Credit Balances (الأرصدة الدائنة)
export async function applyCreditToInvoice(c, org, user, creditBalanceId, invoiceId, amount) {
  await permission(c, org, 'finance');
  const n = cents(amount);
  const cred = (await c.query(
    'select * from credit_balances where id=$1 and org_id=$2 for update',
    [creditBalanceId, org]
  )).rows[0];
  if (!cred) throw bad('Credit balance not found', 404);
  if (cents(cred.balance) < n) throw bad('Insufficient credit balance');

  const inv = (await c.query(
    'select * from invoices where id=$1 and org_id=$2 and status=\'issued\' for update',
    [invoiceId, org]
  )).rows[0];
  if (!inv) throw bad('Issued invoice required', 404);

  const paid = (await c.query('select coalesce(sum(amount),0) total from receipts where invoice_id=$1', [inv.id])).rows[0].total;
  const remaining = cents(inv.amount) - (Number(paid) === 0 ? 0 : cents(paid));
  if (n > remaining) throw bad('Credit applied exceeds invoice balance');

  // Accounting entry: Debit Credit liability (2200), Credit Receivables (1200) -> NO NEW CASH RECEIPT!
  const accs = Object.fromEntries(
    (await c.query("select code, id from accounts where org_id=$1 and code in ('1200','2200')", [org])).rows.map(x => [x.code, x.id])
  );
  const jId = await journal(
    c, org, user,
    `Credit applied to ${inv.number}`,
    [
      {account_id: accs['2200'], debit: toFixedMoney(n)},
      {account_id: accs['1200'], credit: toFixedMoney(n)}
    ],
    null,
    null,
    cred.property_id
  );

  // Deduct from credit balance
  await c.query(
    'update credit_balances set balance=balance-$1 where id=$2 and org_id=$3',
    [toFixedMoney(n), creditBalanceId, org]
  );

  // Record credit transaction
  await c.query(
    `insert into credit_transactions (org_id, credit_balance_id, amount, action, invoice_id, journal_id, notes, created_by)
     values ($1, $2, $3, 'apply_invoice', $4, $5, 'Credit applied to settle invoice', $6)`,
    [org, creditBalanceId, toFixedMoney(n), inv.id, jId, user]
  );

  // Create receipt record referencing the credit settlement
  const docNum = await getNextDocumentNumber(c, org, 'CREC', new Date().getFullYear());
  const r = (await c.query(
    `insert into receipts (org_id, invoice_id, amount, reference, journal_id)
     values ($1, $2, $3, $4, $5)
     returning *`,
    [org, inv.id, toFixedMoney(n), docNum, jId]
  )).rows[0];

  if (n === remaining) {
    await c.query("update invoices set status='paid' where id=$1", [inv.id]);
  }

  return {ok: true, receipt: r, remaining_credit: toFixedMoney(cents(cred.balance) - n)};
}

// 8. Tower Management Transfer (نقل إدارة البرج)
export async function initiateTowerTransfer(c, org, user, propertyId, targetEmail, notes = '') {
  await permission(c, org, 'manage');
  const target = (await c.query('select id from private.users where email=$1', [String(targetEmail).trim().toLowerCase()])).rows[0];
  if (!target) throw bad('Target user not found. Must be registered first.', 404);
  if (target.id === user) throw bad('Cannot transfer to yourself');

  const prop = (await c.query('select * from properties where id=$1 and org_id=$2', [propertyId, org])).rows[0];
  if (!prop) throw bad('Property not found', 404);

  return (await c.query(
    `insert into management_transfers (org_id, property_id, from_user_id, to_user_id, status, notes)
     values ($1, $2, $3, $4, 'pending', $5)
     returning *`,
    [org, propertyId, user, target.id, String(notes).slice(0, 500)]
  )).rows[0];
}

export async function respondTowerTransfer(c, org, user, transferId, accept) {
  const tfr = (await c.query(
    'select * from management_transfers where id=$1 and org_id=$2 for update',
    [transferId, org]
  )).rows[0];
  if (!tfr) throw bad('Transfer request not found', 404);
  if (tfr.to_user_id !== user) throw bad('Unauthorized to respond to this transfer', 403);
  if (tfr.status !== 'pending') throw bad('Transfer is already completed', 409);

  if (!accept) {
    return (await c.query(
      "update management_transfers set status='rejected', responded_at=now() where id=$1 returning *",
      [transferId]
    )).rows[0];
  }

  // Accept: update property manager
  await c.query(
    'update properties set manager_user_id=$1 where id=$2 and org_id=$3',
    [user, tfr.property_id, org]
  );

  // Deactivate old assistant delegations for this property
  await c.query(
    'update delegations set active=false where property_id=$1 and org_id=$2',
    [tfr.property_id, org]
  );

  return (await c.query(
    "update management_transfers set status='accepted', responded_at=now() where id=$1 returning *",
    [transferId]
  )).rows[0];
}

// 9. Tower Archiving & Safe Deletion Guard
export async function archiveTower(c, org, user, propertyId) {
  await permission(c, org, 'manage');
  const prop = (await c.query('select * from properties where id=$1 and org_id=$2 for update', [propertyId, org])).rows[0];
  if (!prop) throw bad('Property not found', 404);

  return (await c.query(
    "update properties set status='archived', archived_at=now() where id=$1 and org_id=$2 returning *",
    [propertyId, org]
  )).rows[0];
}

export async function deleteTowerSafely(c, org, user, propertyId) {
  await permission(c, org, 'manage');
  const prop = (await c.query('select * from properties where id=$1 and org_id=$2 for update', [propertyId, org])).rows[0];
  if (!prop) throw bad('Property not found', 404);

  // Check if any financial records exist for this property
  const hasJournals = (await c.query('select 1 from journals where property_id=$1 and org_id=$2 limit 1', [propertyId, org])).rows.length;
  const hasInvoices = (await c.query('select 1 from invoices i join units u on u.id=i.unit_id where u.property_id=$1 and u.org_id=$2 limit 1', [propertyId, org])).rows.length;
  const hasReceipts = (await c.query('select 1 from payment_proofs where property_id=$1 and org_id=$2 limit 1', [propertyId, org])).rows.length;

  if (hasJournals || hasInvoices || hasReceipts) {
    throw bad('Cannot delete a property with financial records. Use archive instead to keep records intact.', 409);
  }

  await c.query('delete from money_accounts where property_id=$1 and org_id=$2 and balance=0', [propertyId, org]);
  await c.query('delete from units where property_id=$1 and org_id=$2', [propertyId, org]);
  await c.query('delete from properties where id=$1 and org_id=$2', [propertyId, org]);
  return {ok: true, deleted: propertyId};
}

// 10. Full Tower Data Export (Backup) and Migration
export async function exportTowerBackup(c, org, user, propertyId) {
  await permission(c, org, 'manage');
  const prop = (await c.query('select * from properties where id=$1 and org_id=$2', [propertyId, org])).rows[0];
  if (!prop) throw bad('Property not found', 404);

  const units = (await c.query('select id, name, floor, area, status, payer_responsibility from units where property_id=$1 and org_id=$2', [propertyId, org])).rows;
  const moneyAccs = (await c.query('select id, name, kind, balance from money_accounts where property_id=$1 and org_id=$2', [propertyId, org])).rows;
  const leases = (await c.query('select l.* from leases l join units u on u.id=l.unit_id where u.property_id=$1 and u.org_id=$2', [propertyId, org])).rows;
  const ownerships = (await c.query('select o.* from ownerships o join units u on u.id=o.unit_id where u.property_id=$1 and u.org_id=$2', [propertyId, org])).rows;
  const tickets = (await c.query('select t.* from tickets t join units u on u.id=t.unit_id where u.property_id=$1 and u.org_id=$2', [propertyId, org])).rows;
  const contracts = (await c.query('select * from vendor_contracts where property_id=$1 and org_id=$2', [propertyId, org])).rows;

  return {
    version: '1.0',
    exported_at: new Date().toISOString(),
    property: prop,
    units,
    money_accounts: moneyAccs,
    leases,
    ownerships,
    tickets,
    vendor_contracts: contracts
  };
}

export async function importTowerBackup(c, org, user, backupData) {
  await permission(c, org, 'manage');
  if (!backupData || !backupData.property || !backupData.property.name) {
    throw bad('Invalid backup data format');
  }

  const p = backupData.property;
  const prop = (await c.query(
    `insert into properties (org_id, name, address, kind, currency, country, manager_user_id, floors_count, units_per_floor)
     values ($1, $2, $3, $4, $5, $6, $7, $8, $9)
     returning *`,
    [
      org,
      p.name,
      p.address || '',
      p.kind || 'tower',
      p.currency || 'EGP',
      p.country || 'EG',
      user,
      Number(p.floors_count || 0),
      Number(p.units_per_floor || 0)
    ]
  )).rows[0];

  const unitIdMap = new Map();
  if (Array.isArray(backupData.units)) {
    for (const u of backupData.units) {
      const inserted = (await c.query(
        `insert into units (org_id, property_id, name, floor, area, status, payer_responsibility)
         values ($1, $2, $3, $4, $5, $6, $7)
         returning id`,
        [org, prop.id, u.name, u.floor, u.area, u.status || 'vacant', u.payer_responsibility || 'resident']
      )).rows[0];
      unitIdMap.set(u.id, inserted.id);
    }
  }

  // Ensure standard accounts exist in target org
  const accMap = {};
  for (const [code, accName, kind] of [
    ['1100', 'Cash', 'asset'],
    ['1110', 'Bank', 'asset'],
    ['1120', 'Electronic Wallet', 'asset'],
    ['1130', 'Maintenance Reserve', 'asset']
  ]) {
    let a = (await c.query('select id from accounts where org_id=$1 and code=$2', [org, code])).rows[0];
    if (!a) {
      a = (await c.query(
        'insert into accounts (org_id, code, name, kind) values ($1, $2, $3, $4) returning id',
        [org, code, accName, kind]
      )).rows[0];
    }
    accMap[code] = a.id;
  }

  const getAccountId = (kind) => {
    if (kind === 'bank') return accMap['1110'];
    if (kind === 'wallet') return accMap['1120'];
    if (kind === 'maintenance_fund') return accMap['1130'];
    return accMap['1100'];
  };

  if (Array.isArray(backupData.money_accounts)) {
    for (const ma of backupData.money_accounts) {
      await c.query(
        `insert into money_accounts (org_id, property_id, account_id, name, kind, balance)
         values ($1, $2, $3, $4, $5, $6)`,
        [org, prop.id, getAccountId(ma.kind), ma.name, ma.kind, ma.balance || 0]
      );
    }
  } else {
    for (const [name, kind] of [
      ['Tower Cash Box', 'cash'],
      ['Tower Bank Account', 'bank'],
      ['Tower Electronic Wallet', 'wallet'],
      ['Tower Maintenance Fund', 'maintenance_fund']
    ]) {
      await c.query(
        `insert into money_accounts (org_id, property_id, account_id, name, kind, balance)
         values ($1, $2, $3, $4, $5, 0)`,
        [org, prop.id, getAccountId(kind), name, kind]
      );
    }
  }

  return {
    ok: true,
    property: prop,
    units_imported: unitIdMap.size
  };
}

export async function reconcileMoneyAccount(c, org, user, data) {
  await permission(c, org, 'finance');
  if (!data.money_account_id) throw bad('Money account required');
  const stmtDate = data.statement_date || new Date().toISOString().slice(0, 10);
  isoDate(stmtDate);

  const acc = (await c.query(
    'select * from money_accounts where id=$1 and org_id=$2',
    [data.money_account_id, org]
  )).rows[0];
  if (!acc) throw bad('Money account not found', 404);

  const ledgerBalance = Number(acc.balance);
  const stmtBalance = Number(data.statement_balance ?? 0);
  const diff = Number((stmtBalance - ledgerBalance).toFixed(2));
  const status = Math.abs(diff) < 0.001 ? 'matched' : 'discrepancy';

  const recon = (await c.query(
    `insert into account_reconciliations (
       org_id, money_account_id, statement_date, book_balance, actual_balance, difference, reason, document_ref, reconciled_by
     ) values ($1, $2, $3, $4, $5, $6, $7, $8, $9)
     returning *`,
    [
      org,
      data.money_account_id,
      stmtDate,
      ledgerBalance,
      stmtBalance,
      diff,
      data.notes || data.reason || '',
      data.document_ref || '',
      user
    ]
  )).rows[0];

  return { ...recon, status };
}

export async function reopenPeriod(c, org, user, periodId, reason) {
  await permission(c, org, 'manage');
  if (!reason || !String(reason).trim()) throw bad('Reopening reason is strictly required');

  const p = (await c.query('select * from accounting_periods where id=$1 and org_id=$2 for update', [periodId, org])).rows[0];
  if (!p) throw bad('Accounting period not found', 404);
  if (p.status !== 'closed') throw bad('Only closed periods can be reopened', 409);

  const updated = (await c.query(
    "update accounting_periods set status='open', closed_at=null, closed_by=null where id=$1 and org_id=$2 returning *",
    [periodId, org]
  )).rows[0];

  await c.query(
    "insert into audit_logs (org_id, actor_id, entity, entity_id, action) values ($1, $2, 'accounting_periods', $3, $4)",
    [org, user, periodId, `reopen: ${reason.slice(0, 200)}`]
  );

  return updated;
}

