export const catalog={
 properties:{fields:['name','address','kind','currency','country','floors_count','units_per_floor','status'],required:['name']},
 units:{fields:['property_id','unit_number','name','floor','area','status','ownership_type','payer_responsibility','vacant_policy','vacant_rate','notes'],required:['property_id','name']},
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
 expenses:{fields:['description','amount'],required:['description','amount']},
 money_accounts:{fields:['property_id','account_id','name','kind','bank_name','account_number'],required:['property_id','name','kind']},
 payment_proofs:{fields:['property_id','unit_id','party_id','invoice_id','amount','payment_method','target_account_id','reference','image_url'],required:['property_id','unit_id','party_id','amount','reference']},
 fines:{fields:['property_id','unit_id','party_id','invoice_id','title','amount','destination'],required:['property_id','unit_id','party_id','title','amount']},
 credit_balances:{fields:['property_id','party_id','unit_id','balance'],required:['property_id','party_id']},
 vendor_contracts:{fields:['property_id','vendor_id','title','service_type','amount','frequency','start_date','end_date','auto_renew','terms'],required:['property_id','vendor_id','title','service_type','amount','start_date','end_date']},
 budgets:{fields:['property_id','name','period','start_date','end_date','overrun_policy','rollover_policy'],required:['property_id','name','start_date','end_date']},
 delegations:{fields:['property_id','user_id','role_title','approval_limit','active','expires_at'],required:['property_id','user_id']}
};

export const readable=[
 ...Object.keys(catalog),
 'journals','journal_lines','receipts','audit_logs','notifications','trial_balance',
 'accounting_periods','invoice_lines','ticket_comments','expense_distributions','distribution_shares',
 'credit_transactions','contract_versions','budget_items','account_reconciliations',
 'management_transfers','support_tickets','support_messages','support_grants','subscription_plans','property_subscriptions'
];

export function clean(entity,data,partial=false){
 const spec=catalog[entity];if(!spec) throw Object.assign(new Error('Unknown entity'),{status:404});
 if(!data||typeof data!=='object'||Array.isArray(data))throw Object.assign(new Error('Invalid data'),{status:400});
 if(entity==='units'){
  if(data.unit_number && !data.name) data.name=String(data.unit_number).trim();
  if(data.name && !data.unit_number) data.unit_number=String(data.name).trim();
 }
 const out={};
 for(const k of spec.fields)if(data[k]!==undefined&&data[k]!==null&&data[k]!==''){
 const v=data[k];if(!['string','number','boolean'].includes(typeof v)||String(v).length>4000)throw Object.assign(new Error('Invalid field: '+k),{status:400});out[k]=typeof v==='string'?v.trim():v;
 }
 if(!partial)for(const k of spec.required)if(out[k]===undefined||out[k]==='')throw Object.assign(new Error('Required: '+k),{status:400});
 return out;
}

export function cents(value){const s=String(value);if(!/^\d{1,12}(\.\d{1,2})?$/.test(s))throw Object.assign(new Error('Invalid amount'),{status:400});const [a,b='']=s.split('.');const n=Number(a)*100+Number(b.padEnd(2,'0'));if(!Number.isSafeInteger(n)||n<=0)throw Object.assign(new Error('Invalid amount'),{status:400});return n;}
