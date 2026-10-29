import {cents} from './catalog.mjs';import {permission,journal} from './finance.mjs';import {isoDate} from './platform.mjs';
const fail=(s,status=400)=>Object.assign(new Error(s),{status});
async function scopePermission(c,org,entity,verb='read'){const sql=verb==='read'?'select private.can_read($1,$2) ok':'select private.can_write($1,$2,$3) ok';if(!(await c.query(sql,verb==='read'?[org,entity]:[org,entity,verb])).rows[0].ok)throw fail('Permission denied',403);}
const date=v=>v instanceof Date?v.toISOString().slice(0,10):String(v).slice(0,10);
function request(b){const amount=cents(b.amount),reference=String(b.reference||'').trim();if(!reference||reference.length>160)throw fail('Unique payment reference required');return {amount,reference,on:isoDate(b.paid_on||b.transferred_on||new Date().toISOString().slice(0,10))};}
async function moneyAccount(c,org,id){const a=(await c.query("select id from accounts where org_id=$1 and id=$2 and kind='asset' and (treasury_kind is not null or code='1100')",[org,id])).rows[0];if(!a)throw fail('Treasury account must belong to this building',400);return a;}
export async function balance(c,org,id){return (await c.query("select coalesce(sum(l.debit-l.credit),0)::text balance from journal_lines l join journals j on j.id=l.journal_id and j.org_id=l.org_id where l.org_id=$1 and l.account_id=$2 and j.status='posted'",[org,id])).rows[0].balance;}
async function sufficient(c,org,account,n){const current=await balance(c,org,account);const normalized=String(current);if(normalized.startsWith('-')||(Number(normalized)===0?0:cents(normalized))<n)throw fail('Insufficient available account balance',409);}
export async function treasury(c,org){await permission(c,org,'finance');await scopePermission(c,org,'accounts');return (await c.query("select a.id,a.code,a.name,coalesce(a.treasury_kind,'cash') treasury_kind,coalesce(sum(l.debit-l.credit) filter(where j.status='posted'),0)::text balance from accounts a left join journal_lines l on l.account_id=a.id and l.org_id=a.org_id left join journals j on j.id=l.journal_id and j.org_id=l.org_id where a.org_id=$1 and a.kind='asset' and (a.treasury_kind is not null or a.code='1100') group by a.id order by a.code",[org])).rows;}
export async function createTreasury(c,org,b){
 await permission(c,org,'finance');await scopePermission(c,org,'accounts','create');
 if(!['cash','bank','wallet'].includes(b.treasury_kind)||!String(b.name||'').trim())throw fail('Treasury name and type required');
 await c.query('select id from organizations where id=$1 for update',[org]);let code=String(b.code||'').trim();
 if(!code){const used=new Set((await c.query("select code from accounts where org_id=$1 and code like '11%'",[org])).rows.map(x=>x.code));for(let i=1;i<=99999;i++){const candidate='11'+String(i).padStart(2,'0');if(!used.has(candidate)){code=candidate;break;}}}
 if(!/^11\d{2,5}$/.test(code))throw fail('Valid treasury account code required');
 return (await c.query("insert into accounts(org_id,code,name,kind,treasury_kind) values($1,$2,$3,'asset',$4) returning *",[org,code,String(b.name).trim().slice(0,160),b.treasury_kind])).rows[0];
}
export async function financialSummary(c,org){await permission(c,org,'finance');await scopePermission(c,org,'expenses');const balances=await treasury(c,org);const summary=(await c.query("select (select coalesce(sum(l.debit-l.credit),0) from journal_lines l join journals j on j.id=l.journal_id and j.org_id=l.org_id join accounts a on a.id=l.account_id and a.org_id=l.org_id where l.org_id=$1 and j.status='posted' and a.kind='asset' and (a.treasury_kind is not null or a.code='1100')) available_cash, (select coalesce(sum(amount),0) from expenses where org_id=$1 and status='approved') approved_expenses,(select coalesce(sum(paid_amount),0) from expenses where org_id=$1 and status='approved') paid_expenses,(select coalesce(sum(amount-paid_amount),0) from expenses where org_id=$1 and status='approved') unpaid_expenses,(select count(*)::int from expenses where org_id=$1 and status='pending') pending_approvals",[org])).rows[0];return {...summary,accounts:balances};}
export async function payExpense(c,org,user,id,b){
 await permission(c,org,'finance');await scopePermission(c,org,'expenses','update');const r=request(b);await c.query('select id from organizations where id=$1 for update',[org]);
 const previous=(await c.query('select * from expense_payments where org_id=$1 and reference=$2',[org,r.reference])).rows[0];
 if(previous){if(previous.expense_id!==id||previous.account_id!==b.account_id||cents(previous.amount)!==r.amount||date(previous.paid_on)!==r.on)throw fail('Payment reference already used with different details',409);return {...previous,repeated:true};}
 const e=(await c.query('select * from expenses where org_id=$1 and id=$2 for update',[org,id])).rows[0];if(!e||e.status!=='approved')throw fail('Approved expense required',409);
 if(r.amount>cents(e.amount)-(Number(e.paid_amount)===0?0:cents(e.paid_amount)))throw fail('Payment exceeds unpaid expense',409);
 await moneyAccount(c,org,b.account_id);await sufficient(c,org,b.account_id,r.amount);
 const payable=(await c.query("select id,kind from accounts where org_id=$1 and code='2200'",[org])).rows[0];if(payable?.kind!=='liability')throw fail('Expense payable account unavailable',409);
 const j=await journal(c,org,user,'Expense payment '+r.reference,[{account_id:payable.id,debit:b.amount},{account_id:b.account_id,credit:b.amount}],r.on);
 const result=(await c.query('insert into expense_payments(org_id,expense_id,account_id,amount,reference,paid_on,paid_by,journal_id) values($1,$2,$3,$4,$5,$6,$7,$8) returning *',[org,id,b.account_id,b.amount,r.reference,r.on,user,j])).rows[0];
 await c.query('update expenses set paid_amount=paid_amount+$3 where org_id=$1 and id=$2',[org,id,b.amount]);return result;
}
export async function transfer(c,org,user,b){
 await permission(c,org,'finance');await scopePermission(c,org,'journals','create');const r=request(b);if(b.source_id===b.target_id)throw fail('Choose two different accounts');await c.query('select id from organizations where id=$1 for update',[org]);
 const previous=(await c.query('select * from treasury_transfers where org_id=$1 and reference=$2',[org,r.reference])).rows[0];if(previous){if(previous.source_id!==b.source_id||previous.target_id!==b.target_id||cents(previous.amount)!==r.amount||date(previous.transferred_on)!==r.on)throw fail('Transfer reference already used with different details',409);return {...previous,repeated:true};}
 await moneyAccount(c,org,b.source_id);await moneyAccount(c,org,b.target_id);await sufficient(c,org,b.source_id,r.amount);
 const j=await journal(c,org,user,'Treasury transfer '+r.reference,[{account_id:b.target_id,debit:b.amount},{account_id:b.source_id,credit:b.amount}],r.on);
 return (await c.query('insert into treasury_transfers(org_id,source_id,target_id,amount,reference,transferred_on,created_by,journal_id) values($1,$2,$3,$4,$5,$6,$7,$8) returning *',[org,b.source_id,b.target_id,b.amount,r.reference,r.on,user,j])).rows[0];
}
