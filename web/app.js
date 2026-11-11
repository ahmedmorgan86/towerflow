import {mountMessages} from './resident-messages.js';
import {mountReports} from './reports.js';
import {mountWorkflows} from './workflows.js';
import {mountProfile} from './profile.js';
import {icon,architecture} from './design.js';

const $=s=>document.querySelector(s), escape=s=>String(s??'').replace(/[&<>"']/g,x=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[x]));
let lang=localStorage.tfLang||'ar',dark=localStorage.tfDark==='true',me=null,org='',page='dashboard',cache={},busy=false;

const words={
 dashboard:['خزينة وصندوق العمارة','Building Sandok & Dashboard'],properties:['الأبراج والعمارات','Towers & Buildings'],instapay_id:['عنوان إنستاباي InstaPay IPA','InstaPay IPA'],wallet_number:['رقم المحفظة الإلكترونية (فودافون كاش / وي)','E-Wallet Mobile'],occupancy_state:['حالة إشغال الوحدة','Occupancy State'],units:['الشقق والوحدات','Apartments & Units'],
 residing:['مشغولة ومقيمة 🟢','Residing (Occupied)'],closed:['مغلقة 🟡','Closed'],finishing:['تحت التشطيب 🔵','Under Finishing'],resident_phone:['هاتف الساكن (واتساب)','Resident WhatsApp Phone'],
 parties:['الملاك والشاغلون','Residents & Owners'],leases:['عقود الوحدات','Units Records'],ownerships:['ملكيات الوحدات','Ownerships'],
 invoices:['المطالبات والمستحقات','Dues & Invoices'],receipts:['سندات القبض','Receipts'],expenses:['مصروفات الصندوق','Sandok Expenses'],
 journals:['دفتر اليومية','Journal'],accounts:['دليل الحسابات','Accounts'],tickets:['الصيانة والخدمات','Maintenance'],
 assets:['الأصول','Assets'],technicians:['الفنيون','Technicians'],reports:['التقارير المالية','Financial Reports'],
 announcements:['إعلانات وإشعارات السكان','Announcements'],notifications:['الإشعارات','Notifications'],
 audit_logs:['سجل التدقيق','Audit trail'],members:['المستخدمون والصلاحيات','Members & roles'],settings:['الإعدادات','Settings'],
 portal:['بوابة السكان','Resident portal'],money_accounts:['حسابات الأموال والخزائن','Money Accounts & Cash Boxes'],
 payment_proofs:['إثباتات التحويل','Payment Proofs'],fines:['الغرامات والتأخير','Fines & Late Fees'],
 credit_balances:['الأرصدة الدائنة','Credit Balances'],expense_distributions:['توزيع المصروفات','Expense Distributions'],
 vendor_contracts:['عقود الخدمات والموردين','Vendor Contracts'],budgets:['الميزانيات التقديرية','Budgets'],
 support_tickets:['الدعم الفني','Support Tickets'],quick_collect:['سجّل تحصيل','Quick Collect'],
 quick_expense:['إضافة مصروف','Quick Expense'],quick_proof:['إرسال إثبات دفع','Submit Payment Proof'],
 transfer_funds:['تحويل بين حسابات البرج','Transfer Within Tower'],
 unit_number:['رقم الشقة','Apartment #'],name:['الاسم / المسمى','Name'],address:['العنوان','Address'],kind:['النوع','Type'],property_id:['العقار / البرج','Property / Tower'],
 unit_id:['الشقة / الوحدة','Unit'],party_id:['الساكن / المالك','Resident / Owner'],floor:['الطابق','Floor'],area:['المساحة م²','Area m²'],
 status:['الحالة','Status'],email:['البريد الإلكتروني','Email'],phone:['الهاتف','Phone'],start_date:['بداية العقد','Start date'],
 end_date:['نهاية العقد','End date'],rent:['المبلغ الشهري','Monthly Fee'],share:['نسبة الملكية %','Ownership %'],code:['الكود','Code'],
 specialty:['التخصص','Specialty'],serial:['الرقم التسلسلي','Serial'],next_service:['الصيانة القادمة','Next service'],
 title:['العنوان','Title'],description:['الوصف','Description'],priority:['الأولوية','Priority'],
 technician_id:['الفني المسؤول','Technician'],asset_id:['الأصل','Asset'],body:['المحتوى','Body'],
 number:['رقم المطالبة','Invoice number'],amount:['المبلغ','Amount'],due_date:['تاريخ الاستحقاق','Due date'],
 reference:['رقم المرجع / الإيصال','Reference No'],received_on:['تاريخ التحصيل','Received on'],created_at:['التاريخ','Date'],
 entry_date:['تاريخ القيد','Entry date'],message:['الرسالة','Message'],entity:['الجدول','Entity'],
 entity_id:['السجل','Record'],action:['الإجراء','Action'],role:['الصلاحية','Role'],debit:['مدين','Debit'],
 credit:['دائن','Credit'],balance:['الرصيد','Balance'],destination:['جهة الحصيلة','Destination'],
 vendor_id:['المورد','Vendor'],service_type:['نوع الخدمة','Service Type'],frequency:['الدورية','Frequency'],
 period:['الفترة','Period'],payment_method:['طريقة الدفع','Payment Method'],ticket_number:['رقم التذكرة','Ticket #'],
 category:['التصنيف','Category'],save:['حفظ','Save'],cancel:['إلغاء','Cancel'],add:['إضافة','Add'],
 edit:['تعديل','Edit'],export:['تصدير CSV','Export CSV'],empty:['لا توجد سجلات بعد. ابدأ بإضافة أول سجل.','No records yet. Add your first record.'],
 loading:['جارٍ التحميل…','Loading…'],saved:['تم الحفظ','Saved'],search:['ابحث في السجلات…','Search records…'],
 logout:['تسجيل الخروج','Sign out'],signin:['تسجيل الدخول','Sign in'],signup:['إنشاء حساب ومؤسسة','Create account & workspace'],
 password:['كلمة المرور (12 حرفًا على الأقل)','Password (at least 12 characters)'],organization:['اسم البرج أو اتحاد الشاغلين','Tower / HOA Name'],
 welcome:['صندوق وخزينة العمارة، بوضوح.','Building Sandok & Management.'],
 subtitle:['إدارة البرج والشقق والتحصيلات في مساحة واحدة مباشرة وسهلة.','Manage your tower, units and collections in one simple place.'],
 draft:['مسودة','Draft'],issued:['صادرة','Issued'],paid:['مدفوعة','Paid'],pending:['قيد المراجعة','Pending'],
 approved:['معتمدة','Approved'],rejected:['مرفوضة','Rejected'],exempt:['معفاة','Exempt'],
 vacant:['مغلقة 🟡','Closed'],occupied:['مشغولة ومقيمة 🟢','Residing'],owned:['تمليك','Owned'],maintenance:['تحت التشطيب 🔵','Under Finishing'],
 active:['نشط','Active'],ended:['منتهٍ','Ended'],open:['مفتوح','Open'],assigned:['مسند','Assigned'],
 in_progress:['جارٍ العمل','In progress'],resolved:['تم الحل','Resolved'],closed:['مغلق','Closed'],
 normal:['عادية','Normal'],urgent:['عاجلة','Urgent'],low:['منخفضة','Low'],high:['مرتفعة','High'],critical:['حرجة','Critical'],
 owner:['مالك','Owner'],tenant:['ساكن','Resident'],vendor:['مورد','Vendor'],asset:['أصل','Asset'],
 liability:['التزام','Liability'],equity:['حقوق ملكية','Equity'],income:['إيراد','Income'],expense:['مصروف','Expense'],
 admin:['مدير برج / مسؤول','Administrator'],manager:['مدير برج','Manager'],accountant:['أمين صندوق','Accountant'],
 technician:['فني صيانة','Technician'],resident:['ساكن / مالك','Resident'],viewer:['مشاهد','Viewer'],posted:['مرحّل','Posted'],
 issue:['إصدار','Issue'],collect:['تسجيل تحصيل','Collect'],approve:['اعتماد','Approve'],
 reject:['رفض','Reject'],exempt_btn:['إعفاء','Exempt'],apply_credit:['سداد من الرصيد الدائن','Settle with credit'],
 cash:['خزنة نقدية','Cash Box'],bank:['حساب بنكي','Bank Account'],wallet:['محفظة إلكترونية','E-Wallet'],
 maintenance_fund:['صندوق الصيانة','Maintenance Fund'],fines_fund:['صندوق الغرامات','Fines Fund']
};

Object.assign(words,{
 collections:['التحصيل الشهري للشقق','Monthly Unit Collections'],resident_messages:['رسائل سكان الأبراج','Resident messages'],platform_owner:['مالك التطبيق','App owner'],
 recurring_charges:['المستحقات الدورية','Recurring charges'],maintenance_plans:['الصيانة الوقائية','Preventive maintenance'],
 accounting_periods:['الفترات المحاسبية','Accounting periods'],profile:['الملف الشخصي','My profile'],
 debit_account:['الحساب المدين','Debit account'],credit_account:['الحساب الدائن','Credit account'],tower:['برج','Tower'],ownership_type:['نوع الحيازة / الملكية','Ownership type'],rental:['إيجار','Rental'],owned_units:['وحدات تمليك','Owned units'],rented_units:['وحدات مؤجرة','Rented units'],payer_responsibility:['المسؤول عن الدفع','Payer responsibility'],
 bank_transfer:['تحويل بنكي','Bank transfer'],card:['بطاقة دفع إلكتروني','Card'],other:['طريقة أخرى','Other'],
 monthly:['شهري','Monthly'],quarterly:['ربع سنوي','Quarterly'],yearly:['سنوي','Yearly'],annual:['سنوي','Annual'],one_time:['مرة واحدة','One-time'],
 general:['عام','General'],billing:['فواتير ومطالبات','Billing'],accounting:['محاسبة ومالية','Accounting'],technical:['فني وتقني','Technical'],security:['أمن وسلامة','Security'],
 exempted:['معفاة','Exempted'],cancelled:['ملغاة','Cancelled'],
 floors_count:['عدد الطوابق','Floors count'],units_per_floor:['عدد الوحدات بكل طابق','Units per floor'],prefix:['بادئة الترقيم','Prefix'],
 reason:['السبب / المبرر','Reason'],rejection_reason:['سبب الرفض','Rejection reason'],decision:['القرار الإداري','Decision'],
 invoice_id:['رقم الفاتورة','Invoice ID'],amount_to_pay:['المبلغ المستحق للدفع','Amount to pay'],
 bank_name:['اسم البنك','Bank name'],account_number:['رقم الحساب','Account number'],
 advanced:['سجلات ومحاسبة متقدمة','Advanced records']
});

const t=(k)=>words[k]?.[lang==='ar'?0:1]||k;

const schemas={
 properties:['name','address','kind','instapay_id','wallet_number'],
 units:['property_id','unit_number','name','floor','area','occupancy_state','resident_phone'],
 parties:['name','kind','email','phone'],
 leases:['unit_id','party_id','start_date','end_date','rent','status'],
 ownerships:['unit_id','party_id','share'],
 accounts:['code','name','kind'],
 technicians:['name','specialty','phone'],
 assets:['property_id','name','serial','next_service'],
 tickets:['title','description','unit_id','priority','status'],
 announcements:['title','body'],
 invoices:['number','party_id','unit_id','description','amount','due_date'],
 expenses:['description','amount'],
 money_accounts:['property_id','name','kind','bank_name','account_number'],
 payment_proofs:['property_id','unit_id','party_id','amount','payment_method','reference'],
 fines:['property_id','unit_id','party_id','title','amount','destination'],
 vendor_contracts:['property_id','vendor_id','title','service_type','amount','frequency','start_date','end_date'],
 budgets:['property_id','name','period','start_date','end_date'],
 support_tickets:['title','description','category','priority']
};

const optional=new Set(['address','phone','email','area','floor','serial','next_service','asset_id','technician_id','description','bank_name','account_number','instapay_id','wallet_number','resident_phone']);
const relations={property_id:'properties',unit_id:'units',party_id:'parties',asset_id:'assets',technician_id:'technicians',vendor_id:'parties'};
const enums={
 units:{occupancy_state:['residing','closed','finishing']},
 properties:{kind:['tower','building']},
 parties:{kind:['owner','tenant']},
 tickets:{priority:['low','normal','urgent'],status:['open','assigned','in_progress','resolved','closed']},
 payment_proofs:{payment_method:['cash','bank_transfer','wallet','card','other']},
 support_tickets:{category:['general','billing','accounting','technical','security'],priority:['low','normal','high','critical']}
};

const iconNames={
 dashboard:'grid',collections:'wallet',properties:'building',units:'home',parties:'people',leases:'file',ownerships:'home',
 invoices:'file',receipts:'wallet',expenses:'chart',journals:'file',accounts:'chart',tickets:'tool',
 assets:'building',technicians:'people',reports:'chart',portal:'home',recurring_charges:'wallet',
 maintenance_plans:'tool',accounting_periods:'file',resident_messages:'bell',settings:'settings',
 profile:'people',notifications:'bell',announcements:'bell',audit_logs:'shield',members:'people',
 money_accounts:'wallet',payment_proofs:'check',fines:'clock',credit_balances:'wallet',
 expense_distributions:'chart',vendor_contracts:'file',budgets:'chart',support_tickets:'shield'
};

const descriptions={
 dashboard:['خزينة وصندوق العمارة، الرصيد، التحصيل الشهري، والمتأخرات.','Building Sandok, treasury balance, monthly dues and arrears.'],
 properties:['إدارة الأبراج والعمارات، إضافة وتعديل وحذف، وبيانات إنستاباي والمحافظ.','Towers management, add, edit, delete, and payment channels.'],
 units:['الشقق والوحدات، الملاك والشاغلون، حالات الإشغال والواتساب.','Units & apartments, occupancy states and direct WhatsApp.'],
 collections:['جدول التحصيل الشهري واشتراكات الصيانة وقنوات الدفع.','Monthly collection dues, maintenance fees and payment channels.'],
 expenses:['تسجيل ومتابعة مصروفات الصندوق (كهرباء، نظافة، أسانسير، صيانة).','Sandok expenses tracking (electricity, cleaning, elevator, repairs).'],
 invoices:['تابع المستحقات، وأصدر المطالبات، وسجّل التحصيل.','Manage your dues, outstanding balances and collections.'],
 payment_proofs:['مراجعة واعتماد إثباتات التحويل المرسلة من السكان.','Review and verify resident payment slips.'],
 tickets:['متابعة بلاغات الصيانة لخدمات ومرافق البرج المشتركة.','Follow every maintenance request from first report to resolution.'],
 reports:['رؤية أوضح لحسابات الصندوق والمصروفات والتحصيل.','A clearer picture of your treasury and collections.'],
 support_tickets:['تواصل مع الدعم الفني وتتبع تذاكرك ومستجداتها.','Contact support and track your inquiries and tickets.']
};

const localText=(ar,en)=>lang==='ar'?ar:en;
function navigate(next){page=next;cache={};document.body.classList.remove('nav-open');render();}
document.addEventListener('click',e=>{
 const b=e.target.closest('[data-go]');if(b)navigate(b.dataset.go);
 const add=e.target.closest('[data-new]');if(add)editor(add.dataset.new);
 const qc=e.target.closest('[data-quick]');if(qc)handleQuickAction(qc.dataset.quick);
});

function emptyState(entity,canAdd=false){
 return `<div class="empty"><span class="empty-icon">${icon(iconNames[entity]||'file',25)}</span><strong>${localText('المساحة جاهزة لبياناتك','Ready for your first record')}</strong><p>${localText('ستظهر سجلاتك هنا فور إضافتها، مع تفاصيلها وحالتها.','Your records will appear here with their details and current status.')}</p>${canAdd?`<button class="primary" data-new="${entity}">${icon('plus',16)}${t('add')} ${t(entity)}</button>`:''}</div>`;
}

const money=n=>new Intl.NumberFormat(lang==='ar'?'ar-EG':'en-GB',{style:'currency',currency:me?.organizations.find(x=>x.id===org)?.currency||'EGP',maximumFractionDigits:2}).format(Number(n||0));
const display=(key,v)=>relations[key]?cache[relations[key]]?.find(x=>x.id===v)?.name||v:t(String(v??''));
async function api(path,options={}){
 const q=org?(path.includes('?')?'&':'?')+'org='+encodeURIComponent(org):'';
 const res=await fetch('/api/'+path+q,{...options,headers:{'Content-Type':'application/json',...options.headers},credentials:'same-origin'});
 const rawText=await res.text();
 let data;
 try{data=rawText?JSON.parse(rawText):{};}catch{data={error:'HTTP '+res.status+': '+(res.statusText||'Server Error')};}
 if(!res.ok)throw new Error(data.error||res.statusText||('HTTP '+res.status));
 return data;
}
function toast(s){$('#toast').textContent=s;$('#toast').style.display='block';setTimeout(()=>$('#toast').style.display='none',4000);}
function theme(){document.documentElement.lang=lang;document.documentElement.dir=lang==='ar'?'rtl':'ltr';document.documentElement.classList.toggle('dark',dark);localStorage.tfLang=lang;localStorage.tfDark=dark;}
const brand=()=>`<div class="brand"><img src="/logo.svg" alt="TowerFlow"><span>TowerFlow<span>${localText('إدارة الأبراج وصندوق العمارة','TOWER & SANDOK')}</span></span></div>`;
const toggles=()=>`<button data-global="language" aria-label="${localText('تغيير اللغة','Change language')}">${lang==='ar'?'English':'العربية'}</button><button class="icon-button" data-global="theme" aria-label="${localText('تغيير المظهر','Toggle theme')}">${icon(dark?'sun':'moon',18)}</button>`;

document.addEventListener('click',async e=>{
 const b=e.target.closest('[data-global]');if(!b)return;
 if(b.dataset.global==='language')lang=lang==='ar'?'en':'ar';else dark=!dark;
 theme();if(me)await render();else auth();
});

function auth(register=false){
 theme();document.body.classList.remove('nav-open');
 $('#app').innerHTML=`<main class="auth"><section class="auth-art">${brand()}<div class="auth-story"><div class="eyebrow">${localText('إدارة الأبراج والعمارات · اتحاد الشاغلين','TOWER & SANDOK SYSTEM')}</div><h1>${localText('صندوق وخزينة العمارة.<br>إدارة أسهل وتحصيل أسرع.','Smart Building Management.<br>Simple, direct & effortless.')}</h1><p class="story-desc">${localText('متابعة رصيد الخزينة، اشتراكات الشقق الشهرية، التحصيل اللحظي عبر إنستاباي والمحافظ الإلكترونية، والتواصل المباشر مع الملاك والسكان عبر واتساب.','Manage treasury balance, monthly dues, instant WhatsApp reminders, and InstaPay / E-Wallet payments.')}</p>${architecture()}<div class="auth-benefits"><span>${icon('check',15)}${localText('صندوق وخزينة العمارة','Building Sandok')}</span><span>${icon('check',15)}${localText('إنستاباي ومحافظ ذكية','InstaPay & Wallets')}</span><span>${icon('check',15)}${localText('تواصل واتساب مباشر','Instant WhatsApp')}</span></div><a href="/TowerFlow-latest.apk" download="TowerFlow-v0.8.2.apk" class="auth-app-badge">${icon('android',16)} ${localText('تطبيق أندرويد متاح الآن v0.8.2 (APK)','Native Android APK v0.8.2 Available')}</a></div><div class="foot">© Ahmed Morgan ${new Date().getFullYear()} · TowerFlow ${localText('لإدارة الأبراج','Tower Management')}</div></section><section class="auth-main"><div class="auth-mobile-brand">${brand()}</div><div class="auth-tools">${toggles()}</div><form id="authform"><div class="login-symbol">${icon(register?'building':'shield',24)}</div><h1>${localText(register?'ابدأ مع TowerFlow':'أهلًا بعودتك',register?'Your workspace starts here':'Welcome back')}</h1><p>${localText(register?'أنشئ حسابك لإدارة برجك وصندوق العمارة.':'سجّل دخولك لمتابعة صندوق العمارة والشقق والتحصيلات.',register?'Create an account to manage your tower and sandok.':'Sign in to manage your building and dues.')}</p>${register?`<label>${t('name')}<input name="name" required maxlength="160" autocomplete="name" placeholder="${localText('اسمك الكامل','Your full name')}"></label><label>${t('organization')}<input name="organization" required maxlength="160" autocomplete="organization" placeholder="${localText('اسم البرج أو اتحاد الشاغلين','Tower / HOA Name')}"></label>`:''}<label>${t('email')}<input name="email" type="email" required autocomplete="email" placeholder="name@company.com"></label><label>${localText('كلمة المرور','Password')}<input name="password" type="password" minlength="12" maxlength="128" required autocomplete="${register?'new-password':'current-password'}" placeholder="${localText('12 حرفًا على الأقل','At least 12 characters')}"></label><div class="error" id="autherror" role="alert"></div><button class="primary" type="submit">${t(register?'signup':'signin')}${icon('arrow',17)}</button><div class="auth-divider">${localText('أو','or')}</div><button type="button" class="google-button" id="google-login"><span aria-hidden="true">G</span>${localText('المتابعة باستخدام Google','Continue with Google')}</button><p id="google-note" class="muted" role="status"></p><div class="auth-switch"><span>${localText(register?'لديك حساب بالفعل؟':'ليس لديك حساب بعد؟',register?'Already have an account?':'New to TowerFlow?')}</span><button type="button" class="text-button" id="switchauth">${localText(register?'تسجيل الدخول':'إنشاء حساب',register?'Sign in':'Create an account')}</button></div><div class="app-download-card">
  <div class="app-download-content">
    <div class="app-download-icon">${icon('android',22)}</div>
    <div class="app-download-text">
      <strong>${localText('تطبيق TowerFlow للأندرويد','TowerFlow Android App')}</strong>
      <small>${localText('تحميل مباشر للأجهزة الذكية · إصدار v0.8.2','Direct APK installation · v0.8.2')}</small>
    </div>
  </div>
  <a href="/TowerFlow-latest.apk" download="TowerFlow-v0.8.2.apk" class="app-download-btn">
    ${icon('android',17)} ${localText('تحميل التطبيق للأندرويد','Download for Android')}
  </a>
</div><details class="install-guide"><summary>${localText('تعليمات التثبيت وتخطي تحذير الحماية المعتاد','Installation guide & Play Protect steps')}</summary><div class="install-steps"><p>1. ${localText('عند بدء التنزيل: اختر <b>تنزيل على أي حال (Download anyway)</b>.','When downloading: tap <b>Download anyway</b>.')}</p><p>2. ${localText('عند فتح الملف: اضغط <b>الإعدادات</b> ثم فعّل <b>السماح بتثبيت التطبيقات من هذا المصدر</b>.','When opening: tap <b>Settings</b> and enable <b>Allow from this source</b>.')}</p><p>3. ${localText('إذا ظهر تنبيه Play Protect: اضغط <b>مزيد من التفاصيل (More details)</b> ثم <b>التثبيت على أي حال (Install anyway)</b> — الحزمة أصلية وموقعة بشهادة TowerFlow الرسمية.','If Play Protect warns: tap <b>More details</b> then <b>Install anyway</b> — The APK is clean and officially signed.')}</p><p>4. ${localText('إذا ظهر تعارض في الحزمة (Package conflicts): احذف النسخة القديمة من الهاتف أولاً ثم ثبّت هذه الحزمة.','If you get a package conflict error: uninstall the previous version from your phone first, then install.')}</p></div></details><p class="privacy-link"><a href="/privacy.html">${localText('الخصوصية وبياناتك','Privacy & your data')}</a></p><div class="auth-security">${icon('shield',14)}${localText('بيانات مؤسستك معزولة ومحمية بصلاحيات الوصول','Your workspace is isolated and protected by access controls')}</div></form></section></main>`;
 api('auth/providers').then(p=>{if(!p.google)$('#google-note')&&( $('#google-note').textContent=localText('الدخول بجوجل بانتظار تفعيل إعدادات الحساب. يمكنك استخدام البريد الآن.','Google is awaiting account setup. Email sign-in is available.'));}).catch(()=>{});
 $('#google-login').onclick=()=>{location.href='/api/auth/google/start';};
 const googleError=new URLSearchParams(location.search).get('google_error');if(googleError)$('#autherror').textContent=googleError;
 $('#switchauth').onclick=()=>auth(!register);
 $('#authform').onsubmit=async e=>{e.preventDefault();const b=e.submitter;b.disabled=true;try{await api('auth/'+(register?'register':'login'),{method:'POST',body:JSON.stringify(Object.fromEntries(new FormData(e.target)))});await boot();}catch(error){$('#autherror').textContent=error.message;}finally{b.disabled=false;}};
}

function role(){return me?.organizations.find(x=>x.id===org)?.role;}
function baseAllowed(){
 if(role()==='technician')return ['properties','units','tickets','announcements','notifications','support_tickets','profile','settings'];
 if(role()==='resident')return ['portal','invoices','payment_proofs','tickets','announcements','notifications','support_tickets','profile','settings'];
 return ['dashboard','collections','properties','units','expenses','tickets','payment_proofs','reports','announcements','notifications','support_tickets',...(['admin','manager'].includes(role())?['members']:[]),'profile','settings'];
}
function allowed(){
 return [...new Set([...baseAllowed(),...(['admin','manager'].includes(role())?['resident_messages']:[])])];
}
async function boot(){
 try{me=await api('me');org=me.organizations.some(x=>x.id===localStorage.tfOrg)?localStorage.tfOrg:me.organizations[0]?.id||'';if(role()==='resident')page='portal';await render();}catch{me=null;auth();}
}
async function rows(entity){return cache[entity]=await api(entity);}
function head(title,action=''){
 const desc=descriptions[page]?.[lang==='ar'?0:1]||localText('إدارة واضحة لكل التفاصيل، في مكان واحد.','Every detail, organized in one place.');
 return `<div class="pagehead"><div><h1>${escape(title)}</h1><p>${escape(desc)}</p></div><div class="actions">${action}</div></div>`;
}

function appHeader(){
  const available = allowed();
  const isRes = role() === 'resident';
  const mainItems = isRes ? [
    ['portal', localText('الرئيسية والصندوق','Dashboard'), 'home'],
    ['invoices', localText('مستحقاتي الشهري','My Invoices'), 'wallet'],
    ['payment_proofs', localText('إثبات سداد','Submit Proof'), 'check'],
    ['tickets', localText('طلبات الصيانة','Maintenance'), 'tool'],
    ['announcements', localText('الإعلانات','Announcements'), 'bell'],
    ['profile', localText('الملف الشخصي','Profile'), 'people']
  ] : [
    ['dashboard', localText('الرئيسية والصندوق','Sandok & Dashboard'), 'grid'],
    ['collections', localText('التحصيل الشهري','Monthly Collections'), 'wallet'],
    ['units', localText('الشقق والوحدات','Units & Condos'), 'home'],
    ['properties', localText('الأبراج والعمارات','Towers'), 'building'],
    ['expenses', localText('مصروفات الصندوق','Sandok Expenses'), 'chart'],
    ['tickets', localText('الصيانة والخدمات','Maintenance'), 'tool'],
    ['payment_proofs', localText('إثباتات التحويل','Payment Proofs'), 'check'],
    ['announcements', localText('الإعلانات','Announcements'), 'bell'],
    ['reports', localText('التقارير المالية','Financial Reports'), 'chart']
  ];

  return `
    <header class="app-header">
      <div class="app-bar">
        <div class="app-bar-start">
          <div class="app-brand" data-go="${isRes ? 'portal' : 'dashboard'}" role="button" tabindex="0">
            <img src="/logo.svg" alt="TowerFlow" class="app-logo">
            <div class="app-title-wrap">
              <span class="app-title">TowerFlow</span>
              <span class="app-subtitle">${localText('إدارة الأبراج وصندوق العمارة','Tower Management')}</span>
            </div>
          </div>

          <div class="tower-selector-pill">
            <span class="tower-icon">🏢</span>
            <select id="org" aria-label="${t('organization')}">
              ${me.organizations.map(x=>`<option value="${x.id}" ${x.id===org?'selected':''}>${escape(x.name)}</option>`).join('')}
            </select>
          </div>
        </div>

        <div class="app-bar-end">
          <div class="actions">
            ${toggles()}
            <button class="icon-button" data-go="notifications" aria-label="${t('notifications')}">${icon('bell',18)}</button>
          </div>
          <button class="profile profile-link" data-go="profile">
            <div class="avatar">${me.user.avatar?`<img src="${escape(me.user.avatar)}" alt="">`:escape(me.user.name.slice(0,1))}</div>
            <div class="profile-info">
              <b>${escape(me.user.name)}</b>
              <small>${t(me.user.platform_owner?'platform_owner':role())}</small>
            </div>
          </button>
          <button class="icon-button logout-btn" id="logout" aria-label="${t('logout')}" title="${t('logout')}">${icon('logout',17)}</button>
        </div>
      </div>

      <nav class="app-nav-tabs" aria-label="${localText('التنقل الرئيسي','Main Navigation')}">
        <div class="tabs-scroll-wrap">
          ${mainItems.filter(([k])=>available.includes(k)).map(([k, label, ic]) => `
            <button data-page="${k}" class="nav-tab ${k === page ? 'active' : ''}" ${k === page ? 'aria-current="page"' : ''}>
              ${icon(ic, 18)}
              <span>${label}</span>
            </button>
          `).join('')}
        </div>
      </nav>
    </header>
  `;
}

function bottomNav(){
  const isRes = role() === 'resident';
  const navItems = isRes ? [
    ['portal', localText('الرئيسية','Home'), 'home'],
    ['invoices', localText('مستحقاتي','Invoices'), 'wallet'],
    ['tickets', localText('الصيانة','Maintenance'), 'tool'],
    ['announcements', localText('الإعلانات','News'), 'bell'],
    ['profile', localText('حسابي','Profile'), 'people']
  ] : [
    ['collections', localText('التحصيل','Collect'), 'wallet'],
    ['dashboard', localText('الرئيسية','Home'), 'grid'],
    ['units', localText('الشقق','Units'), 'home'],
    ['expenses', localText('المصروفات','Expenses'), 'chart'],
    ['tickets', localText('الصيانة','Tickets'), 'tool']
  ];
  return `<nav class="mobile-bottom-nav" aria-label="${localText('شريط التنقل السريع','Mobile Navigation')}">
    ${navItems.map(([k, label, ic]) => `
      <button data-page="${k}" class="mobile-bottom-nav-item ${k === page ? 'active' : ''}">
        ${icon(ic, 19)}
        <span>${label}</span>
      </button>
    `).join('')}
  </nav>`;
}

async function render(){
  if(busy)return;busy=true;theme();localStorage.tfOrg=org;
  if(!allowed().includes(page))page=allowed()[0];
  $('#app').innerHTML=`
    <div class="app-shell">
      ${appHeader()}
      <main class="app-main">
        <div class="content-wrap">
          <section id="content" aria-live="polite">
            <div class="loading">${t('loading')}</div>
          </section>
          <footer class="footer">
            <span>© Ahmed Morgan ${new Date().getFullYear()} · TowerFlow</span>
            <span>${localText('إدارة الأبراج وصندوق العمارة','Building Sandok & Management')}</span>
          </footer>
        </div>
      </main>
      ${bottomNav()}
    </div>
  `;
  document.querySelectorAll('[data-page]').forEach(b=>b.onclick=()=>navigate(b.dataset.page));
  document.querySelectorAll('[data-go]').forEach(b=>b.onclick=()=>navigate(b.dataset.go));
  $('#org').onchange=e=>{org=e.target.value;cache={};render();};
  $('#logout').onclick=async()=>{await api('auth/logout',{method:'POST',body:'{}'});me=null;cache={};auth();};
  try{
    if(['dashboard','portal'].includes(page))await dashboard();
    else if(['recurring_charges','maintenance_plans','accounting_periods'].includes(page))await mountWorkflows({api,lang,page,escape,toast,money});
    else if(page==='collections')await collectionsView();else if(page==='reports')await mountReports({api,lang,escape,money});
    else if(page==='resident_messages')await mountMessages({api,lang,escape,toast});
    else if(page==='profile')await mountProfile({api,me,lang,escape,toast,onSave:async user=>{me.user=user;lang=user.locale||lang;await render();}});
    else if(page==='settings')settings();
    else if(page==='members')await members();
    else await listing(page);
  }catch(e){
    $('#content').innerHTML=head(t(page))+`<div class="notice" role="alert">${escape(e.message)}</div><button id="retry">${localText('إعادة المحاولة','Retry')}</button>`;
    $('#retry').onclick=render;
  }finally{busy=false;}
}

async function dashboard(){
 const month = localStorage.tfCollectionMonth || new Date().toISOString().slice(0, 7);
 const [colData, expList, recList, ticketsList, proofsList, summary] = await Promise.all([
   api('collections/monthly?month=' + month).catch(() => ({ summary: {}, units: [], property_settings: {} })),
   rows('expenses').catch(() => []),
   rows('receipts').catch(() => []),
   rows('tickets').catch(() => []),
   rows('payment_proofs').catch(() => []),
   api('dashboard').catch(() => ({ units: { total: 0, occupied: 0, vacant: 0, maintenance: 0 } }))
 ]);

 const s = colData.summary || {};
 const units = colData.units || [];
 const prop = colData.property_settings || {};

 const totalIn = recList.reduce((acc, r) => acc + Number(r.amount || 0), 0);
 const totalOut = expList.filter(e => e.status === 'approved' || !e.status).reduce((acc, e) => acc + Number(e.amount || 0), 0);
 const sandokBalance = totalIn - totalOut;

 const totalArrears = s.total_remaining !== undefined ? s.total_remaining : 0;
 const monthCollected = s.total_collected !== undefined ? s.total_collected : 0;
 const monthRate = s.collection_rate || 0;

 const openTickets = ticketsList.filter(x => !['closed', 'resolved'].includes(x.status));
 const pendingProofs = proofsList.filter(p => p.status === 'pending');

 const parts = month.split('-');
 const mNum = parseInt(parts[1], 10) || 1;
 const monthNamesAr = ['', 'يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];
 const monthNamesEn = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
 const monthDisplay = (lang === 'ar' ? monthNamesAr[mNum] : monthNamesEn[mNum]) + ' ' + parts[0];

 const quickActionsHtml = role() === 'resident' ? `
   <button class="primary" data-quick="submit_proof">${icon('plus',17)}${t('quick_proof')}</button>
   <button data-quick="new_ticket">${icon('tool',17)}${localText('طلب صيانة','Request Maintenance')}</button>
   <button data-quick="new_support">${icon('shield',17)}${localText('تذكرة دعم','Support Ticket')}</button>
 ` : `
   <button class="primary" data-go="collections">${icon('wallet',17)}${localText('⚡ جدول التحصيل الشهري','Monthly Collections')}</button>
   <button data-quick="expense">${icon('chart',17)}${localText('💸 إضافة مصروف','Add Expense')}</button>
   <button data-go="payment_proofs">${icon('check',17)}${t('payment_proofs')} ${pendingProofs.length?`<span class="badge warn">${pendingProofs.length}</span>`:''}</button>
   <button data-go="properties">${icon('building',17)}${localText('إدارة الأبراج','Towers')}</button>
 `;

 $('#content').innerHTML = head(localText('خزينة وصندوق العمارة', 'Building Sandok & Dashboard'), quickActionsHtml) + `
   <div class="card hero" style="background:linear-gradient(135deg, #0f766e 0%, #115e59 100%);color:#fff">
     <div class="hero-copy">
       <div class="eyebrow" style="color:#5eead4">${localText('منظومة إدارة الأبراج والعمارات · اتحاد الشاغلين','TOWER & OCCUPANTS MANAGEMENT')}</div>
       <h2 style="color:#fff">${localText('صندوق وخزينة ','Treasury: ')}${escape(me.organizations.find(o=>o.id===org)?.name || 'البرج')}</h2>
       <p style="color:#ccfbf1">${localText('متابعة رصيد الصندوق، الاشتراكات الشهرية، متأخرات السكان، والتحصيل عبر إنستاباي والمحافظ.','Track treasury balance, monthly dues, arrears, and electronic collections.')}</p>
       <span class="hero-tag" style="background:rgba(255,255,255,0.2);color:#fff">${monthDisplay}</span>
     </div>
     ${architecture()}
   </div>

   <div class="card" style="margin-top:1.25rem;background:var(--bg-accent, #eff6ff);border:1px solid #bfdbfe;padding:1.1rem 1.4rem">
     <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:0.75rem">
       <div>
         <h4 style="margin:0 0 0.4rem 0;color:#1d4ed8;font-size:1.05rem;display:flex;align-items:center;gap:0.5rem">
           <span>⚡📱</span> ${localText('قنوات الدفع اللحظي والمحافظ الإلكترونية للبرج:','Tower Electronic & InstaPay Channels:')}
         </h4>
         <div style="display:flex;gap:2rem;flex-wrap:wrap;align-items:center">
           <div>
             <span class="muted" style="font-size:0.85rem">${localText('معرف إنستاباي (InstaPay IPA):','InstaPay IPA:')}</span>
             <br><strong style="font-size:1.1rem;color:#1e3a8a">${prop.instapay_id ? escape(prop.instapay_id) : `<span class="muted" style="font-weight:normal;font-size:0.9rem">${localText('غير محدد (أضفه من إعدادات البرج)','Not set')}</span>`}</strong>
           </div>
           <div>
             <span class="muted" style="font-size:0.85rem">${localText('المحفظة الإلكترونية (فودافون كاش / وي):','E-Wallet Mobile:')}</span>
             <br><strong style="font-size:1.1rem;color:#1e3a8a">${prop.wallet_number ? escape(prop.wallet_number) : `<span class="muted" style="font-weight:normal;font-size:0.9rem">${localText('غير محدد (أضفه من إعدادات البرج)','Not set')}</span>`}</strong>
           </div>
         </div>
       </div>
       <div>
         <button class="ghost small" data-go="properties" style="font-size:0.85rem;color:#1d4ed8;border-color:#93c5fd">${localText('⚙️ تعديل بيانات الدفع والبرج','Edit Tower Payment Info')}</button>
       </div>
     </div>
   </div>

   <div class="quick-hub" style="margin-top:1.25rem">
     ${role() === 'resident' ? `
       <button class="quick-hub-btn primary" data-quick="submit_proof">${icon('plus',18)}<span>${localText('إرسال إثبات سداد','Submit Payment Proof')}</span></button>
       <button class="quick-hub-btn" data-quick="new_ticket">${icon('tool',18)}<span>${localText('طلب صيانة جديد','Request Maintenance')}</span></button>
       <button class="quick-hub-btn" data-go="invoices">${icon('wallet',18)}<span>${localText('مستحقاتي','My Invoices')}</span></button>
     ` : `
       <button class="quick-hub-btn primary" data-go="collections">${icon('wallet',18)}<span>${localText('⚡ التحصيل الشهري','Monthly Collections')}</span></button>
       <button class="quick-hub-btn" data-quick="expense">${icon('chart',18)}<span>${localText('💸 تسجيل مصروف','Add Expense')}</span></button>
       <button class="quick-hub-btn" data-go="units">${icon('home',18)}<span>${localText('🚪 الشقق والوحدات','Units & Condos')}</span></button>
       <button class="quick-hub-btn" data-go="payment_proofs">${icon('check',18)}<span>${localText('📱 إثباتات التحويل','Payment Proofs')} ${pendingProofs.length?`<span class="badge warn">${pendingProofs.length}</span>`:''}</span></button>
       <button class="quick-hub-btn" data-quick="new_ticket">${icon('tool',18)}<span>${localText('🛠️ بلاغ صيانة','New Ticket')}</span></button>
     `}
   </div>

   <div class="metrics" style="margin-top:1.25rem">
     <article class="card metric">
       <div class="metric-top"><small>${localText('رصيد الخزينة والصندوق الفعلي','Treasury & Sandok Balance')}</small><span class="metric-icon">${icon('wallet',18)}</span></div>
       <strong style="color:var(--primary,#0f766e)" title="${money(sandokBalance)}">${money(sandokBalance)}</strong>
       <div class="metric-foot"><span class="dot"></span>${localText('صافي الرصيد الحالي بعد خصم المصروفات','Current net balance')}</div>
     </article>

     <article class="card metric">
       <div class="metric-top"><small>${localText('إجمالي المتأخرات على الشقق','Total Resident Arrears')}</small><span class="metric-icon" style="color:#ef4444">${icon('clock',18)}</span></div>
       <strong style="color:#ef4444" title="${money(totalArrears)}">${money(totalArrears)}</strong>
       <div class="metric-foot"><span class="dot" style="background:#ef4444"></span>${localText('مستحقات معلقة غير مسددة','Pending uncollected dues')}</div>
     </article>

     <article class="card metric">
       <div class="metric-top"><small>${localText('تحصيل شهر ' + monthDisplay,'Collections for ' + monthDisplay)}</small><span class="metric-icon" style="color:#10b981">${icon('chart',18)}</span></div>
       <strong style="color:#10b981" title="${money(monthCollected)}">${money(monthCollected)}</strong>
       <div class="metric-foot"><span class="dot" style="background:#10b981"></span>${localText('نسبة التحصيل: ','Collection rate: ')}<strong>${monthRate}%</strong></div>
     </article>

     <article class="card metric">
       <div class="metric-top"><small>${localText('الشقق والوحدات','Units & Condos')}</small><span class="metric-icon">${icon('building',18)}</span></div>
       <strong title="${summary.units.total||units.length}">${summary.units.total||units.length}</strong>
       <div class="metric-foot"><span class="dot"></span>${localText('كل الوحدات مسجلة كـ تمليك','All registered as owned')}</div>
     </article>
   </div>

   <div class="grid" style="margin-top:1.5rem">
     <article class="card">
       <div class="card-header">
         <h2>${localText('حالات إشغال الوحدات','Unit Occupancy States')}</h2>
         <button class="text-button" data-go="units">${localText('إدارة الوحدات','View all units')}</button>
       </div>
       <div style="display:flex;flex-direction:column;gap:1rem">
         <div style="display:grid;grid-template-columns:repeat(3, 1fr);gap:0.75rem;text-align:center">
           <div style="background:#ecfdf5;border:1px solid #a7f3d0;border-radius:8px;padding:0.75rem">
             <span style="font-size:1.5rem">🟢</span>
             <h4 style="margin:0.25rem 0 0 0;font-size:0.85rem;color:#065f46">${localText('مشغولة مقيمة','Residing')}</h4>
             <b style="font-size:1.25rem;color:#047857">${units.filter(u=>u.occupancy_state==='residing'||(!u.occupancy_state&&u.is_paid)).length}</b>
           </div>
           <div style="background:#fefce8;border:1px solid #fef08a;border-radius:8px;padding:0.75rem">
             <span style="font-size:1.5rem">🟡</span>
             <h4 style="margin:0.25rem 0 0 0;font-size:0.85rem;color:#854d0e">${localText('مغلقة','Closed')}</h4>
             <b style="font-size:1.25rem;color:#a16207">${units.filter(u=>u.occupancy_state==='closed').length}</b>
           </div>
           <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:0.75rem">
             <span style="font-size:1.5rem">🔵</span>
             <h4 style="margin:0.25rem 0 0 0;font-size:0.85rem;color:#1e40af">${localText('تحت التشطيب','Finishing')}</h4>
             <b style="font-size:1.25rem;color:#2563eb">${units.filter(u=>u.occupancy_state==='finishing').length}</b>
           </div>
         </div>
         <div style="margin-top:0.5rem;padding:0.75rem;background:var(--bg-muted,#f8fafc);border-radius:8px;font-size:0.85rem;color:var(--text-muted,#64748b)">
           <span>ℹ️ ${localText('الهدف من المنظومة هو إدارة البرج واتحاد الشاغلين فقط. جميع الوحدات تمليك ولا توجد عقود تأجير خاصة.','Condo management scope only. All units are owned (تمليك).')}</span>
         </div>
       </div>
     </article>

     <article class="card">
       <div class="card-header">
         <h2>${localText('طلبات الصيانة المفتوحة','Open Maintenance')}</h2>
         <button class="text-button" data-go="tickets">${localText('عرض الكل','View all')}</button>
       </div>
       ${openTickets.length ? openTickets.slice(-4).reverse().map(x => `
         <div class="listrow">
           <div class="listrow-main"><span class="list-icon">${icon('tool',17)}</span><div><b>${escape(x.title)}</b><p class="muted">${t(x.priority)}</p></div></div>
           <span class="badge ${x.priority==='urgent'?'warn':''}">${t(x.status)}</span>
         </div>
       `).join('') : `<div class="empty"><span class="empty-icon">${icon('check',25)}</span><strong>${localText('لا توجد بلاغات صيانة مفتوحة','Everything is fine')}</strong><p>${localText('مرافق وخدمات البرج تعمل بشكل سليم.','All tower facilities are in good condition.')}</p></div>`}
     </article>
   </div>

   <div class="dashboard-bottom" style="margin-top:1.5rem">
     <article class="card">
        <div class="card-header">
          <h2>${localText('بطاقات تحصيل شقق البرج لشهر ' + monthDisplay, 'Tower Units Smart Collection Cards')}</h2>
          <button class="primary small" data-go="collections">${localText('فتح شاشة التحصيل الكاملة ⚡','Open Full Collections')}</button>
        </div>
        ${units.length ? `
          <div class="units-grid" style="margin-top:0.75rem">
            ${units.slice(0, 6).map(u => {
              let resDisplay = u.resident_name;
              if (!resDisplay || resDisplay.includes('شاغرة') || resDisplay === 'vacant') {
                resDisplay = localText('مالك الوحدة (تمليك)', 'Unit Owner (Owned)');
              }
              const occState = u.occupancy_state || 'residing';
              const occBadge = occState === 'closed'
                ? `<span class="badge" style="background:#fef9c3;color:#854d0e">🟡 ${localText('مغلقة','Closed')}</span>`
                : (occState === 'finishing'
                  ? `<span class="badge" style="background:#dbeafe;color:#1e40af">🔵 ${localText('تشطيب','Finishing')}</span>`
                  : `<span class="badge" style="background:#dcfce7;color:#166534">🟢 ${localText('مشغولة','Residing')}</span>`);
              const cleanPhone = (u.resident_phone || '').replace(/[^0-9]/g, '');
              const waPhone = cleanPhone.startsWith('0') ? '20' + cleanPhone.substring(1) : cleanPhone;
              const waMsg = u.is_paid
                ? localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، تم استلام اشتراك صيانة شهر ' + month + ' بسند رقم ' + (u.receipt_reference || '-') + '. شكراً لتعاونكم - إدارة البرج.', 'Dues received.')
                : localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، نذكركم بسداد اشتراك صيانة شهر ' + month + ' بمبلغ ' + u.monthly_fee + ' ج.م. شاكرين حسن تعاونكم - إدارة البرج.', 'Dues reminder.');

              return `
                <div class="unit-card ${u.is_paid ? 'paid' : 'unpaid'}">
                  <div class="unit-card-header">
                    <div class="unit-card-title">
                      <span class="unit-num-badge">🏢 ${localText('شقة', 'Unit')} ${escape(u.unit_number || u.unit_name)}</span>
                      <span class="unit-floor-badge">${localText('طابق', 'Floor')} ${u.floor || 0}</span>
                    </div>
                    <div class="unit-card-pills">
                      ${occBadge}
                      ${u.is_paid ? `<span class="badge ok">${localText('✅ مسدد', 'Paid')}</span>` : `<span class="badge warn">${localText('⏳ غير مسدد', 'Unpaid')}</span>`}
                    </div>
                  </div>
                  <div class="unit-card-body">
                    <div class="unit-owner-row">
                      <div class="unit-owner-name">
                        <span>👤 ${escape(resDisplay)}</span>
                      </div>
                      ${u.resident_phone ? `<div class="unit-owner-phone"><a href="tel:${escape(u.resident_phone)}">📞 ${escape(u.resident_phone)}</a></div>` : ''}
                    </div>
                    <div class="unit-fee-row">
                      <span class="unit-fee-label">${localText('الاشتراك المطلوب:', 'Monthly Dues:')}</span>
                      <span class="unit-fee-amount">${money(u.monthly_fee)}</span>
                    </div>
                    ${u.is_paid ? `
                      <div class="unit-paid-info">
                        <span>🧾 ${localText('سند قبض:', 'Receipt:')} <strong>${escape(u.receipt_reference || '-')}</strong></span>
                      </div>
                    ` : ''}
                  </div>
                  <div class="unit-card-actions">
                    ${!u.is_paid ? `
                      <button class="primary small btn-quick-collect" data-unit="${escape(u.unit_id)}" data-name="${escape(u.unit_number||u.unit_name)}" data-res="${escape(resDisplay)}" data-fee="${u.monthly_fee}">
                        ⚡ ${localText('تحصيل فوري', 'Quick Collect')}
                      </button>
                    ` : `
                      <span class="unit-paid-info" style="flex:1;text-align:center;justify-content:center;background:#ecfdf5;font-weight:600">✓ ${localText('تم التحصيل', 'Paid')}</span>
                    `}
                    ${u.resident_phone ? `
                      <a href="https://api.whatsapp.com/send?phone=${encodeURIComponent(waPhone)}&text=${encodeURIComponent(waMsg)}" target="_blank" class="button small whatsapp-btn">
                        💬 ${localText('واتساب', 'WhatsApp')}
                      </a>
                    ` : ''}
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        ` : `<div class="empty"><p>${localText('لم يتم تسجيل شقق في هذا البرج بعد.','No units registered yet.')}</p><button class="primary" data-go="units">${localText('إضافة الشقق','Add Units')}</button></div>`}
      </article>
   </div>
 `;

 document.querySelectorAll('.btn-quick-collect').forEach(btn => {
   btn.onclick = () => {
     const uId = btn.dataset.unit;
     const uName = btn.dataset.name;
     const uRes = btn.dataset.res;
     const uFee = btn.dataset.fee || '500';

     modal(localText('⚡ تسجيل تحصيل فوري - شقة ' + uName, 'Quick Collection - Unit ' + uName),
       `<p class="muted" style="margin-bottom:0.75rem">${localText('الساكن المسدد: ','Resident: ')}<strong>${escape(uRes)}</strong></p>` +
       field('amount', uFee, { placeholder: localText('المبلغ المراد تحصيله (ج.م)', 'Amount to collect') }) +
       field('payment_channel', 'cash', {
         choices: [
           ['cash', localText('💵 نقدي / كاش', 'Cash')],
           ['instapay', localText('⚡ إنستاباي InstaPay', 'InstaPay')],
           ['wallet', localText('📱 محفظة إلكترونية (فودافون كاش / وي)', 'E-Wallet')],
           ['bank_transfer', localText('🏦 تحويل بنكي مباشر', 'Bank Transfer')]
         ]
       }) +
       field('reference', '', { placeholder: localText('رقم المرجع / الإيصال (اختياري)', 'Reference / Receipt No') }),
       async (b) => {
         if (!b.amount || Number(b.amount) <= 0) return uiAlert(localText('مبلغ غير صحيح', 'Invalid Amount'), localText('يرجى إدخال مبلغ تحصيل صحيح بالجنيه المصري', 'Invalid amount'), 'danger');
         try {
           const res = await api('collections/quick-collect', {
             method: 'POST',
             body: JSON.stringify({
               unit_id: uId,
               amount: b.amount,
               payment_channel: b.payment_channel,
               method: b.payment_channel,
               reference: b.reference,
               month: month
             })
           });
           toast(localText('تم التحصيل بنجاح! سند قبض رقم: ' + res.receipt_reference, 'Collection successful! Receipt: ' + res.receipt_reference));
           cache = {};
           await render();
         } catch (err) {
           uiAlert(localText('تنبيه', 'Notice'), err.message, 'danger');
         }
       }
     );
   };
 });
}

const columns={
 properties:['name','kind','address','instapay_id','wallet_number'],
 units:['unit_number','name','property_id','floor','occupancy_state','resident_phone'],
 parties:['name','kind','phone','email'],
 leases:['unit_id','party_id','start_date','end_date','rent','status'],
 ownerships:['unit_id','party_id','share'],
 invoices:['number','party_id','description','amount','due_date','status'],
 receipts:['reference','amount','received_on'],
 expenses:['description','amount','status'],
 money_accounts:['name','property_id','kind','balance'],
 payment_proofs:['reference','unit_id','amount','payment_method','status','created_at'],
 fines:['title','unit_id','amount','status','destination'],
 credit_balances:['party_id','property_id','balance'],
 vendor_contracts:['title','property_id','vendor_id','amount','frequency','status'],
 budgets:['name','property_id','period','start_date','end_date','status'],
 support_tickets:['ticket_number','title','category','priority','status','created_at'],
 journals:['description','entry_date','status'],
 accounts:['code','name','kind'],
 tickets:['title','priority','status','technician_id'],
 assets:['name','property_id','serial','next_service'],
 technicians:['name','specialty','phone'],
 announcements:['title','body','created_at'],
 notifications:['message','created_at'],
 audit_logs:['entity','action','entity_id','created_at']
};

function canWrite(entity){
 const r=role();
 if(r==='admin')return true;
 if(r==='viewer')return false;
 if(r==='resident')return ['tickets','payment_proofs','support_tickets'].includes(entity);
 if(['invoices','expenses','accounts','journals','money_accounts','fines','credit_balances'].includes(entity))return ['manager','accountant'].includes(r);
 if(['assets','tickets','technicians','vendor_contracts','budgets'].includes(entity))return ['manager','technician'].includes(r);
 return r==='manager';
}

async function listing(entity){
 const data=await rows(entity);
 const cols=columns[entity]||[];
 const deps=[...new Set(cols.filter(k=>relations[k]).map(k=>relations[k]))];
 await Promise.all(deps.map(rows));

 $('#content').innerHTML=head(
   t(entity),
   `<button id="export">${t('export')}</button>${(schemas[entity]||entity==='journals')&&canWrite(entity)?`<button class="primary" id="add">＋ ${t('add')}</button>`:''}`
 )+`
   <div class="table-toolbar">
     <div class="search-wrap">${icon('search',18)}<input id="search" class="search" aria-label="${t('search')}" placeholder="${t('search')}"></div>
     <span class="table-count">${data.length} ${localText('سجل','records')}</span>
   </div>
   <div id="records"></div>
 `;

 const draw=(list)=>{
   $('#records').innerHTML=list.length?`
     <div class="tablewrap">
       <table>
         <thead><tr>${cols.map(k=>`<th>${t(k)}</th>`).join('')}<th></th></tr></thead>
         <tbody>
           ${list.map(x=>`
             <tr>
               ${cols.map(k=>`
                 <td>
                   ${['status','priority'].includes(k)?`<span class="badge ${['pending','urgent','draft','open','high','critical'].includes(x[k])?'warn':''}">${escape(t(x[k]))}</span>`:['amount','rent','balance'].includes(k)?money(x[k]):escape(display(k,x[k]))}
                 </td>
               `).join('')}
               <td>
                 <div class="actions">
                   ${(schemas[entity]&&!['invoices','expenses','payment_proofs'].includes(entity)&&canWrite(entity)&&role()!=='resident')?`<button data-edit="${x.id}">${t('edit')}</button>`:''}${entity==='properties'&&['admin','manager'].includes(role())?`<button data-del-prop="${x.id}" class="danger" style="margin-inline-start:4px">${localText('🗑️ حذف البرج','Delete Tower')}</button>`:''}
                   ${entity==='invoices'&&canWrite(entity)?(x.status==='draft'?`<button data-issue="${x.id}">${t('issue')}</button>`:x.status==='issued'?`<button data-collect="${x.id}">${t('collect')}</button>`:''):''}
                   ${entity==='expenses'&&x.status==='pending'&&['admin','manager'].includes(role())?`<button data-approve="${x.id}">${t('approve')}</button><button data-reject="${x.id}" class="danger">${t('reject')}</button>`:''}
                   ${entity==='payment_proofs'&&x.status==='pending'&&['admin','manager'].includes(role())?`<button data-approve-proof="${x.id}">${t('approve')}</button><button data-reject-proof="${x.id}" class="danger">${t('reject')}</button>`:''}
                   ${entity==='fines'&&x.status==='pending'&&['admin','manager'].includes(role())?`<button data-exempt-fine="${x.id}">${t('exempt_btn')}</button>`:''}
                 </div>
               </td>
             </tr>
           `).join('')}
         </tbody>
       </table>
     </div>
   `:`<div class="card">${emptyState(entity,!!schemas[entity]&&canWrite(entity))}</div>`;

   document.querySelectorAll('[data-edit]').forEach(b=>b.onclick=()=>editor(entity,data.find(x=>x.id===b.dataset.edit)));
   document.querySelectorAll('[data-del-prop]').forEach(b=>b.onclick=async()=>{
      const ok = await uiConfirm(
        localText('حذف البرج السكني', 'Delete Tower'),
        localText('تحذير هام: هل أنت متأكد من حذف هذا البرج نهائياً بما يشمل كافة وحداته وسجلاته المالية؟ هذا الإجراء لا يمكن التراجع عنه.', 'Warning: Are you sure you want to permanently delete this tower and all its units and records?'),
        { danger: true, confirmText: localText('نعم، حذف البرج نهائياً', 'Yes, Delete Tower') }
      );
      if(!ok) return;
      try{
        await api('properties/'+b.dataset.delProp+'?force=true',{method:'DELETE'});
        toast(localText('تم حذف البرج بنجاح','Tower deleted successfully'));
        cache={};await render();
      }catch(err){
        uiAlert(localText('تعذر حذف البرج', 'Delete Error'), err.message, 'danger');
      }
    });
   document.querySelectorAll('[data-issue]').forEach(b=>b.onclick=()=>perform(()=>api(`invoices/${b.dataset.issue}/issue`,{method:'POST',body:'{}'})));
   document.querySelectorAll('[data-collect]').forEach(b=>b.onclick=()=>collection(b.dataset.collect));
   for(const [attr,decision] of [['approve','approved'],['reject','rejected']]) {
     document.querySelectorAll(`[data-${attr}]`).forEach(b=>b.onclick=()=>perform(()=>api(`expenses/${b.dataset[attr]}/decide`,{method:'POST',body:JSON.stringify({decision})})));
   }
   document.querySelectorAll('[data-approve-proof]').forEach(b=>b.onclick=()=>perform(()=>api(`payment_proofs/${b.dataset.approveProof}/review`,{method:'POST',body:JSON.stringify({decision:'approved'})})));
   document.querySelectorAll('[data-reject-proof]').forEach(b=>b.onclick=()=>modal(t('reject'),field('reason','',{optional:false}),res=>api(`payment_proofs/${b.dataset.rejectProof}/review`,{method:'POST',body:JSON.stringify({decision:'rejected',rejection_reason:res.reason})})));
   document.querySelectorAll('[data-exempt-fine]').forEach(b=>b.onclick=()=>modal(t('exempt_btn'),field('reason','',{optional:false}),res=>api(`fines/${b.dataset.exemptFine}/exempt`,{method:'POST',body:JSON.stringify({reason:res.reason})})));
 };

 draw(data);
 $('#search').oninput=e=>{const q=e.target.value.toLowerCase();draw(data.filter(x=>cols.some(k=>String(display(k,x[k])).toLowerCase().includes(q))));};
 $('#export').onclick=()=>csv(entity,cols,data);
 if($('#add'))$('#add').onclick=()=>entity==='journals'?journalForm():editor(entity);
}

function csv(name,cols,data){
 const cell=v=>'"'+String(v??'').replace(/^[=+@\-\t\r]/,"'$&").replaceAll('"','""')+'"';
 const text='\ufeff'+[cols.map(t),...data.map(x=>cols.map(k=>display(k,x[k])))].map(r=>r.map(cell).join(',')).join('\r\n');
 const url=URL.createObjectURL(new Blob([text],{type:'text/csv;charset=utf-8'}));
 const a=document.createElement('a');a.href=url;a.download='towerflow-'+name+'.csv';a.click();
 setTimeout(()=>URL.revokeObjectURL(url),500);
}

function uiConfirm(title, message, options = {}) {
  return new Promise((resolve) => {
    const d = $('#modal');
    const isDanger = options.danger ?? true;
    const confirmLabel = options.confirmText || (isDanger ? localText('تأكيد الحذف', 'Delete') : localText('تأكيد', 'Confirm'));
    const cancelLabel = options.cancelText || localText('إلغاء', 'Cancel');
    const iconGlyph = options.icon || (isDanger ? '🗑️' : '⚠️');

    d.innerHTML = `
      <div class="dialog-content">
        <div class="dialog-header">
          <span class="dialog-icon">${iconGlyph}</span>
          <h3>${escape(title)}</h3>
        </div>
        <div class="dialog-body">
          <p>${escape(message)}</p>
        </div>
        <div class="dialog-actions">
          <button type="button" class="ghost" id="dlg-cancel">${cancelLabel}</button>
          <button type="button" class="${isDanger ? 'danger' : 'primary'}" id="dlg-confirm">${confirmLabel}</button>
        </div>
      </div>
    `;

    const cancelBtn = $('#dlg-cancel');
    const confirmBtn = $('#dlg-confirm');

    cancelBtn.onclick = () => {
      d.close();
      resolve(false);
    };

    confirmBtn.onclick = () => {
      d.close();
      resolve(true);
    };

    d.oncancel = (e) => {
      e.preventDefault();
      d.close();
      resolve(false);
    };

    d.showModal();
    confirmBtn.focus();
  });
}

function uiAlert(title, message, type = 'info') {
  return new Promise((resolve) => {
    const d = $('#modal');
    const iconGlyph = type === 'danger' ? '⚠️' : type === 'success' ? '✅' : 'ℹ️';
    const okLabel = localText('حسناً', 'OK');

    d.innerHTML = `
      <div class="dialog-content">
        <div class="dialog-header">
          <span class="dialog-icon">${iconGlyph}</span>
          <h3>${escape(title)}</h3>
        </div>
        <div class="dialog-body">
          <p>${escape(message)}</p>
        </div>
        <div class="dialog-actions">
          <button type="button" class="primary" id="dlg-ok">${okLabel}</button>
        </div>
      </div>
    `;

    const okBtn = $('#dlg-ok');
    okBtn.onclick = () => {
      d.close();
      resolve();
    };

    d.oncancel = (e) => {
      e.preventDefault();
      d.close();
      resolve();
    };

    d.showModal();
    okBtn.focus();
  });
}

function uiPrompt(title, message, defaultValue = '', options = {}) {
  return new Promise((resolve) => {
    const d = $('#modal');
    const inputType = options.inputType || 'text';
    const confirmLabel = options.confirmText || localText('متابعة', 'Continue');
    const cancelLabel = options.cancelText || localText('إلغاء', 'Cancel');
    const iconGlyph = options.icon || '⚡';

    d.innerHTML = `
      <form id="dlg-prompt-form" class="dialog-content">
        <div class="dialog-header">
          <span class="dialog-icon">${iconGlyph}</span>
          <h3>${escape(title)}</h3>
        </div>
        <div class="dialog-body">
          <p>${escape(message)}</p>
          <div class="dialog-input-wrap">
            <input id="dlg-prompt-input" type="${inputType}" value="${escape(defaultValue)}" required autofocus>
          </div>
        </div>
        <div class="dialog-actions">
          <button type="button" class="ghost" id="dlg-prompt-cancel">${cancelLabel}</button>
          <button type="submit" class="primary">${confirmLabel}</button>
        </div>
      </form>
    `;

    const cancelBtn = $('#dlg-prompt-cancel');
    const form = $('#dlg-prompt-form');
    const input = $('#dlg-prompt-input');

    cancelBtn.onclick = () => {
      d.close();
      resolve(null);
    };

    form.onsubmit = (e) => {
      e.preventDefault();
      const val = input.value;
      d.close();
      resolve(val);
    };

    d.oncancel = (e) => {
      e.preventDefault();
      d.close();
      resolve(null);
    };

    d.showModal();
    input.focus();
    input.select();
  });
}

// Global window fallbacks for safety
window.alert = (msg) => uiAlert(localText('تنبيه', 'Notice'), String(msg));
window.confirm = (msg) => uiConfirm(localText('تأكيد الإجراء', 'Confirm Action'), String(msg));
window.prompt = (msg, dflt) => uiPrompt(localText('إدخال مطلوب', 'Input Required'), String(msg), dflt || '');

function modal(title,fields,submit){
 const d=$('#modal');
 d.innerHTML=`<form id="editor"><h2>${escape(title)}</h2><div class="fields">${fields}</div><div class="error" id="formerror" role="alert"></div><div class="actions"><button type="button" id="cancel">${t('cancel')}</button><button class="primary" type="submit">${t('save')}</button></div></form>`;
 $('#cancel').onclick=()=>d.close();
 $('#editor').onsubmit=async e=>{
   e.preventDefault();e.submitter.disabled=true;
   try{await submit(Object.fromEntries(new FormData(e.target)));d.close();toast(t('saved'));cache={};await render();}catch(error){$('#formerror').textContent=error.message;}finally{e.submitter.disabled=false;}
 };
 d.showModal();
}

function field(key,value='',options={}){
 const label=t(key),choices=options.choices;
 return `<label class="${['description','body','title','terms','notes'].includes(key)?'full':''}">${label}${choices?`<select name="${key}" ${options.optional?'':'required'}>${options.optional?'<option value="">—</option>':''}${choices.map(x=>`<option value="${escape(x.value??x)}" ${String(x.value??x)===String(value)?'selected':''}>${escape(x.label??t(x))}</option>`).join('')}</select>`:`<input name="${key}" value="${escape(value)}" type="${key.endsWith('_date')||key==='next_service'?'date':['amount','rent','area','share','floor','floors_count','units_per_floor','balance'].includes(key)?'number':key==='email'?'email':'text'}" ${['amount','rent','area','share','balance'].includes(key)?'min="0.01" step="0.01"':''} ${options.optional?'':'required'} maxlength="4000">`}</label>`;
}

async function editor(entity,record={}){
 try{
   let keys=schemas[entity]||[];
   if(role()==='resident'&&entity==='tickets')keys=['title','description','unit_id','priority'];
   await Promise.all([...new Set(keys.filter(k=>relations[k]).map(k=>relations[k]))].map(rows));
   modal(t(entity),keys.map(k=>field(k,record[k],{optional:optional.has(k)||(entity==='invoices'&&k==='unit_id'),choices:relations[k]?cache[relations[k]].map(x=>({value:x.id,label:x.name})):enums[entity]?.[k]})).join(''),b=>api(entity+(record.id?'/'+record.id:''),{method:record.id?'PATCH':'POST',body:JSON.stringify(b)}));
 }catch(e){toast(e.message);}
}

async function perform(fn){try{await fn();toast(t('saved'));cache={};await render();}catch(e){toast(e.message);}}

function collection(id){
 modal(t('collect'),field('amount')+field('reference'),b=>api('invoices/'+id+'/collect',{method:'POST',body:JSON.stringify(b)}));
}

async function handleQuickAction(action){
 if(action==='collect'){
   const invoices=(await rows('invoices')).filter(i=>i.status==='issued');
   const opts=invoices.map(i=>({value:i.id,label:`${i.number} · ${money(i.amount)}`}));
   modal(t('quick_collect'),field('invoice_id','',{choices:opts})+field('amount')+field('reference'),b=>api(`invoices/${b.invoice_id}/collect`,{method:'POST',body:JSON.stringify({amount:b.amount,reference:b.reference})}));
 }else if(action==='expense'){
   editor('expenses');
 }else if(action==='submit_proof'){
   editor('payment_proofs');
 }else if(action==='new_ticket'){
   editor('tickets');
 }else if(action==='new_support'){
   editor('support_tickets');
 }else if(action==='units_batch'){
   const props=await rows('properties');
   const opts=props.map(p=>({value:p.id,label:p.name}));
   const typeOpts=[{value:'rental',label:localText('وحدات إيجار','Rental units')},{value:'owned',label:localText('وحدات تمليك','Owned units')}];
   modal(localText('توليد أدوار وشقق','Batch Generate Units'),field('property_id','',{choices:opts})+field('floors_count','5')+field('units_per_floor','4')+field('ownership_type','rental',{choices:typeOpts})+field('prefix',''),b=>api(`properties/${b.property_id}/units_batch`,{method:'POST',body:JSON.stringify({floors:b.floors_count,units_per_floor:b.units_per_floor,prefix:b.prefix,ownership_type:b.ownership_type})}));
 }
}

async function journalForm(){
 try{
   const accounts=await rows('accounts');
   const opts=accounts.map(x=>({value:x.id,label:x.code+' · '+x.name}));
   modal(t('journals'),field('description')+field('debit_account','',{choices:opts})+field('credit_account','',{choices:opts})+field('amount'),b=>api('journals',{method:'POST',body:JSON.stringify({description:b.description,lines:[{account_id:b.debit_account,debit:b.amount},{account_id:b.credit_account,credit:b.amount}]})}));
 }catch(e){toast(e.message);}
}

async function members(){
 const data=await api('members');
 $('#content').innerHTML=head(t('members'),role()==='admin'?`<button class="primary" id="addmember">＋ ${t('add')}</button>`:'')+`
   <div class="card">${data.map(x=>`<div class="listrow"><div><b>${escape(x.name)}</b><p class="muted">${escape(x.email)}</p></div><span class="badge">${t(x.role)}</span></div>`).join('')}</div>
 `;
 if($('#addmember'))$('#addmember').onclick=async()=>{
   try{
     await rows('parties');
     modal(t('members'),`<p class="muted full">${lang==='ar'?'يجب أن ينشئ المستخدم حسابه أولًا. اربط الساكن بسجل المستأجر لإتاحة وحدته وفواتيره.':'The user must register first. Link residents to their tenant record for unit and invoice access.'}</p>`+field('email')+field('role','viewer',{choices:['viewer','resident','technician','accountant','manager','admin']})+field('party_id','',{optional:true,choices:cache.parties.map(x=>({value:x.id,label:x.name}))}),b=>api('members',{method:'POST',body:JSON.stringify(b)}));
   }catch(e){toast(e.message);}
 };
}

function settings(){
 $('#content').innerHTML=head(t('settings'))+`
   <div class="card">
     <h2>${escape(me.organizations.find(x=>x.id===org)?.name)}</h2>
     <p>${escape(me.user.email)}</p>
     <p class="muted">${lang==='ar'?'المؤسسات والأبراج معزولة إداريًا وماليًا. التحويل بين الأبراج ممنوع منعًا باتًا. القيود المرحلة غير قابلة للتعديل.':'Towers and organizations are strictly isolated financially and administratively. Inter-tower transfers are forbidden.'}</p>
     <button id="neworg">${lang==='ar'?'إنشاء مؤسسة إضافية':'Create another workspace'}</button>
   </div>
   <div class="notice">
     ${lang==='ar'?'TowerFlow Free مجاني بالكامل للأبد لجميع الوظائف. النسخة التجارية تقدم دعمًا فنيًا ذا أولوية وإعدادًا مخصصًا ومستوى خدمة SLA.':'TowerFlow Free is completely free forever for all tower operations. Commercial edition offers priority SLAs and dedicated assistance.'}
   </div>
 `;
 $('#neworg').onclick=()=>modal(t('organization'),field('name'),async b=>{await api('organizations',{method:'POST',body:JSON.stringify(b)});me=await api('me');});
}

theme();
boot();


async function collectionsView(){
  const month = localStorage.tfCollectionMonth || new Date().toISOString().slice(0, 7);
  let viewMode = localStorage.tfCollectionView || 'cards';
  let filterState = localStorage.tfCollectionFilter || 'all';
  const data = await api('collections/monthly?month=' + month);
  const s = data.summary || {};
  const allUnits = data.units || [];

  const parts = month.split('-');
  const mNum = parseInt(parts[1], 10) || 1;
  const monthNamesAr = ['', 'يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];
  const monthNamesEn = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthDisplay = (lang === 'ar' ? monthNamesAr[mNum] : monthNamesEn[mNum]) + ' ' + parts[0];

  const shift = (offset) => {
    let y = parseInt(parts[0], 10);
    let m = parseInt(parts[1], 10) + offset;
    if (m < 1) { m = 12; y--; }
    if (m > 12) { m = 1; y++; }
    return y + '-' + (m < 10 ? '0' + m : m);
  };

  const paidCount = allUnits.filter(u => u.is_paid).length;
  const unpaidCount = allUnits.filter(u => !u.is_paid).length;
  const filteredUnits = allUnits.filter(u => {
    if (filterState === 'paid') return u.is_paid;
    if (filterState === 'unpaid') return !u.is_paid;
    if (filterState === 'closed') return u.occupancy_state === 'closed';
    if (filterState === 'finishing') return u.occupancy_state === 'finishing';
    return true;
  });

  const renderUnitCard = (u) => {
    let resDisplay = u.resident_name;
    if (!resDisplay || resDisplay.includes('شاغرة') || resDisplay === 'vacant') {
      resDisplay = localText('مالك الوحدة (تمليك)', 'Unit Owner (Owned)');
    }
    const occState = u.occupancy_state || 'residing';
    const occBadge = occState === 'closed'
      ? `<span class="badge" style="background:#fef9c3;color:#854d0e">🟡 ${localText('مغلقة','Closed')}</span>`
      : (occState === 'finishing'
        ? `<span class="badge" style="background:#dbeafe;color:#1e40af">🔵 ${localText('تحت التشطيب','Finishing')}</span>`
        : `<span class="badge" style="background:#dcfce7;color:#166534">🟢 ${localText('مشغولة مقيمة','Residing')}</span>`);
    const cleanPhone = (u.resident_phone || '').replace(/[^0-9]/g, '');
    const waPhone = cleanPhone.startsWith('0') ? '20' + cleanPhone.substring(1) : cleanPhone;
    const waMsg = u.is_paid
      ? localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، تم استلام اشتراك صيانة شهر ' + month + ' بسند رقم ' + (u.receipt_reference || '-') + '. شكراً لتعاونكم - إدارة البرج.', 'Dues received.')
      : localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، نذكركم بسداد اشتراك صيانة شهر ' + month + ' بمبلغ ' + u.monthly_fee + ' ج.م. شاكرين حسن تعاونكم - إدارة البرج.', 'Dues reminder.');

    return `
      <div class="unit-card ${u.is_paid ? 'paid' : 'unpaid'}">
        <div class="unit-card-header">
          <div class="unit-card-title">
            <span class="unit-num-badge">🏢 ${localText('شقة', 'Unit')} ${escape(u.unit_number || u.unit_name)}</span>
            <span class="unit-floor-badge">${localText('طابق', 'Floor')} ${u.floor || 0}</span>
          </div>
          <div class="unit-card-pills">
            ${occBadge}
            ${u.is_paid ? `<span class="badge ok">${localText('✅ تم السداد', 'Paid')}</span>` : `<span class="badge warn">${localText('⏳ غير مسدد', 'Unpaid')}</span>`}
          </div>
        </div>

        <div class="unit-card-body">
          <div class="unit-owner-row">
            <div class="unit-owner-name">
              <span>👤 ${escape(resDisplay)}</span>
              ${!u.party_id ? `<button class="ghost micro btn-assign-owner" data-unit="${escape(u.unit_id)}" data-name="${escape(u.unit_number||u.unit_name)}" data-kind="owner" data-occ="${escape(occState)}" style="padding:2px 6px;font-size:0.75rem">${localText('✏️ تعيين','Set')}</button>` : ''}
            </div>
            ${u.resident_phone ? `<div class="unit-owner-phone"><a href="tel:${escape(u.resident_phone)}">📞 ${escape(u.resident_phone)}</a></div>` : ''}
          </div>

          <div class="unit-fee-row">
            <span class="unit-fee-label">${localText('الاشتراك المطلوب لشهر ' + monthDisplay + ':', 'Dues for ' + monthDisplay + ':')}</span>
            <span class="unit-fee-amount">${money(u.monthly_fee)}</span>
          </div>

          ${u.is_paid ? `
            <div class="unit-paid-info">
              <span>🧾 ${localText('سند قبض:', 'Receipt:')} <strong>${escape(u.receipt_reference || '-')}</strong></span>
              ${u.collected_at ? `<small class="muted">${escape(u.collected_at)}</small>` : ''}
            </div>
          ` : ''}
        </div>

        <div class="unit-card-actions">
          ${!u.is_paid ? `
            <button class="primary btn-quick-collect" data-unit="${escape(u.unit_id)}" data-name="${escape(u.unit_number||u.unit_name)}" data-res="${escape(resDisplay)}" data-fee="${u.monthly_fee}">
              ⚡ ${localText('تحصيل فوري', 'Quick Collect')}
            </button>
          ` : `
            <span class="unit-paid-info" style="flex:1;text-align:center;justify-content:center;background:#ecfdf5;font-weight:600">✓ ${localText('مكتمل السداد', 'Paid')}</span>
          `}
          ${u.resident_phone ? `
            <a href="https://api.whatsapp.com/send?phone=${encodeURIComponent(waPhone)}&text=${encodeURIComponent(waMsg)}" target="_blank" class="button whatsapp-btn">
              💬 ${localText('واتساب', 'WhatsApp')}
            </a>
          ` : ''}
        </div>
      </div>
    `;
  };

  const renderTable = (list) => `
    <div class="tablewrap">
      <table>
        <thead>
          <tr>
            <th>${t('unit')}</th>
            <th>${localText('الساكن / المالك','Resident / Owner')}</th>
            <th>${localText('المبلغ الشهري','Monthly Fee')}</th>
            <th>${t('status')}</th>
            <th>${localText('بيانات السداد','Payment Details')}</th>
            <th>${localText('إجراء','Action')}</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(u => {
            let resDisplay = u.resident_name;
            if (!resDisplay || resDisplay.includes('شاغرة') || resDisplay === 'vacant') {
              resDisplay = localText('مالك الوحدة (تمليك)', 'Unit Owner (Owned)');
            }
            const occState = u.occupancy_state || 'residing';
            const occBadge = occState === 'closed'
              ? `<span class="badge" style="background:#fef9c3;color:#854d0e">🟡 ${localText('مغلقة','Closed')}</span>`
              : (occState === 'finishing'
                ? `<span class="badge" style="background:#dbeafe;color:#1e40af">🔵 ${localText('تحت التشطيب','Finishing')}</span>`
                : `<span class="badge" style="background:#dcfce7;color:#166534">🟢 ${localText('مشغولة مقيمة','Residing')}</span>`);
            const kindBadge = `<span class="badge" style="background:#e0f2fe;color:#0369a1">${localText('تمليك','Owned')}</span>`;
            const cleanPhone = (u.resident_phone || '').replace(/[^0-9]/g, '');
            const waPhone = cleanPhone.startsWith('0') ? '20' + cleanPhone.substring(1) : cleanPhone;
            const waMsg = u.is_paid
              ? localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، تم استلام اشتراك صيانة شهر ' + month + ' بسند رقم ' + (u.receipt_reference || '-') + '. شكراً لتعاونكم - إدارة البرج.', 'Dues received.')
              : localText('مرحباً ساكن شقة ' + (u.unit_number || u.unit_name) + '، نذكركم بسداد اشتراك صيانة شهر ' + month + ' بمبلغ ' + u.monthly_fee + ' ج.م. شاكرين حسن تعاونكم - إدارة البرج.', 'Dues reminder.');

            return `
            <tr>
              <td><strong>${localText('شقة','Unit')} ${escape(u.unit_number||u.unit_name)}</strong><br><small class="muted">${localText('طابق','Floor')} ${u.floor||0} · ${localText('تمليك','Owned')}</small></td>
              <td>
                <strong>${escape(resDisplay)}</strong><br>
                <small class="muted">${kindBadge} · ${occBadge}${u.resident_phone?' · 📞 '+escape(u.resident_phone):''}</small>
                ${!u.party_id ? `<br><button class="ghost small btn-assign-owner" data-unit="${escape(u.unit_id)}" data-name="${escape(u.unit_number||u.unit_name)}" data-kind="owner" data-occ="${escape(occState)}" style="margin-top:4px;padding:2px 6px;font-size:0.75rem">${localText('👤 تحديد المالك/الساكن والحالة','👤 Set Resident & State')}</button>` : ''}
              </td>
              <td><strong>${money(u.monthly_fee)}</strong></td>
              <td>${u.is_paid?'<span class="badge ok">'+localText('✅ تم السداد','Paid')+'</span>':'<span class="badge warn">'+localText('⏳ غير مسدد','Unpaid')+'</span>'}</td>
              <td>${u.is_paid?`<small>🧾 ${escape(u.receipt_reference||'-')}<br>${escape(u.collected_at||'')}</small>`:'-'}</td>
              <td>
                ${u.is_paid?'<span class="muted">'+localText('مكتمل','Done')+'</span>':`<button class="primary small btn-quick-collect" data-unit="${escape(u.unit_id)}" data-name="${escape(u.unit_number||u.unit_name)}" data-res="${escape(resDisplay)}" data-fee="${u.monthly_fee}">${localText('⚡ تحصيل فوري','Quick Collect')}</button>`}
                ${u.resident_phone ? `<a href="https://api.whatsapp.com/send?phone=${encodeURIComponent(waPhone)}&text=${encodeURIComponent(waMsg)}" target="_blank" class="button small ghost" style="margin-inline-start:4px;color:#059669;border-color:#a7f3d0" title="${localText('مراسلة على واتساب','WhatsApp message')}">💬 واتساب</a>` : ''}
              </td>
            </tr>
            `;
          }).join('')}
        </tbody>
      </table>
    </div>
  `;

  $('#content').innerHTML = `
    ${head(t('collections'), `<button class="primary" id="btn-batch-generate">${localText('⚡ توليد مطالبات الشهر','⚡ Generate Monthly Dues')}</button>`)}
    ${data.property_settings && (data.property_settings.instapay_id || data.property_settings.wallet_number) ? `
      <div class="card" style="margin-bottom:1rem;background:var(--bg-accent, #eff6ff);border:1px solid #bfdbfe;padding:1rem 1.25rem">
        <h4 style="margin:0 0 0.5rem 0;color:#1d4ed8;font-size:0.95rem">📱 ${localText('قنوات الدفع اللحظي والمحافظ الإلكترونية للبرج:','Tower Electronic & InstaPay Channels:')}</h4>
        <div style="display:flex;gap:1.5rem;flex-wrap:wrap">
          ${data.property_settings.instapay_id ? `<div><small class="muted">${localText('معرف إنستاباي:','InstaPay IPA:')}</small> <strong style="font-size:1.05rem;color:#1e3a8a">⚡ ${escape(data.property_settings.instapay_id)}</strong></div>` : ''}
          ${data.property_settings.wallet_number ? `<div><small class="muted">${localText('محفظة إلكترونية (فودافون كاش / إنستاباي):','E-Wallet Mobile:')}</small> <strong style="font-size:1.05rem;color:#1e3a8a">💳 ${escape(data.property_settings.wallet_number)}</strong></div>` : ''}
        </div>
      </div>
    ` : ''}
    <div class="card" style="margin-bottom:1rem">
      <div style="display:flex;align-items:center;justify-content:space-between">
        <button class="ghost" id="btn-prev-month">‹ ${localText('الشهر السابق','Previous Month')}</button>
        <h2 style="margin:0;font-size:1.25rem">${monthDisplay}</h2>
        <button class="ghost" id="btn-next-month">${localText('الشهر التالي','Next Month')} ›</button>
      </div>
    </div>
    <div class="kpi-grid">
      <article class="card kpi"><h3>${localText('إجمالي المطلوب','Total Due')}</h3><b>${money(s.total_due||0)}</b><small>${localText('اشتراكات صيانة وخدمات البرج','Tower maintenance dues')}</small></article>
      <article class="card kpi"><h3>${localText('المحصل فعلياً','Collected')}</h3><b style="color:var(--success,#10b981)">${money(s.total_collected||0)}</b><small>${localText('نسبة التحصيل','Rate')} ${s.collection_rate||0}%</small></article>
      <article class="card kpi"><h3>${localText('المتبقي للتحصيل','Remaining')}</h3><b style="color:var(--danger,#ef4444)">${money(s.total_remaining||0)}</b><small>${localText('مستحقات معلقة','Pending')}</small></article>
      <article class="card kpi"><h3>${localText('الوحدات المأهولة','Occupancy')}</h3><b>${s.occupied_units||0} / ${s.total_units||0}</b><small>${localText('وحدة مسجلة','registered units')}</small></article>
    </div>

    <!-- Units Control Bar: Switcher (Cards vs Table) & Filter Tabs -->
    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;margin-top:1.5rem">
      <div class="view-switcher">
        <button class="view-btn ${viewMode === 'cards' ? 'active' : ''}" id="btn-view-cards">🗂️ ${localText('بطاقات ذكية', 'Smart Cards')}</button>
        <button class="view-btn ${viewMode === 'table' ? 'active' : ''}" id="btn-view-table">📋 ${localText('جدول تفصيلي', 'Table')}</button>
      </div>

      <div style="display:flex;gap:6px;flex-wrap:wrap">
        <button class="view-btn ${filterState === 'all' ? 'active' : ''}" id="filter-all">${localText('الكل (' + allUnits.length + ')', 'All (' + allUnits.length + ')')}</button>
        <button class="view-btn ${filterState === 'unpaid' ? 'active' : ''}" id="filter-unpaid" style="color:var(--danger,#ef4444)">⏳ ${localText('غير مسدد (' + unpaidCount + ')', 'Unpaid (' + unpaidCount + ')')}</button>
        <button class="view-btn ${filterState === 'paid' ? 'active' : ''}" id="filter-paid" style="color:var(--success,#10b981)">✅ ${localText('مسدد (' + paidCount + ')', 'Paid (' + paidCount + ')')}</button>
      </div>
    </div>

    <div id="units-view-container" style="margin-top:1rem">
      ${viewMode === 'cards' ? `
        <div class="units-grid">
          ${filteredUnits.map(renderUnitCard).join('')}
        </div>
      ` : `
        <article class="card">
          ${renderTable(filteredUnits)}
        </article>
      `}
      ${filteredUnits.length === 0 ? `
        <div class="card" style="text-align:center;padding:2rem;color:var(--text-muted,#64748b)">
          ${localText('لا توجد شقق مطابقة للفلتر المحدد.', 'No units matching the selected filter.')}
        </div>
      ` : ''}
    </div>
  `;

  $('#btn-prev-month').onclick = () => { localStorage.tfCollectionMonth = shift(-1); collectionsView(); };
  $('#btn-next-month').onclick = () => { localStorage.tfCollectionMonth = shift(1); collectionsView(); };

  $('#btn-view-cards').onclick = () => { localStorage.tfCollectionView = 'cards'; collectionsView(); };
  $('#btn-view-table').onclick = () => { localStorage.tfCollectionView = 'table'; collectionsView(); };

  $('#filter-all').onclick = () => { localStorage.tfCollectionFilter = 'all'; collectionsView(); };
  $('#filter-unpaid').onclick = () => { localStorage.tfCollectionFilter = 'unpaid'; collectionsView(); };
  $('#filter-paid').onclick = () => { localStorage.tfCollectionFilter = 'paid'; collectionsView(); };

  $('#btn-batch-generate').onclick = async () => {
    const fee = await uiPrompt(
      localText('⚡ توليد مطالبات اشتراك الصيانة والخدمات', 'Generate Monthly Maintenance Dues'),
      localText('أدخل قيمة اشتراك الصيانة والخدمات الشهري المستحق على الشقق (بالجنيه المصري):', 'Enter default monthly maintenance fee for units (EGP):'),
      '500',
      { inputType: 'number', confirmText: localText('توليد المطالبات الآن ⚡', 'Generate Now ⚡'), icon: '⚡' }
    );
    if (!fee) return;
    try {
      const res = await api('collections/generate-month', { method: 'POST', body: JSON.stringify({ month, default_fee: fee }) });
      toast(localText('تم توليد ' + res.generated_count + ' مطالبة بنجاح', 'Generated ' + res.generated_count + ' dues successfully'));
      collectionsView();
    } catch (err) {
      uiAlert(localText('تعذر توليد المطالبات', 'Generation Error'), err.message, 'danger');
    }
  };

  document.querySelectorAll('.btn-assign-owner').forEach(btn => {
    btn.onclick = () => {
      const uId = btn.dataset.unit;
      const uName = btn.dataset.name;
      const currentOcc = btn.dataset.occ || 'residing';
      modal(localText('تحديد بيانات المالك / الساكن لشقة ' + uName, 'Set Resident / Owner for Unit ' + uName),
        field('name', '', { placeholder: localText('اسم المالك أو الساكن بالكامل', 'Full Name') }) +
        field('phone', '', { placeholder: localText('رقم الهاتف للتواصل والواتساب', 'Phone number') }) +
        field('occupancy_state', currentOcc, {
          choices: [
            ['residing', localText('مشغولة ومقيمة 🟢', 'Residing 🟢')],
            ['closed', localText('مغلقة 🟡', 'Closed 🟡')],
            ['finishing', localText('تحت التشطيب 🔵', 'Finishing 🔵')]
          ]
        }),
        async (b) => {
          if (!b.name || !b.name.trim()) return uiAlert(localText('بيانات ناقصة', 'Missing Data'), localText('يرجى إدخال اسم المالك أو الساكن بالكامل', 'Please enter a name'), 'danger');
          try {
            await api('units/' + uId + '/assign-resident', {
              method: 'POST',
              body: JSON.stringify({
                name: b.name.trim(),
                phone: (b.phone || '').trim(),
                kind: 'owner',
                occupancy_state: b.occupancy_state || 'residing'
              })
            });
            toast(localText('تم حفظ وتعيين المالك وحالة الشقة بنجاح', 'Assigned successfully'));
            collectionsView();
          } catch (err) {
            uiAlert(localText('تنبيه', 'Notice'), err.message, 'danger');
          }
        }
      );
    };
  });

  document.querySelectorAll('.btn-quick-collect').forEach(btn => {
    btn.onclick = () => {
      const uId = btn.dataset.unit;
      const uName = btn.dataset.name;
      const uRes = btn.dataset.res;
      const uFee = btn.dataset.fee || '500';

      modal(localText('⚡ تسجيل تحصيل فوري - شقة ' + uName, 'Quick Collection - Unit ' + uName),
        `<p class="muted" style="margin-bottom:0.75rem">${localText('الساكن المسدد: ','Resident: ')}<strong>${escape(uRes)}</strong></p>` +
        field('amount', uFee, { placeholder: localText('المبلغ المراد تحصيله (ج.م)', 'Amount to collect') }) +
        field('payment_channel', 'cash', {
          choices: [
            ['cash', localText('💵 نقدي / كاش', 'Cash')],
            ['instapay', localText('⚡ إنستاباي InstaPay', 'InstaPay')],
            ['wallet', localText('📱 محفظة إلكترونية (فودافون كاش / وي)', 'E-Wallet')],
            ['bank_transfer', localText('🏦 تحويل بنكي مباشر', 'Bank Transfer')]
          ]
        }) +
        field('reference', '', { placeholder: localText('رقم المرجع / الإيصال (اختياري)', 'Reference / Receipt No') }),
        async (b) => {
          if (!b.amount || Number(b.amount) <= 0) return uiAlert(localText('مبلغ غير صحيح', 'Invalid Amount'), localText('يرجى إدخال مبلغ تحصيل صحيح بالجنيه المصري', 'Invalid amount'), 'danger');
          try {
            const res = await api('collections/quick-collect', {
              method: 'POST',
              body: JSON.stringify({
                unit_id: uId,
                amount: b.amount,
                payment_channel: b.payment_channel,
                method: b.payment_channel,
                reference: b.reference,
                month: month
              })
            });
            toast(localText('تم التحصيل بنجاح! سند قبض رقم: ' + res.receipt_reference, 'Collection successful! Receipt: ' + res.receipt_reference));
            collectionsView();
          } catch (err) {
            uiAlert(localText('تنبيه', 'Notice'), err.message, 'danger');
          }
        }
      );
    };
  });
}
