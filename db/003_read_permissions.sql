begin;
create function private.can_read(o uuid, entity text) returns boolean language sql stable security definer set search_path='' as $$
 select private.can(o,'read') and (
 entity not in ('parties','leases','ownerships','accounts','journals','journal_lines','invoices','receipts','expenses','audit_logs')
 or exists(select 1 from private.memberships m where m.org_id=o and m.user_id=private.uid() and m.role in ('admin','manager','accountant','viewer'))
 );
$$;
revoke all on function private.can_read(uuid,text) from public;
grant execute on function private.can_read(uuid,text) to towerflow_client;
do $$declare t text;begin
 foreach t in array array['properties','units','parties','leases','ownerships','accounts','journals','journal_lines','invoices','receipts','expenses','technicians','assets','tickets','announcements','audit_logs'] loop
 execute format('drop policy staff_read on public.%I',t);
 execute format('create policy staff_read on public.%I for select to towerflow_client using(private.can_read(org_id,%L))',t,t);
 end loop;
end$$;
commit;
