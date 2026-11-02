begin;

-- Add unit_number column to units
alter table public.units add column if not exists unit_number text not null default '';
update public.units set unit_number = name where unit_number = '' or unit_number is null;

commit;
