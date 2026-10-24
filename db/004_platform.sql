begin;
create table private.permission_rules(org_id uuid not null,user_id uuid not null,capability text not null check(capability in ('read','manage','finance','maintain') or capability ~ '^(read|create|update):[a-z_]+$'),allowed boolean not null,primary key(org_id,user_id,capability),foreign key(org_id,user_id) references private.memberships(org_id,user_id));
create or replace function private.can(o uuid, capability text) returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from private.memberships m where m.org_id=o and m.user_id=private.uid() and
 (m.role='admin' or coalesce((select r.allowed from private.permission_rules r where r.org_id=o and r.user_id=m.user_id and r.capability=can.capability),
 (capability='read' and m.role in ('manager','accountant','technician','viewer')) or (capability='manage' and m.role='manager') or (capability='finance' and m.role in ('manager','accountant')) or (capability='maintain' and m.role in ('manager','technician')))));
$$;
create or replace function private.can_read(o uuid, entity text) returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from private.memberships m where m.org_id=o and m.user_id=private.uid() and (m.role='admin' or coalesce((select r.allowed from private.permission_rules r where r.org_id=o and r.user_id=m.user_id and r.capability='read:'||entity),private.can(o,'read') and (entity not in ('parties','leases','ownerships','accounts','journals','journal_lines','invoices','receipts','expenses','audit_logs','accounting_periods','recurring_charges','invoice_lines') or m.role in ('manager','accountant','viewer')))));
$$;
create function private.can_write(o uuid, entity text, verb text) returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from private.memberships m where m.org_id=o and m.user_id=private.uid() and (m.role='admin' or coalesce((select r.allowed from private.permission_rules r where r.org_id=o and r.user_id=m.user_id and r.capability=verb||':'||entity),private.can(o,case when entity in ('accounts','journals','journal_lines','invoices','receipts','expenses','recurring_charges','invoice_lines') then 'finance' when entity in ('assets','technicians','tickets','maintenance_plans') then 'maintain' else 'manage' end))));
$$;
revoke all on function private.can_write(uuid,text,text) from public;
grant execute on function private.can_write(uuid,text,text) to towerflow_client;
do $$declare t text;begin foreach t in array array['properties','units','parties','leases','ownerships','accounts','technicians','assets','tickets','announcements'] loop
 execute format('alter policy staff_insert on public.%I with check(private.can_write(org_id,%L,''create''))',t,t);
 execute format('alter policy staff_update on public.%I using(private.can_write(org_id,%L,''update'')) with check(private.can_write(org_id,%L,''update''))',t,t,t);
 end loop;end$$;
create table public.accounting_periods(id uuid primary key default gen_random_uuid(),org_id uuid not null references organizations(id),name text not null,start_date date not null,end_date date not null,status text not null default 'open' check(status in ('open','closed')),closed_by uuid,closed_at timestamptz,check(end_date>=start_date),unique(org_id,id));
alter table journals add column reverses_id uuid;
alter table journals add constraint journal_reversal_org foreign key(org_id,reverses_id) references journals(org_id,id);
create unique index one_reversal_per_journal on journals(reverses_id) where reverses_id is not null;
create function private.period_guard() returns trigger language plpgsql security definer set search_path='' as $$begin
 perform 1 from public.organizations where id=new.org_id for update;
 if exists(select 1 from public.accounting_periods p where p.org_id=new.org_id and p.status='closed' and new.entry_date between p.start_date and p.end_date) then raise exception 'Accounting period is closed';end if;
 return new;end$$;
revoke all on function private.period_guard() from public;
create trigger period_lock before insert or update on journals for each row execute function private.period_guard();
create table public.invoice_lines(id uuid primary key default gen_random_uuid(),org_id uuid not null,invoice_id uuid not null,description text not null,quantity numeric(12,2) not null check(quantity>0),unit_price numeric(16,2) not null check(unit_price>0),tax_percent numeric(5,2) not null default 0 check(tax_percent>=0 and tax_percent<=100),line_total numeric(16,2) not null check(line_total>0),foreign key(org_id,invoice_id) references invoices(org_id,id));
alter table invoices add column subtotal numeric(16,2);
alter table invoices add column tax_amount numeric(16,2) not null default 0;
create table public.recurring_charges(id uuid primary key default gen_random_uuid(),org_id uuid not null references organizations(id),party_id uuid not null,unit_id uuid,name text not null,amount numeric(16,2) not null check(amount>0),frequency text not null default 'monthly' check(frequency in ('monthly','quarterly','yearly')),next_due date not null,end_date date,anchor_day int check(anchor_day between 1 and 31),active boolean not null default true,unique(org_id,id),foreign key(org_id,party_id) references parties(org_id,id),foreign key(org_id,unit_id) references units(org_id,id));
create table private.billing_runs(charge_id uuid not null references recurring_charges(id),due_date date not null,invoice_id uuid not null references invoices(id),primary key(charge_id,due_date));
create table public.maintenance_plans(id uuid primary key default gen_random_uuid(),org_id uuid not null references organizations(id),asset_id uuid not null,name text not null,interval_days int not null check(interval_days between 1 and 3660),next_due date not null,active boolean not null default true,unique(org_id,id),foreign key(org_id,asset_id) references assets(org_id,id));
alter table tickets add column due_at timestamptz;
alter table tickets add column resolved_at timestamptz;
alter table tickets add column plan_id uuid;
alter table tickets add constraint ticket_plan_org foreign key(org_id,plan_id) references maintenance_plans(org_id,id);
alter table tickets add constraint tickets_org_unique unique(org_id,id);
create table private.maintenance_runs(plan_id uuid not null references maintenance_plans(id),due_date date not null,ticket_id uuid not null references tickets(id),primary key(plan_id,due_date));
create table public.ticket_comments(id uuid primary key default gen_random_uuid(),org_id uuid not null references organizations(id),ticket_id uuid not null,body text not null check(length(body) between 1 and 4000),created_by uuid not null default private.uid(),created_at timestamptz not null default now(),foreign key(org_id,ticket_id) references tickets(org_id,id));
do $$declare t text;begin foreach t in array array['accounting_periods','invoice_lines','recurring_charges','maintenance_plans','ticket_comments'] loop
 execute format('alter table public.%I enable row level security',t);
 execute format('create index on public.%I(org_id)',t);
 execute format('grant select on public.%I to towerflow_client',t);
 execute format('create policy staff_read on public.%I for select to towerflow_client using(private.can_read(org_id,%L))',t,t);
 execute format('create trigger audit_change after insert or update on public.%I for each row execute function private.audit()',t);
 end loop;end$$;
create policy resident_invoice_lines on invoice_lines for select to towerflow_client using(exists(select 1 from invoices i where i.id=invoice_id and i.org_id=invoice_lines.org_id and private.resident_party(i.org_id,i.party_id)));
create policy resident_comments on ticket_comments for select to towerflow_client using(exists(select 1 from tickets t where t.id=ticket_id and t.org_id=ticket_comments.org_id and t.created_by=private.uid() and private.resident_unit(t.org_id,t.unit_id)));
create policy resident_receipts on receipts for select to towerflow_client using(exists(select 1 from invoices i where i.id=invoice_id and i.org_id=receipts.org_id and private.resident_party(i.org_id,i.party_id)));
commit;
