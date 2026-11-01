import {getNextDocumentNumber} from './tower-operations.mjs';
import {permission} from './finance.mjs';

const bad = (s, status = 400) => Object.assign(new Error(s), {status});

// 1. Support Tickets
export async function createSupportTicket(c, user, data) {
  if (!data.title || !String(data.title).trim() || !data.description) {
    throw bad('Title and description required');
  }
  const orgId = data.org_id || null;
  const propertyId = data.property_id || null;
  const category = data.category || 'general';
  const priority = data.priority || 'normal';
  const edition = data.edition || 'free';

  const ticketNumber = await getNextDocumentNumber(c, orgId || '00000000-0000-0000-0000-000000000000', 'TICKET', new Date().getFullYear());

  const ticket = (await c.query(
    `insert into support_tickets (ticket_number, user_id, org_id, property_id, edition, title, description, category, priority, status)
     values ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'open')
     returning *`,
    [
      ticketNumber,
      user,
      orgId,
      propertyId,
      edition,
      String(data.title).slice(0, 200),
      String(data.description).slice(0, 4000),
      category,
      priority
    ]
  )).rows[0];

  await c.query(
    `insert into support_messages (ticket_id, sender_user_id, is_staff, message)
     values ($1, $2, false, $3)`,
    [ticket.id, user, String(data.description).slice(0, 4000)]
  );

  return ticket;
}

export async function addSupportMessage(c, user, ticketId, message, isStaff = false) {
  if (!String(message || '').trim()) throw bad('Message required');
  const t = (await c.query('select * from support_tickets where id=$1', [ticketId])).rows[0];
  if (!t) throw bad('Support ticket not found', 404);

  // Non-staff can only message their own tickets
  if (!isStaff && t.user_id !== user) {
    throw bad('Unauthorized to reply to this ticket', 403);
  }

  const msg = (await c.query(
    `insert into support_messages (ticket_id, sender_user_id, is_staff, message)
     values ($1, $2, $3, $4)
     returning *`,
    [ticketId, user, isStaff, String(message).slice(0, 4000)]
  )).rows[0];

  // Update ticket timestamp and status
  const nextStatus = isStaff ? 'waiting_user' : 'in_progress';
  await c.query(
    'update support_tickets set status=$1, updated_at=now() where id=$2',
    [nextStatus, ticketId]
  );

  return msg;
}

// 2. Temporary Support Access Grant (طلب وموافقة الوصول المؤقت للدعم الفني)
export async function requestSupportAccess(c, supportUserId, ticketId, orgId, propertyId, hours = 2) {
  const ticket = (await c.query('select * from support_tickets where id=$1', [ticketId])).rows[0];
  if (!ticket) throw bad('Support ticket not found', 404);

  const durationHours = Math.max(1, Math.min(24, Number(hours || 2)));
  const expiresAt = new Date(Date.now() + durationHours * 3600 * 1000);

  return (await c.query(
    `insert into support_grants (ticket_id, org_id, property_id, support_user_id, status, expires_at)
     values ($1, $2, $3, $4, 'requested', $5)
     returning *`,
    [ticketId, orgId, propertyId || null, supportUserId, expiresAt]
  )).rows[0];
}

export async function approveSupportAccess(c, managerUserId, grantId, allowDownload = false) {
  const grant = (await c.query('select * from support_grants where id=$1 for update', [grantId])).rows[0];
  if (!grant) throw bad('Support grant not found', 404);
  if (grant.status !== 'requested') throw bad('Grant is not in requested state', 409);

  // Verify manager has admin or manager capability on that org
  await permission(c, grant.org_id, 'manage');

  return (await c.query(
    `update support_grants
     set status='approved', approved_by_user_id=$1, allow_download=$2
     where id=$3
     returning *`,
    [managerUserId, allowDownload, grantId]
  )).rows[0];
}

export async function revokeSupportAccess(c, managerUserId, grantId) {
  const grant = (await c.query('select * from support_grants where id=$1 for update', [grantId])).rows[0];
  if (!grant) throw bad('Support grant not found', 404);
  await permission(c, grant.org_id, 'manage');

  return (await c.query(
    "update support_grants set status='revoked' where id=$1 returning *",
    [grantId]
  )).rows[0];
}

export async function checkSupportAccessActive(c, supportUserId, orgId) {
  const active = (await c.query(
    `select * from support_grants
     where support_user_id=$1 and org_id=$2 and status='approved' and expires_at > now()
     limit 1`,
    [supportUserId, orgId]
  )).rows[0];
  return !!active;
}
