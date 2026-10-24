begin;
alter table private.users alter column password_hash drop not null;
alter table private.users add column phone text not null default '', add column bio text not null default '', add column locale text not null default 'ar', add column avatar text;
create table private.oauth_accounts(provider text not null,subject text not null,user_id uuid not null references private.users(id),primary key(provider,subject),unique(provider,user_id));
create table private.oauth_states(state_hash text primary key,browser_hash text not null,verifier text not null,nonce text not null,link_user uuid references private.users(id),app_challenge text,expires_at timestamptz not null default now()+interval '10 minutes');
create table private.mobile_codes(code_hash text primary key,user_id uuid not null references private.users(id),challenge text not null,expires_at timestamptz not null default now()+interval '1 minute');
commit;
