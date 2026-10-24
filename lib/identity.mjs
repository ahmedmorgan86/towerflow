import {randomBytes,createHash} from 'node:crypto';
import {OAuth2Client} from 'google-auth-library';
import sharp from 'sharp';
import {database} from './db.mjs';
import {hashToken,session,createSession,throttle} from './auth.mjs';
const bad=(s,status=400)=>Object.assign(new Error(s),{status});
const random=()=>randomBytes(32).toString('base64url');
export const challenge=s=>createHash('sha256').update(s).digest('base64url');
export const googleReady=()=>!!(process.env.GOOGLE_CLIENT_ID&&process.env.GOOGLE_CLIENT_SECRET);
const origin=()=>process.env.APP_ORIGIN||'http://localhost:3000';
const client=()=>new OAuth2Client(process.env.GOOGLE_CLIENT_ID,process.env.GOOGLE_CLIENT_SECRET,origin()+'/api/auth/google/callback');
export const publicUser=u=>({id:u.id,email:u.email,name:u.name,phone:u.phone||'',bio:u.bio||'',locale:u.locale||'ar',avatar:u.avatar||null});
export async function normalizeAvatar(value){
 if(value===null)return null;
 if(typeof value!=='string'||value.length>700000||!/^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/]+=*$/.test(value))throw bad('Choose a JPEG, PNG or WebP image under 500 KB');
 try{const bytes=Buffer.from(value.slice(value.indexOf(',')+1),'base64');if(bytes.length>512000)throw new Error();const output=await sharp(bytes,{limitInputPixels:20000000,animated:false}).rotate().resize(512,512,{fit:'cover',withoutEnlargement:true}).jpeg({quality:82}).toBuffer();return 'data:image/jpeg;base64,'+output.toString('base64');}catch{throw bad('Invalid image or image too large');}
}
export async function updateProfile(user,b){
 const name=String(b.name??user.name).trim(),phone=String(b.phone??user.phone??'').trim(),bio=String(b.bio??user.bio??'').trim(),locale=b.locale??user.locale??'ar';
 if(!name||name.length>160||phone.length>40||bio.length>1000||!['ar','en'].includes(locale))throw bad('Check name, phone, bio and language');
 const avatar=Object.hasOwn(b,'avatar')?await normalizeAvatar(b.avatar):user.avatar||null;
 return database().transaction(async c=>publicUser((await c.query('update private.users set name=$2,phone=$3,bio=$4,locale=$5,avatar=$6 where id=$1 returning *',[user.id,name,phone,bio,locale,avatar])).rows[0]));
}
export async function googleStart(req,u,res){
 if(!googleReady())throw bad('Google sign-in is awaiting administrator setup',503);
 const mobile=u.searchParams.get('client')==='android',appChallenge=u.searchParams.get('challenge');
 if(mobile&&!/^[A-Za-z0-9_-]{43}$/.test(appChallenge||''))throw bad('Invalid mobile challenge');
 const link=u.searchParams.get('link')==='true'?await session(req):null;
 if(u.searchParams.get('link')==='true'&&!link)throw bad('Sign in before linking Google',401);
 const state=random(),browser=random(),verifier=random(),nonce=random();
 await database().transaction(async c=>{await throttle(c,'oauth:'+String(req.headers['x-forwarded-for']||req.socket?.remoteAddress||'unknown'));await c.query('delete from private.oauth_states where expires_at<now()');await c.query('insert into private.oauth_states(state_hash,browser_hash,verifier,nonce,link_user,app_challenge) values($1,$2,$3,$4,$5,$6)',[hashToken(state),hashToken(browser),verifier,nonce,link?.id||null,mobile?appChallenge:null]);});
 res.setHeader('Set-Cookie',`tf_oauth=${browser}; HttpOnly; SameSite=Lax; Path=/api/auth/google; Max-Age=600${origin().startsWith('https:')?'; Secure':''}`);
 return client().generateAuthUrl({scope:['openid','email','profile'],state,nonce,code_challenge:challenge(verifier),code_challenge_method:'S256',prompt:'select_account'});
}
export async function completeGoogle(c,claims,linkUser){
 if(!claims?.sub||claims.email_verified!==true||!claims.email)throw bad('A verified Google email is required',401);
 const existing=(await c.query("select user_id from private.oauth_accounts where provider='google' and subject=$1",[claims.sub])).rows[0];
 if(existing){if(linkUser&&existing.user_id!==linkUser)throw bad('Google account is already linked to another member',409);return existing.user_id;}
 let id=linkUser;
 if(!id){const email=claims.email.trim().toLowerCase();if((await c.query('select id from private.users where email=$1',[email])).rows.length)throw bad('Sign in with your password, then link Google from your profile',409);
 const name=String(claims.name||email.split('@')[0]).slice(0,160);id=(await c.query('insert into private.users(email,name) values($1,$2) returning id',[email,name])).rows[0].id;
 const org=(await c.query('insert into organizations(name) values($1) returning id',[(name+' · TowerFlow').slice(0,160)])).rows[0].id;
 await c.query("insert into private.memberships values($1,$2,'admin')",[org,id]);await c.query("select set_config('app.user_id',$1,true)",[id]);
 for(const a of [['1100','Cash','asset'],['1200','Receivables','asset'],['4000','Rental income','income'],['5000','Operating expenses','expense']])await c.query('insert into accounts(org_id,code,name,kind) values($1,$2,$3,$4)',[org,...a]);
 }
 await c.query("insert into private.oauth_accounts values('google',$1,$2)",[claims.sub,id]);return id;
}
export async function googleCallback(req,u,res,setSession){
 const browser=(req.headers.cookie||'').split(';').map(x=>x.trim()).find(x=>x.startsWith('tf_oauth='))?.slice(9);
 const state=u.searchParams.get('state');if(!browser||!state)throw bad('Google sign-in expired; please retry',401);
 const pending=await database().transaction(async c=>(await c.query('delete from private.oauth_states where state_hash=$1 and browser_hash=$2 and expires_at>now() returning *',[hashToken(state),hashToken(browser)])).rows[0]);
 if(!pending)throw bad('Google sign-in expired; please retry',401);
 if(u.searchParams.get('error'))throw bad('Google sign-in was cancelled');
 const code=u.searchParams.get('code');if(!code)throw bad('Google authorization code missing');
 const oauth=client();const {tokens}=await oauth.getToken({code,codeVerifier:pending.verifier});
 const claims=(await oauth.verifyIdToken({idToken:tokens.id_token,audience:process.env.GOOGLE_CLIENT_ID})).getPayload();
 if(claims.nonce!==pending.nonce)throw bad('Google sign-in verification failed',401);
 return database().transaction(async c=>{const id=await completeGoogle(c,claims,pending.link_user);if(pending.app_challenge){const ticket=random();await c.query('insert into private.mobile_codes values($1,$2,$3,now()+interval \'1 minute\')',[hashToken(ticket),id,pending.app_challenge]);return 'towerflow://auth?code='+ticket;}
 const token=await createSession(c,id);res.setHeader('Set-Cookie',[setSession(token),`tf_oauth=; HttpOnly; SameSite=Lax; Path=/api/auth/google; Max-Age=0${origin().startsWith('https:')?'; Secure':''}`]);return origin()+'/?google=success';});
}
export async function mobileExchange(b){
 if(!/^[A-Za-z0-9_-]{43}$/.test(b.code||'')||!/^[A-Za-z0-9_-]{43,128}$/.test(b.verifier||''))throw bad('Invalid sign-in code');
 return database().transaction(async c=>{const row=(await c.query('delete from private.mobile_codes where code_hash=$1 and challenge=$2 and expires_at>now() returning user_id',[hashToken(b.code),challenge(b.verifier)])).rows[0];if(!row)throw bad('Sign-in code expired or already used',401);return {token:await createSession(c,row.user_id)};});
}
