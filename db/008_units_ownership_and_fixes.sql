begin;

-- 1. Ensure units extension columns exist
alter table public.units
  add column if not exists payer_responsibility text not null default 'tenant' check(payer_responsibility in ('owner','tenant')),
  add column if not exists vacant_policy text not null default 'full' check(vacant_policy in ('full','partial','exempt','custom')),
  add column if not exists vacant_rate numeric(5,2) not null default 100 check(vacant_rate >= 0 and vacant_rate <= 100),
  add column if not exists notes text not null default '',
  add column if not exists ownership_type text not null default 'rental' check(ownership_type in ('rental','owned'));

alter table public.units drop constraint if exists units_status_check;
alter table public.units add constraint units_status_check check(status in ('vacant','occupied','maintenance','owned'));

-- 2. Ensure property extension columns exist
alter table public.properties 
  add column if not exists currency text not null default 'EGP' check(currency in ('EGP','SAR','AED','USD','KWD','BHD','QAR','OMR','JOD')),
  add column if not exists country text not null default 'EG',
  add column if not exists status text not null default 'active' check(status in ('active','archived')),
  add column if not exists archived_at timestamptz,
  add column if not exists manager_user_id uuid references private.users(id),
  add column if not exists floors_count int not null default 0,
  add column if not exists units_per_floor int not null default 0;

-- 3. Ensure resident unit access helper exists
create or replace function private.resident_unit(o uuid,u uuid) returns boolean language sql stable security definer set search_path='' as $$select private.uid() is not null and (exists(select 1 from public.leases l join public.parties p on p.id=l.party_id and p.org_id=l.org_id where l.org_id=o and l.unit_id=u and l.status='active' and p.user_id=private.uid()) or exists(select 1 from public.ownerships ow join public.parties p on p.id=ow.party_id and p.org_id=ow.org_id where ow.org_id=o and ow.unit_id=u and p.user_id=private.uid()))$$;

commit;
