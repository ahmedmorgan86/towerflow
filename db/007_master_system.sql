begin;

-- 1. Property extensions
alter table public.properties 
  add column if not exists currency text not null default 'EGP' check(currency in ('EGP','SAR','AED','USD','KWD','BHD','QAR','OMR','JOD')),
  add column if not exists country text not null default 'EG',
  add column if not exists status text not null default 'active' check(status in ('active','archived')),
  add column if not exists archived_at timestamptz,
  add column if not exists manager_user_id uuid references private.users(id),
  add column if not exists floors_count int not null default 0,
  add column if not exists units_per_floor int not null default 0;

-- 2. Units extension
alter table public.units
  add column if not exists payer_responsibility text not null default 'tenant' check(payer_responsibility in ('owner','tenant')),
  add column if not exists vacant_policy text not null default 'full' check(vacant_policy in ('full','partial','exempt','custom')),
  add column if not exists vacant_rate numeric(5,2) not null default 100 check(vacant_rate >= 0 and vacant_rate <= 100),
  add column if not exists notes text not null default '',
  add column if not exists ownership_type text not null default 'rental' check(ownership_type in ('rental','owned'));
alter table public.units drop constraint if exists units_status_check;
alter table public.units add constraint units_status_check check(status in ('vacant','occupied','maintenance','owned'));

-- 3. Money accounts per Tower
create table if not exists public.money_accounts (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  account_id uuid not null,
  name text not null,
  kind text not null check(kind in ('cash','bank','wallet','maintenance_fund','fines_fund')),
  bank_name text not null default '',
  account_number text not null default '',
  balance numeric(16,2) not null default 0,
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id),
  foreign key(org_id, account_id) references public.accounts(org_id, id)
);

-- 4. Tower tagging on journals to enforce inter-tower financial isolation
alter table public.journals add column if not exists property_id uuid;
do $$begin
  if not exists (select 1 from pg_constraint where conname='journals_property_fk') then
    alter table public.journals add constraint journals_property_fk foreign key(org_id, property_id) references public.properties(org_id, id);
  end if;
end$$;

-- 5. Payment Proofs from residents
create table if not exists public.payment_proofs (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  unit_id uuid not null,
  party_id uuid not null,
  invoice_id uuid references public.invoices(id),
  amount numeric(16,2) not null check(amount > 0),
  payment_method text not null default 'bank_transfer' check(payment_method in ('cash','bank_transfer','wallet','card','other')),
  target_account_id uuid references public.money_accounts(id),
  reference text not null,
  image_url text not null default '',
  status text not null default 'pending' check(status in ('pending','approved','rejected')),
  rejection_reason text not null default '',
  reviewed_by uuid references private.users(id),
  reviewed_at timestamptz,
  receipt_id uuid references public.receipts(id),
  created_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  unique(org_id, id),
  unique(org_id, reference),
  foreign key(org_id, property_id) references public.properties(org_id, id),
  foreign key(org_id, unit_id) references public.units(org_id, id),
  foreign key(org_id, party_id) references public.parties(org_id, id)
);

-- 6. Expense distributions (توزيع المصروفات)
create table if not exists public.expense_distributions (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  title text not null,
  total_amount numeric(16,2) not null check(total_amount > 0),
  method text not null check(method in ('equal','area','percentage','custom')),
  expense_date date not null default current_date,
  notes text not null default '',
  created_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id)
);

create table if not exists public.distribution_shares (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  distribution_id uuid not null,
  unit_id uuid not null,
  party_id uuid not null,
  share_amount numeric(16,2) not null check(share_amount >= 0),
  is_exempt boolean not null default false,
  exemption_source text check(exemption_source in ('fund','redistribute')),
  invoice_id uuid references public.invoices(id),
  foreign key(org_id, distribution_id) references public.expense_distributions(org_id, id),
  foreign key(org_id, unit_id) references public.units(org_id, id),
  foreign key(org_id, party_id) references public.parties(org_id, id)
);

-- 7. Fines and late fees (الغرامات)
create table if not exists public.fines (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  unit_id uuid not null,
  party_id uuid not null,
  invoice_id uuid references public.invoices(id),
  title text not null,
  amount numeric(16,2) not null check(amount > 0),
  status text not null default 'pending' check(status in ('pending','collected','exempt')),
  destination text not null default 'maintenance_fund' check(destination in ('maintenance_fund','fines_fund')),
  exemption_reason text not null default '',
  exempted_by uuid references private.users(id),
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id),
  foreign key(org_id, unit_id) references public.units(org_id, id),
  foreign key(org_id, party_id) references public.parties(org_id, id)
);

-- 8. Advance payments & Credit balances (الأرصدة الدائنة)
create table if not exists public.credit_balances (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  party_id uuid not null,
  unit_id uuid,
  balance numeric(16,2) not null default 0 check(balance >= 0),
  created_at timestamptz not null default now(),
  unique(org_id, property_id, party_id),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id),
  foreign key(org_id, party_id) references public.parties(org_id, id)
);

create table if not exists public.credit_transactions (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  credit_balance_id uuid not null,
  amount numeric(16,2) not null check(amount > 0),
  action text not null check(action in ('credit_in','apply_invoice','refund')),
  invoice_id uuid references public.invoices(id),
  journal_id uuid references public.journals(id),
  notes text not null default '',
  created_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  foreign key(org_id, credit_balance_id) references public.credit_balances(org_id, id)
);

-- 9. Vendor contracts & Versions (عقود الخدمات والموردين)
create table if not exists public.vendor_contracts (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  vendor_id uuid not null,
  title text not null,
  service_type text not null,
  amount numeric(16,2) not null check(amount > 0),
  frequency text not null default 'monthly' check(frequency in ('monthly','quarterly','yearly','one_time')),
  start_date date not null,
  end_date date not null,
  auto_renew boolean not null default false,
  status text not null default 'active' check(status in ('active','terminated','expired')),
  terms text not null default '',
  created_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  check(end_date >= start_date),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id),
  foreign key(org_id, vendor_id) references public.parties(org_id, id)
);

create table if not exists public.contract_versions (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  contract_id uuid not null,
  version_number int not null default 1,
  amount numeric(16,2) not null check(amount > 0),
  terms text not null default '',
  start_date date not null,
  end_date date not null,
  changed_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  foreign key(org_id, contract_id) references public.vendor_contracts(org_id, id)
);

-- 10. Budgets per category (الميزانيات)
create table if not exists public.budgets (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  name text not null,
  period text not null default 'annual' check(period in ('monthly','quarterly','annual')),
  start_date date not null,
  end_date date not null,
  overrun_policy text not null default 'warn' check(overrun_policy in ('warn','require_approval')),
  rollover_policy text not null default 'none' check(rollover_policy in ('none','auto','require_approval')),
  status text not null default 'active' check(status in ('draft','active','closed')),
  created_at timestamptz not null default now(),
  check(end_date >= start_date),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id)
);

create table if not exists public.budget_items (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  budget_id uuid not null,
  account_id uuid not null,
  allocated numeric(16,2) not null check(allocated >= 0),
  spent numeric(16,2) not null default 0 check(spent >= 0),
  rolled_over numeric(16,2) not null default 0 check(rolled_over >= 0),
  foreign key(org_id, budget_id) references public.budgets(org_id, id),
  foreign key(org_id, account_id) references public.accounts(org_id, id)
);

-- 11. Account reconciliations (مطابقة الخزائن والبنوك)
create table if not exists public.account_reconciliations (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  money_account_id uuid not null,
  statement_date date not null default current_date,
  book_balance numeric(16,2) not null,
  actual_balance numeric(16,2) not null,
  difference numeric(16,2) not null,
  reason text not null default '',
  document_ref text not null default '',
  reconciled_by uuid not null default private.uid(),
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, money_account_id) references public.money_accounts(org_id, id)
);

-- 12. Document sequence generator
create table if not exists private.document_sequences (
  org_id uuid not null references public.organizations(id),
  document_type text not null,
  fiscal_year int not null default 0,
  last_number int not null default 0,
  primary key(org_id, document_type, fiscal_year)
);

-- 13. Tower Management Transfers & Delegations (نقل الإدارة والتفويضات)
create table if not exists public.management_transfers (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  from_user_id uuid not null references private.users(id),
  to_user_id uuid not null references private.users(id),
  status text not null default 'pending' check(status in ('pending','accepted','rejected','cancelled')),
  notes text not null default '',
  responded_at timestamptz,
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id)
);

create table if not exists public.delegations (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null,
  user_id uuid not null references private.users(id),
  role_title text not null default 'assistant',
  approval_limit numeric(16,2) not null default 0 check(approval_limit >= 0),
  active boolean not null default true,
  granted_by uuid not null default private.uid(),
  expires_at timestamptz,
  created_at timestamptz not null default now(),
  unique(org_id, id),
  foreign key(org_id, property_id) references public.properties(org_id, id)
);

-- 14. Support tickets & Temporary access (الدعم الفني والوصول المؤقت)
create table if not exists public.support_tickets (
  id uuid primary key default gen_random_uuid(),
  ticket_number text not null unique,
  user_id uuid not null references private.users(id),
  org_id uuid references public.organizations(id),
  property_id uuid references public.properties(id),
  edition text not null default 'free' check(edition in ('free','commercial')),
  title text not null,
  description text not null,
  category text not null default 'general' check(category in ('general','billing','accounting','technical','security')),
  priority text not null default 'normal' check(priority in ('low','normal','high','critical')),
  status text not null default 'open' check(status in ('open','in_progress','waiting_user','resolved','closed')),
  assigned_to uuid references private.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.support_messages (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets(id),
  sender_user_id uuid not null references private.users(id),
  is_staff boolean not null default false,
  message text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.support_grants (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets(id),
  org_id uuid not null references public.organizations(id),
  property_id uuid references public.properties(id),
  support_user_id uuid not null references private.users(id),
  approved_by_user_id uuid references private.users(id),
  status text not null default 'requested' check(status in ('requested','approved','revoked','expired')),
  allow_download boolean not null default false,
  expires_at timestamptz not null,
  created_at timestamptz not null default now(),
  unique(id)
);

-- 15. Commercial Subscription Packages (الباقات والاشتراكات التجارية)
create table if not exists public.subscription_plans (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  name text not null,
  monthly_price numeric(12,2) not null default 0,
  annual_price numeric(12,2) not null default 0,
  max_properties int not null default 1,
  priority_support boolean not null default true,
  trial_days int not null default 14,
  active boolean not null default true
);

create table if not exists public.property_subscriptions (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  plan_id uuid not null references public.subscription_plans(id),
  edition text not null default 'commercial' check(edition in ('free','commercial')),
  status text not null default 'active' check(status in ('active','trialing','past_due','cancelled','expired')),
  trial_ends_at timestamptz,
  current_period_end timestamptz not null,
  created_at timestamptz not null default now(),
  unique(org_id)
);

-- 16. Enable RLS and setup policies on all new tables
do $$declare t text;begin
 foreach t in array array[
   'money_accounts','payment_proofs','expense_distributions','distribution_shares',
   'fines','credit_balances','credit_transactions','vendor_contracts','contract_versions',
   'budgets','budget_items','account_reconciliations','management_transfers','delegations',
   'support_tickets','support_messages','support_grants','subscription_plans','property_subscriptions'
 ] loop
   execute format('alter table public.%I enable row level security', t);
   if exists (select 1 from information_schema.columns where table_schema='public' and table_name=t and column_name='org_id') then
     execute format('create index if not exists %I on public.%I(org_id)', 'idx_'||t||'_org', t);
   end if;
   execute format('grant select on public.%I to towerflow_client', t);
   if t in ('money_accounts','payment_proofs','expense_distributions','distribution_shares',
            'fines','credit_balances','credit_transactions','vendor_contracts','contract_versions',
            'budgets','budget_items','account_reconciliations','management_transfers','delegations') then
      execute format('drop policy if exists staff_read on public.%I', t);
     execute format('create policy staff_read on public.%I for select to towerflow_client using(private.can_read(org_id,%L))', t, t);
     execute format('grant insert,update on public.%I to towerflow_client', t);
      execute format('drop policy if exists staff_insert on public.%I', t);
     execute format('create policy staff_insert on public.%I for insert to towerflow_client with check(private.can_write(org_id,%L,''create''))', t, t);
      execute format('drop policy if exists staff_update on public.%I', t);
     execute format('create policy staff_update on public.%I for update to towerflow_client using(private.can_write(org_id,%L,''update'')) with check(private.can_write(org_id,%L,''update''))', t, t, t);
      execute format('drop trigger if exists audit_change on public.%I', t);
     execute format('create trigger audit_change after insert or update on public.%I for each row execute function private.audit()', t);
   end if;
 end loop;
end $$;

-- Resident access policies for payment proofs, fines, and personal support tickets
drop policy if exists resident_proofs on public.payment_proofs;
create policy resident_proofs on public.payment_proofs for select to towerflow_client
  using(created_by = private.uid() or private.resident_party(org_id, party_id));

drop policy if exists resident_proof_insert on public.payment_proofs;
create policy resident_proof_insert on public.payment_proofs for insert to towerflow_client
  with check(created_by = private.uid() and private.resident_party(org_id, party_id) and status = 'pending');

drop policy if exists resident_fines on public.fines;
create policy resident_fines on public.fines for select to towerflow_client
  using(private.resident_party(org_id, party_id));

drop policy if exists resident_credit_balances on public.credit_balances;
create policy resident_credit_balances on public.credit_balances for select to towerflow_client
  using(private.resident_party(org_id, party_id));

drop policy if exists user_support_tickets on public.support_tickets;
create policy user_support_tickets on public.support_tickets for select to towerflow_client
  using(user_id = private.uid());

drop policy if exists user_support_messages on public.support_messages;
create policy user_support_messages on public.support_messages for select to towerflow_client
  using(exists(select 1 from public.support_tickets st where st.id = ticket_id and (st.user_id = private.uid() or st.assigned_to = private.uid())));

drop policy if exists public_plans on public.subscription_plans;
create policy public_plans on public.subscription_plans for select to towerflow_client
  using(active = true);

drop policy if exists org_subscriptions on public.property_subscriptions;
create policy org_subscriptions on public.property_subscriptions for select to towerflow_client
  using(exists(select 1 from private.memberships where org_id = property_subscriptions.org_id and user_id = private.uid()));

-- Insert standard subscription plans if empty
insert into public.subscription_plans(code, name, monthly_price, annual_price, max_properties, priority_support, trial_days)
values
  ('free', 'TowerFlow Free', 0, 0, 999, false, 0),
  ('standard', 'TowerFlow Commercial Standard', 299, 2990, 5, true, 14),
  ('enterprise', 'TowerFlow Commercial Enterprise', 799, 7990, 25, true, 30)
on conflict(code) do nothing;

commit;
