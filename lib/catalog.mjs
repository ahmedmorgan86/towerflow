export const catalog={
 properties:{fields:['name','address','kind'],required:['name']},
 units:{fields:['property_id','name','floor','area','status'],required:['property_id','name']},
 parties:{fields:['name','kind','email','phone'],required:['name','kind']},
 leases:{fields:['unit_id','party_id','start_date','end_date','rent','status'],required:['unit_id','party_id','start_date','end_date','rent']},
 ownerships:{fields:['unit_id','party_id','share'],required:['unit_id','party_id','share']},
 accounts:{fields:['code','name','kind'],required:['code','name','kind']},
 technicians:{fields:['name','specialty','phone'],required:['name','specialty']},
 assets:{fields:['property_id','name','serial','next_service'],required:['property_id','name']},
 tickets:{fields:['unit_id','asset_id','title','description','priority','status','technician_id','due_at'],required:['title']},
 recurring_charges:{fields:['party_id','unit_id','name','amount','frequency','next_due','end_date'],required:['party_id','name','amount','frequency','next_due']},
 maintenance_plans:{fields:['asset_id','name','interval_days','next_due'],required:['asset_id','name','interval_days','next_due']},
 announcements:{fields:['title','body'],required:['title','body']},
 invoices:{fields:['party_id','unit_id','number','description','amount','due_date'],required:['party_id','number','description','amount','due_date']},
 expenses:{fields:['description','amount'],required:['description','amount']}
};
export const readable=[...Object.keys(catalog),'journals','journal_lines','receipts','audit_logs','notifications','trial_balance','accounting_periods','invoice_lines','ticket_comments'];
export function clean(entity,data,partial=false){
 const spec=catalog[entity];if(!spec) throw Object.assign(new Error('Unknown entity'),{status:404});
 if(!data||typeof data!=='object'||Array.isArray(data))throw Object.assign(new Error('Invalid data'),{status:400});
 const out={};
 for(const k of spec.fields)if(data[k]!==undefined&&data[k]!==null&&data[k]!==''){
 const v=data[k];if(!['string','number'].includes(typeof v)||String(v).length>4000)throw Object.assign(new Error('Invalid field: '+k),{status:400});out[k]=typeof v==='string'?v.trim():v;
 }
 if(!partial)for(const k of spec.required)if(out[k]===undefined||out[k]==='')throw Object.assign(new Error('Required: '+k),{status:400});
 return out;
}
export function cents(value){const s=String(value);if(!/^\d{1,12}(\.\d{1,2})?$/.test(s))throw Object.assign(new Error('Invalid amount'),{status:400});const [a,b='']=s.split('.');const n=Number(a)*100+Number(b.padEnd(2,'0'));if(!Number.isSafeInteger(n)||n<=0)throw Object.assign(new Error('Invalid amount'),{status:400});return n;}
