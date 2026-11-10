begin;

-- 1. Support InstaPay & Electronic Wallets settings on properties (Towers)
alter table public.properties add column if not exists instapay_id text not null default '';
alter table public.properties add column if not exists wallet_number text not null default '';
alter table public.properties add column if not exists payment_instructions text not null default '';

-- 2. Support unit occupancy condition matching MulakHub (residing, closed, finishing)
alter table public.units add column if not exists occupancy_state text not null default 'residing';
alter table public.units add column if not exists resident_phone text not null default '';

-- 3. Support one-time emergency projects / liabilities
create table if not exists public.emergency_projects (
  id uuid primary key default gen_random_uuid(),
  org_id uuid not null references public.organizations(id),
  property_id uuid not null references public.properties(id),
  title text not null,
  description text not null default '',
  total_amount numeric(16,2) not null check(total_amount > 0),
  share_per_unit numeric(16,2) not null check(share_per_unit >= 0),
  status text not null default 'active' check(status in ('active','completed','cancelled')),
  created_at timestamptz not null default now(),
  unique(org_id, id)
);

alter table public.emergency_projects enable row level security;
drop policy if exists org_projects on public.emergency_projects;
create policy org_projects on public.emergency_projects for all to towerflow_client
  using(exists(select 1 from private.memberships where org_id = emergency_projects.org_id and user_id = private.uid()));

-- Link invoices to emergency project if applicable
alter table public.invoices add column if not exists project_id uuid references public.emergency_projects(id);
alter table public.invoices add column if not exists liability_type text not null default 'recurring' check(liability_type in ('recurring', 'one_time', 'emergency'));

-- 4. Payment channel recording on receipts
alter table public.receipts add column if not exists payment_channel text not null default 'cash' check(payment_channel in ('cash', 'instapay', 'wallet', 'bank_transfer'));

commit;
