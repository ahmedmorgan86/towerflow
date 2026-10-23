begin;
drop policy staff_read on public.notifications;
create function private.sync_occupancy() returns trigger language plpgsql security definer set search_path='' as $$begin
 if tg_op='UPDATE' and new.unit_id<>old.unit_id then update public.units set status='vacant' where id=old.unit_id and org_id=old.org_id and status='occupied';end if;
 update public.units set status=case when new.status='active' then 'occupied' else 'vacant' end where id=new.unit_id and org_id=new.org_id;
 return new;end$$;
revoke all on function private.sync_occupancy() from public;
create trigger lease_occupancy after insert or update on public.leases for each row execute function private.sync_occupancy();
create function private.check_ownership() returns trigger language plpgsql security definer set search_path='' as $$declare total numeric;begin
 perform 1 from public.units where org_id=new.org_id and id=new.unit_id for update;
 select coalesce(sum(share),0) into total from public.ownerships where org_id=new.org_id and unit_id=new.unit_id and id<>new.id;
 if total+new.share>100 then raise exception 'Ownership shares exceed 100 percent';end if;return new;end$$;
revoke all on function private.check_ownership() from public;
create trigger ownership_total before insert or update on public.ownerships for each row execute function private.check_ownership();
create function private.workflow_notify() returns trigger language plpgsql security definer set search_path='' as $$begin
 insert into public.notifications(org_id,user_id,message)
 select new.org_id,m.user_id,tg_table_name||': '||new.id::text||' · '||new.status
 from private.memberships m where m.org_id=new.org_id and m.role in ('admin','manager') and m.user_id is distinct from private.uid();
 return new;end$$;
revoke all on function private.workflow_notify() from public;
create trigger ticket_notification after insert or update on public.tickets for each row execute function private.workflow_notify();
create trigger expense_notification after insert or update on public.expenses for each row execute function private.workflow_notify();
commit;
