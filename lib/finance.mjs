import {cents} from './catalog.mjs';
export async function permission(c,org,cap){const r=await c.query('select private.can($1,$2) ok',[org,cap]);if(!r.rows[0].ok)throw Object.assign(new Error('Permission denied'),{status:403});}
export async function journal(c,org,user,description,lines,entryDate=null,reversesId=null){
 if(!Array.isArray(lines)||lines.length<2||lines.length>100)throw Object.assign(new Error('2–100 lines required'),{status:400});
 let d=0,cr=0;for(const l of lines){if(l.debit&&l.credit)throw Object.assign(new Error('One side per line'),{status:400});if(l.debit)d+=cents(l.debit);else if(l.credit)cr+=cents(l.credit);else throw Object.assign(new Error('Amount required'),{status:400});}
 if(d!==cr||d<=0)throw Object.assign(new Error('Journal does not balance'),{status:400});
 const j=(await c.query('insert into journals(org_id,description,created_by,entry_date,reverses_id) values($1,$2,$3,coalesce($4::date,current_date),$5) returning *',[org,description,user,entryDate,reversesId])).rows[0];
 for(const l of lines)await c.query('insert into journal_lines(org_id,journal_id,account_id,debit,credit) values($1,$2,$3,$4,$5)',[org,j.id,l.account_id,l.debit||0,l.credit||0]);
 await c.query("update journals set status='posted' where id=$1",[j.id]);return j.id;
}
async function accounts(c,org){return Object.fromEntries((await c.query("select code,id from accounts where org_id=$1 and code in ('1100','1200','4000','5000')",[org])).rows.map(x=>[x.code,x.id]));}
export async function issue(c,org,user,id){
 await permission(c,org,'finance');const i=(await c.query('select * from invoices where id=$1 and org_id=$2 for update',[id,org])).rows[0];
 if(!i||i.status!=='draft')throw Object.assign(new Error('Invoice must be draft'),{status:409});
 const a=await accounts(c,org);const lines=[{account_id:a['1200'],debit:i.amount},{account_id:a['4000'],credit:Number(i.tax_amount)>0?i.subtotal:i.amount}];
 if(Number(i.tax_amount)>0){const tax=(await c.query("insert into accounts(org_id,code,name,kind) values($1,'2100','Tax payable','liability') on conflict(org_id,code) do update set code=excluded.code returning id,kind",[org])).rows[0];if(tax.kind!=='liability')throw Object.assign(new Error('Tax account must be a liability'),{status:409});lines.push({account_id:tax.id,credit:i.tax_amount});}
 const j=await journal(c,org,user,i.description,lines);
 await c.query("update invoices set status='issued',journal_id=$1 where id=$2",[j,id]);return {journal_id:j};
}
export async function collect(c,org,user,id,amount,reference){
 await permission(c,org,'finance');const i=(await c.query('select * from invoices where id=$1 and org_id=$2 for update',[id,org])).rows[0];
 if(!i||i.status!=='issued')throw Object.assign(new Error('Invoice must be issued'),{status:409});
 if(!reference||reference.length>160)throw Object.assign(new Error('Receipt reference required'),{status:400});
 const paid=(await c.query('select coalesce(sum(amount),0) total from receipts where invoice_id=$1',[id])).rows[0].total;
 const remaining=cents(i.amount)-(Number(paid)===0?0:cents(paid));const n=cents(amount);if(n>remaining)throw Object.assign(new Error('Receipt exceeds remaining balance'),{status:400});
 const a=await accounts(c,org);const j=await journal(c,org,user,'Receipt '+reference,[{account_id:a['1100'],debit:amount},{account_id:a['1200'],credit:amount}]);
 const r=(await c.query('insert into receipts(org_id,invoice_id,amount,reference,journal_id) values($1,$2,$3,$4,$5) returning *',[org,id,amount,reference,j])).rows[0];
 if(n===remaining)await c.query("update invoices set status='paid' where id=$1",[id]);return r;
}
export async function approve(c,org,user,id,decision){
 await permission(c,org,'manage');const e=(await c.query('select * from expenses where id=$1 and org_id=$2 for update',[id,org])).rows[0];
 if(!e||e.status!=='pending')throw Object.assign(new Error('Expense must be pending'),{status:409});
 if(e.requested_by===user)throw Object.assign(new Error('Another manager must approve this expense'),{status:403});
 let j=null;if(decision==='approved'){const a=await accounts(c,org);const payable=(await c.query("insert into accounts(org_id,code,name,kind) values($1,'2200','Approved expenses payable','liability') on conflict(org_id,code) do update set code=excluded.code returning id,kind",[org])).rows[0];if(payable.kind!=='liability')throw Object.assign(new Error('Expense payable account must be a liability'),{status:409});j=await journal(c,org,user,e.description,[{account_id:a['5000'],debit:e.amount},{account_id:payable.id,credit:e.amount}]);}
 else if(decision!=='rejected')throw Object.assign(new Error('Invalid decision'),{status:400});
 await c.query("update expenses set status=$1,approved_by=$2,journal_id=$3,approved_at=case when $1='approved' then now() else null end where id=$4",[decision,user,j,id]);return {status:decision};
}
