begin;
create schema if not exists private;
revoke all on schema private from public;
do $$ begin if not exists(select 1 from pg_roles where rolname='towerflow_client') then create role towerflow_client nologin nosuperuser nobypassrls; end if; end $$;
grant towerflow_client to current_user;
create table private.users(id uuid primary key default gen_random_uuid(), email text not null unique, password_hash text not null, name text not null, created_at timestamptz not null default now());
create table private.sessions(token_hash text primary key,user_id uuid not null references private.users(id),expires_at timestamptz not null);
create table private.login_limits(key text primary key,attempts int not null default 0, window_start timestamptz not null default now());
create table public.organizations(id uuid primary key default gen_random_uuid(), name text not null check(length(name) between 1 and 160),currency text not null default 'EGP' check(currency in ('EGP','SAR','AED','USD')),created_at timestamptz not null default now());
create table private.memberships(org_id uuid not null references public.organizations(id), user_id uuid not null references private.users(id),role text not null check(role in ('admin','manager','accountant','technician','resident','viewer')),primary key(org_id,user_id));
create function private.uid() returns uuid language sql stable as $$select nullif(current_setting('app.user_id',true),'')::uuid$$;
create function private.can(o uuid, capability text) returns boolean language sql stable security definer set search_path='' as $$
 select private.uid() is not null and exists(select 1 from private.memberships m where m.org_id=o and m.user_id=private.uid() and
 (m.role='admin' or (capability='read' and m.role in ('manager','accountant','technician','viewer')) or
 (capability='manage' and m.role='manager') or (capability='finance' and m.role in ('manager','accountant')) or (capability='maintain' and m.role in ('manager','technician'))));
$$;
revoke all on function private.can(uuid,text) from public;
grant usage on schema private to towerflow_client;
grant execute on function private.can(uuid,text),private.uid() to towerflow_client;
create table public.properties(id uuid primary key default gen_random_uuid(), org_id uuid not null references public.organizations(id),name text not null, address text not null default '',kind text not null default 'tower',created_at timestamptz default now(),unique(org_id,id));
create table public.units(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),property_id uuid not null, name text not null,floor int not null default 0, area numeric(12,2) check(area>0),status text not null default 'vacant' check(status in ('vacant','occupied','maintenance')),unique(org_id,id),unique(property_id,name),foreign key(org_id,property_id) references public.properties(org_id,id));
create table public.parties(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),name text not null,kind text not null check(kind in ('owner','tenant','vendor')),email text not null default '',phone text not null default '',user_id uuid references private.users(id),unique(org_id,id));
create table public.leases(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),unit_id uuid not null,party_id uuid not null,start_date date not null,end_date date not null,rent numeric(16,2) not null check(rent>0),status text not null default 'active' check(status in ('active','ended')),check(end_date>=start_date),unique(org_id,id),foreign key(org_id,unit_id) references public.units(org_id,id),foreign key(org_id,party_id) references public.parties(org_id,id));
create unique index one_active_lease on public.leases(unit_id) where status='active';
create table public.ownerships(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),unit_id uuid not null,party_id uuid not null,share numeric(5,2) not null check(share>0 and share<=100),foreign key(org_id,unit_id) references public.units(org_id,id),foreign key(org_id,party_id) references public.parties(org_id,id),unique(unit_id,party_id));
create table public.accounts(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),code text not null,name text not null,kind text not null check(kind in ('asset','liability','equity','income','expense')),unique(org_id,id),unique(org_id,code));
create table public.journals(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),description text not null,entry_date date not null default current_date,status text not null default 'draft' check(status in ('draft','posted')),created_by uuid not null default private.uid(),created_at timestamptz not null default now(),unique(org_id,id));
create table public.journal_lines(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),journal_id uuid not null,account_id uuid not null,debit numeric(16,2) not null default 0,credit numeric(16,2) not null default 0,check((debit>0 and credit=0) or (credit>0 and debit=0)),foreign key(org_id,journal_id) references public.journals(org_id,id),foreign key(org_id,account_id) references public.accounts(org_id,id));
create table public.invoices(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),party_id uuid not null,unit_id uuid,number text not null,description text not null,amount numeric(16,2) not null check(amount>0),due_date date not null,status text not null default 'draft' check(status in ('draft','issued','paid')),journal_id uuid,unique(org_id,id),unique(org_id,number),foreign key(org_id,party_id) references public.parties(org_id,id),foreign key(org_id,unit_id) references public.units(org_id,id),foreign key(org_id,journal_id) references public.journals(org_id,id));
create table public.receipts(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),invoice_id uuid not null,amount numeric(16,2) not null check(amount>0),reference text not null,received_on date not null default current_date,journal_id uuid not null,unique(org_id,reference),foreign key(org_id,invoice_id) references public.invoices(org_id,id),foreign key(org_id,journal_id) references public.journals(org_id,id));
create table public.expenses(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),description text not null,amount numeric(16,2) not null check(amount>0),status text not null default 'pending' check(status in ('pending','approved','rejected')),requested_by uuid not null default private.uid(),approved_by uuid, journal_id uuid,created_at timestamptz not null default now(),foreign key(org_id,journal_id) references public.journals(org_id,id));
create table public.technicians(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),name text not null,specialty text not null,phone text not null default '',user_id uuid references private.users(id),unique(org_id,id));
create table public.assets(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),property_id uuid not null,name text not null,serial text not null default '',next_service date,unique(org_id,id),foreign key(org_id,property_id) references public.properties(org_id,id));
create table public.tickets(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),unit_id uuid,asset_id uuid,title text not null,description text not null default '',priority text not null default 'normal' check(priority in ('low','normal','urgent')),status text not null default 'open' check(status in ('open','assigned','in_progress','resolved','closed')),technician_id uuid,created_by uuid not null default private.uid(),created_at timestamptz not null default now(),foreign key(org_id,unit_id) references public.units(org_id,id),foreign key(org_id,asset_id) references public.assets(org_id,id),foreign key(org_id,technician_id) references public.technicians(org_id,id));
create table public.announcements(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),title text not null,body text not null,created_at timestamptz default now());
create table public.notifications(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),user_id uuid not null references private.users(id),message text not null,read_at timestamptz,created_at timestamptz default now());
create table public.audit_logs(id uuid primary key default gen_random_uuid(),org_id uuid not null references public.organizations(id),actor_id uuid,entity text not null,entity_id uuid,action text not null,created_at timestamptz not null default now());
create function private.audit() returns trigger language plpgsql security definer set search_path='' as $$
 begin insert into public.audit_logs(org_id,actor_id,entity,entity_id,action) values(new.org_id,private.uid(),tg_table_name,new.id,tg_op);return new;end $$;
revoke all on function private.audit() from public;
create function private.resident_party(o uuid,p uuid) returns boolean language sql stable security definer set search_path='' as $$select private.uid() is not null and exists(select 1 from public.parties where org_id=o and id=p and user_id=private.uid())$$;
create function private.resident_unit(o uuid,u uuid) returns boolean language sql stable security definer set search_path='' as $$select private.uid() is not null and exists(select 1 from public.leases l join public.parties p on p.id=l.party_id and p.org_id=l.org_id where l.org_id=o and l.unit_id=u and l.status='active' and p.user_id=private.uid())$$;
revoke all on function private.resident_party(uuid,uuid),private.resident_unit(uuid,uuid) from public;
grant execute on function private.resident_party(uuid,uuid),private.resident_unit(uuid,uuid) to towerflow_client;
alter table organizations enable row level security;
create policy org_read on organizations for select to towerflow_client using(exists(select 1 from private.memberships where org_id=id and user_id=private.uid()));
-- Memberships are only visible to the actor; writes occur via validated server endpoints.
grant select on private.memberships to towerflow_client;
alter table private.memberships enable row level security;
create policy own_membership on private.memberships for select to towerflow_client using(user_id=private.uid());
grant select on organizations to towerflow_client;
do $$declare t text; cap text;begin
 foreach t in array array['properties','units','parties','leases','ownerships','accounts','journals','journal_lines','invoices','receipts','expenses','technicians','assets','tickets','announcements','notifications','audit_logs'] loop
 execute format('alter table public.%I enable row level security',t);
 execute format('create index on public.%I(org_id)',t);
 cap:=case when t in ('accounts','journals','journal_lines','invoices','receipts','expenses') then 'finance' when t in ('technicians','assets','tickets') then 'maintain' else 'manage' end;
 execute format('grant select on public.%I to towerflow_client',t);
 execute format('create policy staff_read on public.%I for select to towerflow_client using(private.can(org_id,''read''))',t);
 if t not in ('audit_logs','receipts','journals','journal_lines','notifications') then
 execute format('grant insert,update on public.%I to towerflow_client',t);
 execute format('create policy staff_insert on public.%I for insert to towerflow_client with check(private.can(org_id,%L))',t,cap);
 execute format('create policy staff_update on public.%I for update to towerflow_client using(private.can(org_id,%L)) with check(private.can(org_id,%L))',t,cap,cap);
 end if;
 if t not in ('audit_logs','notifications') then execute format('create trigger audit_change after insert or update on public.%I for each row execute function private.audit()',t); end if;
 end loop;
end $$;
-- Sensitive financial tables are written only through transactional server services.
revoke insert,update on public.invoices,public.expenses from towerflow_client;
create policy resident_invoices on public.invoices for select to towerflow_client using(private.resident_party(org_id,party_id));
create policy resident_leases on public.leases for select to towerflow_client using(private.resident_party(org_id,party_id));
create policy resident_units on public.units for select to towerflow_client using(private.resident_unit(org_id,id));
create policy resident_parties on public.parties for select to towerflow_client using(user_id=private.uid());
create policy resident_tickets on public.tickets for select to towerflow_client using(created_by=private.uid() and private.resident_unit(org_id,unit_id));
create policy resident_ticket_create on public.tickets for insert to towerflow_client with check(created_by=private.uid() and private.resident_unit(org_id,unit_id) and status='open' and technician_id is null);
create policy resident_announcements on public.announcements for select to towerflow_client using(exists(select 1 from private.memberships m where m.org_id=announcements.org_id and m.user_id=private.uid()));
create policy own_notifications on public.notifications for select to towerflow_client using(user_id=private.uid());
-- Hard constraints protect accounting even if an API implementation regresses.
create function private.protect_lines() returns trigger language plpgsql set search_path='' as $$declare j uuid; s text;begin
 j:=case when tg_op='DELETE' then old.journal_id else new.journal_id end;
 select status into s from public.journals where id=j for update;
 if s='posted' then raise exception 'Posted journals are immutable'; end if;
 if tg_op='UPDATE' and (new.journal_id<>old.journal_id or new.org_id<>old.org_id) then raise exception 'Cannot move journal lines';end if;
 if tg_op='DELETE' then return old;end if;return new;end $$;
create trigger immutable_lines before insert or update or delete on public.journal_lines for each row execute function private.protect_lines();
create function private.protect_journal() returns trigger language plpgsql set search_path='' as $$declare d numeric;c numeric;n int;begin
 if old.status='posted' then raise exception 'Posted journals are immutable; use a reversing entry';end if;
 if tg_op='DELETE' then return old;end if;
 if new.status='posted' then select sum(debit),sum(credit),count(*) into d,c,n from public.journal_lines where journal_id=new.id;
 if n<2 or d is null or d<>c or d<=0 then raise exception 'Journal must balance with at least two lines';end if;end if;return new;end $$;
create trigger journal_balance before update or delete on public.journals for each row execute function private.protect_journal();
-- Never allow an already-posted header to bypass the transition trigger.
alter table public.journals add constraint starts_draft check(status in ('draft','posted'));
create function private.new_journal() returns trigger language plpgsql as $$begin if new.status<>'draft' then raise exception 'New journal must be draft';end if;return new;end$$;
create trigger new_journal_draft before insert on public.journals for each row execute function private.new_journal();
revoke all on function private.protect_lines(),private.protect_journal(),private.new_journal() from public;
create view public.trial_balance with(security_invoker=true) as select a.org_id,a.id,a.code,a.name,a.kind,coalesce(sum(l.debit) filter(where j.status='posted'),0) debit,coalesce(sum(l.credit) filter(where j.status='posted'),0) credit from public.accounts a left join public.journal_lines l on l.account_id=a.id and l.org_id=a.org_id left join public.journals j on j.id=l.journal_id group by a.org_id,a.id,a.code,a.name,a.kind;
grant select on public.trial_balance to towerflow_client;
commit;
