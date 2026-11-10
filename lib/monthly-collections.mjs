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

  // Auto-heal any owned units erroneously marked as vacant
  try {
    await c.query(
      `update units
       set status = 'owned'
       where org_id = $1
         and (ownership_type = 'owned' or id in (select unit_id from ownerships where org_id = $1))
         and status = 'vacant'`,
      [org]
    );
  } catch (_) {}

  const now = new Date();
  const currentMonth = monthParam && /^\d{4}-\d{2}$/.test(monthParam) ? monthParam : now.toISOString().slice(0, 7);
  const { startDate, endDate } = getMonthBounds(currentMonth);

  // 0. Fetch Tower / Property Payment Settings (InstaPay & Wallets)
  const propRes = await c.query(
    "select id, name, coalesce(instapay_id, '') as instapay_id, coalesce(wallet_number, '') as wallet_number, coalesce(payment_instructions, '') as payment_instructions from properties where org_id = $1 limit 1",
    [org]
  );
  const propertySettings = propRes.rows[0] || { id: null, name: '', instapay_id: '', wallet_number: '', payment_instructions: '' };

  // 1. Fetch all units for organization
  const unitsRes = await c.query(
    `select u.id, u.property_id, coalesce(nullif(u.unit_number, ''), u.name) as unit_number,
            u.name as unit_name, coalesce(u.floor, 0) as floor, coalesce(u.area, 0) as area,
            coalesce(u.status, 'vacant') as status, coalesce(u.ownership_type, 'rental') as ownership_type, coalesce(u.occupancy_state, 'residing') as occupancy_state, coalesce(u.resident_phone, '') as u_phone,
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

    // Is the unit owned (تمليك)?
    const isOwned = unit.ownership_type === 'owned' || unit.status === 'owned' || Boolean(owner);
    const isLeased = Boolean(lease);

    // Resident kind: Leased unit has tenant, owned unit has owner
    const residentKind = isLeased ? 'tenant' : (isOwned ? 'owner' : (owner ? 'owner' : 'tenant'));

    // Resolve Resident Name & Phone (An owned unit is NEVER displayed as vacant/شاغرة)
    let residentName = '';
    let residentPhone = '';
    if (lease?.party_name) {
      residentName = lease.party_name;
      residentPhone = lease.party_phone || '';
    } else if (owner?.party_name) {
      residentName = owner.party_name;
      residentPhone = owner.party_phone || unit.u_phone || '';
    } else if (isOwned) {
      residentName = 'مالك الوحدة (تمليك)';
    } else if (unit.status === 'occupied') {
      residentName = 'ساكن الوحدة';
    } else {
      residentName = 'شاغرة للإيجار';
    }

    const partyId = lease?.party_id || owner?.party_id || null;

    let fee = 0;
    if (inv) {
      fee = Number(inv.amount) || 0;
    } else {
      fee = 0;
    }

    const paid = inv ? (Number(inv.paid_amount) || 0) : 0;
    const isPaid = inv ? (inv.status === 'paid' || paid >= fee && fee > 0) : false;

    // Occupied count: owned units are occupied by their owners by definition
    if (isOwned || unit.status === 'occupied' || isLeased || fee > 0) occupiedCount++;
    totalDue += fee;
    totalCollected += isPaid ? fee : paid;

    return {
      unit_id: unit.id,
      property_id: unit.property_id,
      property_name: unit.property_name || '',
      unit_number: unit.unit_number,
      unit_name: unit.unit_name,
      floor: unit.floor,
      status: isOwned ? 'owned' : unit.status,
      ownership_type: isOwned ? 'owned' : unit.ownership_type,
      occupancy_state: unit.occupancy_state || 'residing',
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
    property_settings: propertySettings,
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
  const paymentMethod = data.payment_channel || data.method || 'cash';
  const customRef = data.reference && data.reference.trim() ? data.reference.trim() : null;

  // 1. Fetch unit
  const unit = (await c.query('select * from units where id=$1 and org_id=$2', [unitId, org])).rows[0];
  if (!unit) throw Object.assign(new Error('Unit not found'), { status: 404 });

  // 2. Resolve party
  let partyId = data.party_id;
  const isOwnedUnit = unit.ownership_type === 'owned' || unit.status === 'owned';
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
        "insert into parties(org_id, name, kind) values($1, $2, $3) returning id",
        [org, isOwnedUnit ? `مالك وحدة ${unit.unit_number || unit.name}` : `ساكن ${unit.unit_number || unit.name}`, isOwnedUnit ? 'owner' : 'tenant']
      )).rows[0];
      partyId = created.id;
    }
    if (isOwnedUnit) {
      try {
        await c.query("insert into ownerships(org_id, unit_id, party_id, share) values($1, $2, $3, 100) on conflict do nothing", [org, unitId, partyId]);
      } catch (_) {}
    }
  }

  // 3. Check if invoice already exists for this unit in this month
  let invoice = (await c.query(
    "select * from invoices where org_id=$1 and unit_id=$2 and due_date >= $3 and due_date <= $4 and status <> 'void' limit 1",
    [org, unitId, startDate, endDate]
  )).rows[0];

  if (!invoice) {
    const invNum = await getNextDocumentNumber(c, org, 'INV', new Date().getFullYear());
    const desc = `اشتراك صيانة وخدمات شهر ${month} - شقة ${unit.unit_number ? unit.unit_number : unit.name}`;
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
      const fee = defaultFee;
      const feeStr = toFixedMoney(fee);
      let partyId = u.lease_party || u.owner_party || (await c.query('select id from parties where org_id=$1 limit 1', [org])).rows[0]?.id;
      if (!partyId) {
        const p = (await c.query("insert into parties(org_id, name, kind) values($1, 'سكان وملاك البرج', 'resident') returning id", [org])).rows[0];
        partyId = p.id;
      }

      const invNum = await getNextDocumentNumber(c, org, 'INV', new Date().getFullYear());
      const desc = `اشتراك صيانة وخدمات شهر ${month} - شقة ${u.unit_number ? u.unit_number : u.name}`;
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

export async function assignResident(c, org, user, unitId, data) {
  await permission(c, org, 'manage');
  const unit = (await c.query('select * from units where id=$1 and org_id=$2', [unitId, org])).rows[0];
  if (!unit) throw Object.assign(new Error('Unit not found'), { status: 404 });

  const name = (data.name || '').trim();
  if (!name) throw Object.assign(new Error('Name is required'), { status: 400 });
  const phone = (data.phone || '').trim();
  const kind = data.kind === 'tenant' ? 'tenant' : 'owner';

  // 1. Find or create party
  let party = (await c.query(
    'select * from parties where org_id=$1 and name=$2 limit 1',
    [org, name]
  )).rows[0];

  if (!party) {
    party = (await c.query(
      'insert into parties(org_id, name, kind, phone) values($1, $2, $3, $4) returning *',
      [org, name, kind, phone]
    )).rows[0];
  } else if (phone && !party.phone) {
    await c.query('update parties set phone=$1 where id=$2 and org_id=$3', [phone, party.id, org]);
  if (data.occupancy_state) {
    await c.query('update units set occupancy_state=$1, resident_phone=$2 where org_id=$3 and id=$4', [data.occupancy_state, phone, org, unitId]);
  } else if (phone) {
    await c.query('update units set resident_phone=$1 where org_id=$2 and id=$3', [phone, org, unitId]);
  }
  }

  // 2. Link based on kind
  if (kind === 'owner') {
    await c.query('delete from ownerships where org_id=$1 and unit_id=$2', [org, unitId]);
    await c.query('insert into ownerships(org_id, unit_id, party_id, share) values($1, $2, $3, 100)', [org, unitId, party.id]);
    await c.query("update units set status='owned', ownership_type='owned' where org_id=$1 and id=$2", [org, unitId]);
  } else {
    await c.query("update leases set status='ended' where org_id=$1 and unit_id=$2 and status='active'", [org, unitId]);
    const today = new Date().toISOString().slice(0, 10);
    const nextYear = new Date(Date.now() + 365*24*3600*1000).toISOString().slice(0, 10);
    const rent = Number(data.monthly_fee) || 1000;
    await c.query(
      "insert into leases(org_id, unit_id, party_id, start_date, end_date, rent, status) values($1, $2, $3, $4, $5, $6, 'active')",
      [org, unitId, party.id, today, nextYear, toFixedMoney(rent)]
    );
    await c.query("update units set status='occupied', ownership_type='rental' where org_id=$1 and id=$2", [org, unitId]);
  }

  return { ok: true, party_id: party.id, party_name: party.name, phone: party.phone };
}
