begin;
-- Existing ambiguous ledgers remain readable; never guess how historical money is split.
alter table organizations drop constraint organizations_currency_check;
alter table organizations add constraint organizations_currency_check check(currency ~ '^[A-Z]{3}$');
alter table organizations add column country text check(country ~ '^[A-Z]{2}$');
alter table organizations add column ledger_mode text not null default 'isolated' check(ledger_mode in ('isolated','legacy_review'));
alter table organizations add column archived_at timestamptz;
update organizations o set ledger_mode='legacy_review' where (select count(*) from properties p where p.org_id=o.id)>1;
create function private.building_boundary() returns trigger language plpgsql security definer set search_path='' as $$
declare mode text;
begin
 if private.uid() is not null and not private.can_write(new.org_id,'properties',case when tg_op='INSERT' then 'create' else 'update' end) then return new;end if;
 if tg_op='UPDATE' and new.org_id<>old.org_id then raise exception 'Cannot move a building between ledgers';end if;
 select ledger_mode into mode from public.organizations where id=new.org_id for update;
 if mode='isolated' and exists(select 1 from public.properties where org_id=new.org_id and id<>new.id) then raise exception 'Create another independent building workspace';end if;
 return new;
end$$;
create trigger one_building_per_ledger before insert or update on properties for each row execute function private.building_boundary();
create function private.workspace_guard() returns trigger language plpgsql security definer set search_path='' as $$
declare o public.organizations;
begin
 select * into o from public.organizations where id=new.org_id for update;
 if o.archived_at is not null then raise exception 'Building is archived: read only';end if;
 if o.ledger_mode='legacy_review' and tg_table_name in ('accounts','invoices','invoice_lines','receipts','expenses','expense_payments','treasury_transfers','journals','journal_lines','recurring_charges') then raise exception 'Legacy multi-building ledger needs an approved allocation plan';end if;
 return new;
end$$;
create function private.workspace_config_guard() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.currency<>old.currency and exists(select 1 from public.journals where org_id=old.id and status='posted') then raise exception 'Currency is locked after financial posting';end if;
 if new.ledger_mode<>old.ledger_mode then raise exception 'Ledger mode requires an explicit migration';end if;
 return new;
end$$;
create trigger workspace_configuration before update on organizations for each row execute function private.workspace_config_guard();
alter table accounts add column treasury_kind text check(treasury_kind in ('cash','bank','wallet'));
update accounts set treasury_kind='cash' where code='1100' and kind='asset';
create function private.account_history_guard() returns trigger language plpgsql security definer set search_path='' as $$begin
 if (new.code,new.kind,new.treasury_kind,new.org_id) is distinct from (old.code,old.kind,old.treasury_kind,old.org_id) and exists(select 1 from public.journal_lines l join public.journals j on j.id=l.journal_id where l.account_id=old.id and j.status='posted') then raise exception 'Posted account identity is immutable';end if;return new;
end$$;
create trigger account_history before update on accounts for each row execute function private.account_history_guard();
alter table expenses add column paid_amount numeric(16,2) not null default 0 check(paid_amount>=0 and paid_amount<=amount);
alter table expenses add column approved_at timestamptz;
alter table expenses add column legacy_cash_posted boolean not null default false;
-- Old approvals already posted cash. Mark as paid without posting a second journal.
update expenses set paid_amount=amount,legacy_cash_posted=true where status='approved' and journal_id is not null;
alter table expenses add constraint expenses_org_unique unique(org_id,id);
create table expense_payments(id uuid primary key default gen_random_uuid(),org_id uuid not null,expense_id uuid not null,account_id uuid not null,amount numeric(16,2) not null check(amount>0),reference text not null check(length(reference) between 1 and 160),paid_on date not null,paid_by uuid not null references private.users(id),journal_id uuid not null,created_at timestamptz not null default now(),unique(org_id,reference),foreign key(org_id,expense_id) references expenses(org_id,id),foreign key(org_id,account_id) references accounts(org_id,id),foreign key(org_id,journal_id) references journals(org_id,id));
create table treasury_transfers(id uuid primary key default gen_random_uuid(),org_id uuid not null,source_id uuid not null,target_id uuid not null,amount numeric(16,2) not null check(amount>0),reference text not null check(length(reference) between 1 and 160),transferred_on date not null,created_by uuid not null references private.users(id),journal_id uuid not null,created_at timestamptz not null default now(),check(source_id<>target_id),unique(org_id,reference),foreign key(org_id,source_id) references accounts(org_id,id),foreign key(org_id,target_id) references accounts(org_id,id),foreign key(org_id,journal_id) references journals(org_id,id));
do $$declare t text;begin foreach t in array array['expense_payments','treasury_transfers'] loop
 execute format('alter table public.%I enable row level security',t);
 execute format('grant select on public.%I to towerflow_client',t);
 execute format('create policy finance_read on public.%I for select to towerflow_client using(private.can_read(org_id,''expenses''))',t);
 execute format('create index on public.%I(org_id)',t);
 execute format('create trigger workspace_write_guard before insert or update on public.%I for each row execute function private.workspace_guard()',t);
 execute format('create trigger audit_change after insert on public.%I for each row execute function private.audit()',t);
end loop;end$$;
do $$declare t text;begin foreach t in array array['properties','units','parties','leases','ownerships','accounts','invoices','invoice_lines','receipts','expenses','journals','journal_lines','recurring_charges','assets','technicians','tickets','maintenance_plans','announcements','accounting_periods','ticket_comments'] loop
 execute format('create trigger workspace_write_guard before insert or update on public.%I for each row execute function private.workspace_guard()',t);
end loop;end$$;
revoke all on function private.building_boundary(),private.workspace_guard(),private.workspace_config_guard(),private.account_history_guard() from public;
commit;
