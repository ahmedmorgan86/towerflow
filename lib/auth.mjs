import {randomBytes,scrypt as scryptCallback,timingSafeEqual,createHash} from 'node:crypto';
import {promisify} from 'node:util';
import {database} from './db.mjs';
const scrypt=promisify(scryptCallback);
export const hashToken=t=>createHash('sha256').update(t).digest('hex');
export async function passwordHash(password){const salt=randomBytes(16).toString('hex');return salt+':'+(await scrypt(password,salt,64)).toString('hex');}
export async function passwordMatches(password,stored){if(!stored)return false;const [salt,hex]=stored.split(':');const actual=await scrypt(password,salt,64);const expected=Buffer.from(hex,'hex');return actual.length===expected.length&&timingSafeEqual(actual,expected);}
export function tokenFrom(req){return /^Bearer [a-f0-9]{64}$/.test(req.headers.authorization||'')?req.headers.authorization.slice(7):(req.headers.cookie||'').split(';').map(x=>x.trim()).find(x=>x.startsWith('tf_session='))?.slice(11);}
export async function session(req){const token=tokenFrom(req);if(!token)return null;return database().transaction(async c=>(await c.query('select u.id,u.email,u.name,u.phone,u.bio,u.locale,u.avatar from private.sessions s join private.users u on u.id=s.user_id where s.token_hash=$1 and s.expires_at>now()',[hashToken(token)])).rows[0]||null);}
export async function createSession(c,userId){const token=randomBytes(32).toString('hex');await c.query("insert into private.sessions(token_hash,user_id,expires_at) values($1,$2,now()+interval '12 hours')",[hashToken(token),userId]);return token;}
export async function throttle(c,key){const r=await c.query("insert into private.login_limits(key,attempts) values($1,1) on conflict(key) do update set attempts=case when private.login_limits.window_start<now()-interval '15 minutes' then 1 else private.login_limits.attempts+1 end,window_start=case when private.login_limits.window_start<now()-interval '15 minutes' then now() else private.login_limits.window_start end returning attempts",[hashToken(key)]);if(r.rows[0].attempts>20)throw Object.assign(new Error('Too many attempts; retry in 15 minutes'),{status:429});}
