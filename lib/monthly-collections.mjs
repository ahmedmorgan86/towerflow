import { permission, issue, collect } from './finance.mjs';
import { getNextDocumentNumber } from './tower-operations.mjs';

function toFixedMoney(val) {
  const n = Number(val) || 0;
  return n.toFixed(2);
}

function getMonthBounds(monthStr) {
  const [y, m] = monthStr.split('-').map(Number);
  const startDate = `${monthStr}-01`;
  const lastDay = new Date(Date.UTC(y, m, 0)).getUTCDate();
  const endDate = `${monthStr}-${String(lastDay).padStart(2, '0')}`;
  return { startDate, endDate };
}

export async function getMonthlyCollections(c, org, user, monthParam) {
  await permission(c, org, 'finance');
  const now = new Date();
  const currentMonth = monthParam && /^\d{4}-\d{2}$/.test(monthParam) ? monthParam : now.toISOString().slice(0, 7);
  const { startDate, endDate } = getMonthBounds(currentMonth);

  // 1. Fetch all units for organization
  const unitsRes = await c.query(
    `select u.id, u.property_id, coalesce(nullif(u.unit_number, ''), u.name) as unit_number,
            u.name as unit_name, coalesce(u.floor, 0) as floor, coalesce(u.area, 0) as area,
            coalesce(u.status, 'vacant') as status, coalesce(u.ownership_type, 'rental') as ownership_type,
            p.name as property_name
     from units u
     left join properties p on p.id = u.property_id
     where u.org_id = $1
     order by u.unit_number, u.name`,
    [org]
  );

  // 2. Fetch active leases with resident/tenant party
  const leasesRes = await c.query(
    `select l.unit_id, l.party_id, l.rent, p.name as party_name, p.phone as party_phone
     from leases l
     join parties p on p.id = l.party_id
     where l.org_id = $1 and l.status = 'active'`,
    [org]
  );
  const leaseMap = new Map();
  for (const l of leasesRes.rows) leaseMap.set(l.unit_id, l);

  // 3. Fetch ownerships with owner party
  const ownersRes = await c.query(
    `select o.unit_id, o.party_id, p.name as party_name, p.phone as party_phone
     from ownerships o
     join parties p on p.id = o.party_id
     where o.org_id = $1`,
    [org]
  );
  const ownerMap = new Map();
  for (const o of ownersRes.rows) ownerMap.set(o.unit_id, o);

  // 4. Fetch invoices in this month
  const invoicesRes = await c.query(
    `select i.id, i.unit_id, i.party_id, i.number, i.amount, i.due_date, i.status,
            coalesce(sum(r.amount), 0) as paid_amount,
            max(r.reference) as receipt_ref,
            max(r.received_on) as collected_at
     from invoices i
     left join receipts r on r.invoice_id = i.id
     where i.org_id = $1 and i.due_date >= $2 and i.due_date <= $3
     group by i.id, i.unit_id, i.party_id, i.number, i.amount, i.due_date, i.status`,
    [org, startDate, endDate]
  );
  const invoiceMap = new Map();
  for (const inv of invoicesRes.rows) {
    if (inv.unit_id) invoiceMap.set(inv.unit_id, inv);
  }

  let totalDue = 0;
  let totalCollected = 0;
  let occupiedCount = 0;

  const items = unitsRes.rows.map(unit => {
    const lease = leaseMap.get(unit.id);
    const owner = ownerMap.get(unit.id);
    const inv = invoiceMap.get(unit.id);

    const residentName = lease?.party_name || owner?.party_name || (unit.status === 'occupied' ? 'ساكن الوحدة' : 'شاغرة');
    const residentPhone = lease?.party_phone || owner?.party_phone || '';
    const residentKind = lease ? 'tenant' : (owner ? 'owner' : (unit.ownership_type === 'owned' ? 'owner' : 'tenant'));
    const partyId = lease?.party_id || owner?.party_id || null;

    let fee = 0;
    if (inv) {
      fee = Number(inv.amount) || 0;
    } else if (lease && Number(lease.rent) > 0) {
      fee = Number(lease.rent);
    } else {
      fee = 0;
    }

    const paid = inv ? (Number(inv.paid_amount) || 0) : 0;
    const isPaid = inv ? (inv.status === 'paid' || paid >= fee && fee > 0) : false;

    if (unit.status === 'occupied' || fee > 0) occupiedCount++;
    totalDue += fee;
    totalCollected += isPaid ? fee : paid;

    return {
      unit_id: unit.id,
      property_id: unit.property_id,
      property_name: unit.property_name || '',
      unit_number: unit.unit_number,
      unit_name: unit.unit_name,
      floor: unit.floor,
      status: unit.status,
      ownership_type: unit.ownership_type,
      party_id: partyId,
      resident_name: residentName,
      resident_phone: residentPhone,
      resident_kind: residentKind,
      monthly_fee: toFixedMoney(fee),
      paid_amount: toFixedMoney(paid),
      is_paid: isPaid,
      payment_status: isPaid ? 'paid' : (inv ? 'unpaid' : (fee > 0 ? 'unbilled' : 'none')),
      invoice_id: inv?.id || null,
      invoice_number: inv?.number || null,
      receipt_reference: inv?.receipt_ref || null,
      collected_at: inv?.collected_at || null
    };
  });

  const totalRemaining = Math.max(0, totalDue - totalCollected);
  const collectionRate = totalDue > 0 ? Math.round((totalCollected / totalDue) * 100) : (unitsRes.rows.length > 0 && totalCollected > 0 ? 100 : 0);

  return {
    month: currentMonth,
    summary: {
      total_units: unitsRes.rows.length,
      occupied_units: occupiedCount,
      total_due: toFixedMoney(totalDue),
      total_collected: toFixedMoney(totalCollected),
      total_remaining: toFixedMoney(totalRemaining),
      collection_rate: collectionRate
    },
    units: items
  };
}

export async function quickCollect(c, org, user, data) {
  await permission(c, org, 'finance');
  const unitId = data.unit_id;
  if (!unitId) throw Object.assign(new Error('Unit ID required'), { status: 400 });

  const rawAmount = Number(data.amount) || 0;
  if (rawAmount <= 0) throw Object.assign(new Error('Amount must be positive'), { status: 400 });
  const amountStr = toFixedMoney(rawAmount);

  const month = data.month && /^\d{4}-\d{2}$/.test(data.month) ? data.month : new Date().toISOString().slice(0, 7);
  const { startDate, endDate } = getMonthBounds(month);
  const dueDate = `${month}-01`;
  const paymentMethod = data.method || 'cash';
  const customRef = data.reference && data.reference.trim() ? data.reference.trim() : null;

  // 1. Fetch unit
  const unit = (await c.query('select * from units where id=$1 and org_id=$2', [unitId, org])).rows[0];
  if (!unit) throw Object.assign(new Error('Unit not found'), { status: 404 });

  // 2. Resolve party
  let partyId = data.party_id;
  if (!partyId) {
    const lease = (await c.query("select party_id from leases where unit_id=$1 and org_id=$2 and status='active' limit 1", [unitId, org])).rows[0];
    const owner = (await c.query('select party_id from ownerships where unit_id=$1 and org_id=$2 limit 1', [unitId, org])).rows[0];
    partyId = lease?.party_id || owner?.party_id || null;
  }
  if (!partyId) {
    const existing = (await c.query('select id from parties where org_id=$1 limit 1', [org])).rows[0];
    if (existing) {
      partyId = existing.id;
    } else {
      const created = (await c.query(
        "insert into parties(org_id, name, kind) values($1, $2, 'tenant') returning id",
        [org, `ساكن ${unit.unit_number || unit.name}`]
      )).rows[0];
      partyId = created.id;
    }
  }

  // 3. Check if invoice already exists for this unit in this month
  let invoice = (await c.query(
    "select * from invoices where org_id=$1 and unit_id=$2 and due_date >= $3 and due_date <= $4 and status <> 'void' limit 1",
    [org, unitId, startDate, endDate]
  )).rows[0];

  if (!invoice) {
    const invNum = await getNextDocumentNumber(c, org, 'INV', new Date().getFullYear());
    const desc = `اشتراك شهر ${month} - ${unit.unit_number ? 'وحدة ' + unit.unit_number : unit.name}`;
    invoice = (await c.query(
      `insert into invoices(org_id, unit_id, party_id, number, description, amount, due_date, status)
       values($1, $2, $3, $4, $5, $6, $7, 'draft') returning *`,
      [org, unitId, partyId, invNum, desc, amountStr, dueDate]
    )).rows[0];
  }

  // Issue if draft
  if (invoice.status === 'draft') {
    await issue(c, org, user, invoice.id);
  }

  // Generate receipt reference
  const recRef = customRef || await getNextDocumentNumber(c, org, 'REC', new Date().getFullYear());
  const receipt = await collect(c, org, user, invoice.id, amountStr, `${recRef} (${paymentMethod})`);

  return {
    ok: true,
    invoice_id: invoice.id,
    invoice_number: invoice.number,
    receipt_id: receipt.id,
    receipt_reference: recRef,
    amount: amountStr,
    unit_id: unitId
  };
}

export async function generateMonthDues(c, org, user, data) {
  await permission(c, org, 'finance');
  const month = data.month && /^\d{4}-\d{2}$/.test(data.month) ? data.month : new Date().toISOString().slice(0, 7);
  const { startDate, endDate } = getMonthBounds(month);
  const defaultFee = Number(data.default_fee) || 0;
  if (defaultFee <= 0) throw Object.assign(new Error('Default monthly fee must be positive'), { status: 400 });
  const dueDate = `${month}-01`;

  const units = (await c.query(
    `select u.id, u.property_id, u.unit_number, u.name, l.party_id as lease_party, l.rent, o.party_id as owner_party
     from units u
     left join leases l on l.unit_id = u.id and l.status = 'active'
     left join ownerships o on o.unit_id = u.id
     where u.org_id = $1`,
    [org]
  )).rows;

  let count = 0;
  let total = 0;

  for (const u of units) {
    const existing = (await c.query(
      "select id from invoices where org_id=$1 and unit_id=$2 and due_date >= $3 and due_date <= $4 and status <> 'void' limit 1",
      [org, u.id, startDate, endDate]
    )).rows[0];

    if (!existing) {
      const fee = Number(u.rent) > 0 ? Number(u.rent) : defaultFee;
      const feeStr = toFixedMoney(fee);
      const partyId = u.lease_party || u.owner_party || (await c.query('select id from parties where org_id=$1 limit 1', [org])).rows[0]?.id;
      if (!partyId) continue;

      const invNum = await getNextDocumentNumber(c, org, 'INV', new Date().getFullYear());
      const desc = `اشتراك شهر ${month} - ${u.unit_number ? 'وحدة ' + u.unit_number : u.name}`;
      const inv = (await c.query(
        `insert into invoices(org_id, unit_id, party_id, number, description, amount, due_date, status)
         values($1, $2, $3, $4, $5, $6, $7, 'draft') returning *`,
        [org, u.id, partyId, invNum, desc, feeStr, dueDate]
      )).rows[0];

      await issue(c, org, user, inv.id);
      count++;
      total += fee;
    }
  }

  return {
    ok: true,
    month,
    generated_count: count,
    total_amount: toFixedMoney(total)
  };
}
