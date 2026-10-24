export async function ensurePlatformOwner(c,userId,claims){
 const expected=process.env.PLATFORM_OWNER_EMAIL?.trim().toLowerCase();
 if(!expected||claims?.email_verified!==true||claims.email?.trim().toLowerCase()!==expected||!claims.sub)return false;
 await c.query('insert into private.platform_owners(user_id,email,google_subject) values($1,$2,$3) on conflict(user_id) do update set email=excluded.email,google_subject=excluded.google_subject',[userId,expected,claims.sub]);
 await c.query("insert into private.memberships(org_id,user_id,role) select id,$1,'admin' from organizations on conflict(org_id,user_id) do update set role='admin'",[userId]);return true;
}
