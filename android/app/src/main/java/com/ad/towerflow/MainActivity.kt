package com.ad.towerflow

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.messaging.FirebaseMessaging
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors

/** Native Android Views; no WebView, AI SDKs, or embedded service credentials. */
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var token = "" // Memory only. Signing out or process death clears the credential.
    private var base = "https://towerflow.vercel.app"
    private var member = JSONObject()
    private var pendingNotifications = false
    private var avatar: String? = null
    private var org = ""
    private var role = ""
    private var arabic = true
    private var dark = false
    private var organizations = JSONArray()
    private lateinit var screen: LinearLayout
    private val names = linkedMapOf(
        "properties" to Pair("العقارات والأبراج", "Properties"), "units" to Pair("الوحدات", "Units"),
        "parties" to Pair("الملاك والمستأجرون", "People"), "leases" to Pair("العقود", "Leases"),
        "ownerships" to Pair("الملكيات", "Ownerships"), "invoices" to Pair("الفواتير", "Invoices"),
        "receipts" to Pair("التحصيل", "Receipts"), "expenses" to Pair("المصروفات", "Expenses"),
        "journals" to Pair("القيود", "Journals"), "accounts" to Pair("الحسابات", "Accounts"),
        "tickets" to Pair("الصيانة", "Maintenance"), "assets" to Pair("الأصول", "Assets"),
        "technicians" to Pair("الفنيون", "Technicians"), "announcements" to Pair("الإعلانات", "Announcements"),
        "notifications" to Pair("الإشعارات", "Notifications"), "trial_balance" to Pair("ميزان المراجعة", "Trial balance"),
        "audit_logs" to Pair("التدقيق", "Audit log")
    )
    private fun text(ar: String, en: String) = if (arabic) ar else en
    private fun title(k: String) = names[k]?.let { if (arabic) it.first else it.second } ?: k
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); login(); handleAuth(intent); pendingNotifications = intent?.extras?.getBoolean("open_notifications", false) == true || intent?.hasExtra("notification_id") == true; getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("towerflow_residents", "TowerFlow · رسائل السكان", NotificationManager.IMPORTANCE_HIGH)) }
    private fun scaffold(heading: String) {
        val scroll = ScrollView(this)
        screen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(36), dp(22), dp(24)); layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR; setBackgroundColor(Color.parseColor(if (dark) "#102331" else "#F4F7F9")) }
        scroll.addView(screen); setContentView(scroll)
        label("AD · TowerFlow", 26); label(heading, 21); label("© Ahmed Morgan " + java.time.Year.now().value, 12)
        val toggles = LinearLayout(this)
        toggles.addView(Button(this).apply { this.text = if (arabic) "English" else "العربية"; setOnClickListener { arabic = !arabic; if (token.isEmpty()) login() else home() } })
        toggles.addView(Button(this).apply { this.text = "☀ / ☾"; setOnClickListener { dark = !dark; if (token.isEmpty()) login() else home() } })
        screen.addView(toggles)
    }
    private fun label(value: String, size: Int = 16): TextView = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(Color.parseColor(if (dark) "#DFEDF4" else "#193349")); setPadding(0, dp(12), 0, dp(12)); screen.addView(this) }
    private fun input(hint: String, password: Boolean = false): EditText = EditText(this).apply { this.hint = hint; setSingleLine(); inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT; setTextColor(Color.parseColor(if (dark) "#DFEDF4" else "#193349")); setHintTextColor(Color.GRAY); screen.addView(this) }
    private fun button(value: String, fn: () -> Unit) { screen.addView(Button(this).apply { text = value; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#087F79")); setOnClickListener { fn() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) }) }
    private fun error(e: Exception) { AlertDialog.Builder(this).setTitle(text("تعذر تنفيذ الطلب", "Request failed")).setMessage(e.message).setPositiveButton("OK", null).show() }
    private fun call(path: String, method: String = "GET", data: JSONObject? = null, done: (String) -> Unit) {
        worker.execute {
            try {
                val connection = URL(base + "/api/" + path + (if (path.contains('?')) "&" else "?") + "org=" + org).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method; connection.connectTimeout = 15000; connection.readTimeout = 20000
                    connection.setRequestProperty("Content-Type", "application/json"); connection.setRequestProperty("X-TowerFlow-Client", "android")
                    if (token.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")
                    if (data != null) { connection.doOutput = true; connection.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) } }
                    val code = connection.responseCode
                    val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                    if (code !in 200..299) throw IllegalStateException(JSONObject(raw).optString("error", "HTTP $code"))
                    runOnUiThread { try { done(raw) } catch (e: Exception) { error(e) } }
                } finally { connection.disconnect() }
            } catch (e: Exception) { runOnUiThread { error(e) } }
        }
    }
    private fun login(register: Boolean = false) {
        scaffold(text(if (register) "إنشاء حساب" else "تسجيل الدخول", if (register) "Create account" else "Sign in"))
        label(text("أهلًا بك في TowerFlow", "Welcome to TowerFlow"))
        val name = if (register) input(text("الاسم", "Name")) else null
        val organization = if (register) input(text("المؤسسة", "Organization")) else null
        val email = input(text("البريد الإلكتروني", "Email")).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val pass = input(text("كلمة المرور · 12 حرفًا على الأقل", "Password · minimum 12 characters"), true)
        button(text("متابعة", "Continue")) {
            try {
                val uri = URI(base)
                require(uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) { "A valid HTTPS origin is required" }
                base = uri.toString().trimEnd('/'); getPreferences(0).edit().putString("base", base).apply()
                val payload = JSONObject().put("email", email.text.toString()).put("password", pass.text.toString())
                if (register) payload.put("name", name?.text.toString()).put("organization", organization?.text.toString())
                call("auth/" + if (register) "register" else "login", "POST", payload) { raw -> token = JSONObject(raw).getString("token"); pass.text.clear(); loadMe() }
            } catch (e: Exception) { error(e) }
        }
        button(text("المتابعة باستخدام Google", "Continue with Google")) { googleLogin() }
        button(text(if (register) "لدي حساب" else "إنشاء حساب ومؤسسة", if (register) "I have an account" else "Create account & workspace")) { login(!register) }
    }
    private fun loadMe() { call("me") { raw -> member = JSONObject(raw).getJSONObject("user"); organizations = JSONObject(raw).getJSONArray("organizations"); if (organizations.length() > 0) { val o = organizations.getJSONObject(0); org = o.getString("id"); role = o.getString("role") }; registerPush(); if (pendingNotifications) { pendingNotifications = false; listing("notifications") } else home() } }
    private fun home() {
        scaffold(text("إدارة عقاراتك بوضوح", "Your properties. In perspective."))
        if (organizations.length() > 0) {
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until organizations.length()).map { organizations.getJSONObject(it).getString("name") })
            spinner.setSelection((0 until organizations.length()).firstOrNull { organizations.getJSONObject(it).getString("id") == org } ?: 0)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {} ; override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { val o = organizations.getJSONObject(position); org = o.getString("id"); role = o.getString("role") } }
            screen.addView(spinner)
        }
        if (role in listOf("admin", "manager", "accountant")) { button(text("الخزائن والحسابات", "Treasury")) { treasuryScreen() }; button(text("إنشاء برج مستقل", "Create independent building")) { independentBuildingForm() } }
        button(text("ملفي الشخصي", "My profile")) { profile() }
        button(text("تفعيل إشعارات الموبايل", "Enable phone notifications")) { enablePush() }
        label(text("بيانات حقيقية من مؤسستك. تُطبق الصلاحيات على الخادم.", "Live workspace data. Permissions are enforced on the server."))
        names.keys.filter { role != "resident" || it in listOf("units", "leases", "invoices", "tickets", "announcements", "notifications") }.forEach { key -> button(title(key)) { listing(key) } }
        button(text("تسجيل الخروج", "Sign out")) { disablePushAndLogout() }
    }
    private fun listing(entity: String) {
        scaffold(title(entity)); button(text("الرئيسية", "Home")) { home() }
        if (entity in forms.keys && role in listOf("admin", "manager", "accountant", "technician")) button(text("إضافة سجل", "Add record")) { recordForm(entity) }
        if (entity == "tickets") button(text("طلب صيانة", "Request maintenance")) { ticketForm() }
        call(entity) { raw ->
            val records = JSONArray(raw)
            if (records.length() == 0) label(text("لا توجد سجلات بعد", "No records yet"))
            for (i in 0 until records.length()) {
                val row = records.getJSONObject(i)
                val heading = listOf("name", "title", "number", "description", "message", "entity").map { row.optString(it) }.firstOrNull { it.isNotEmpty() } ?: "Record"
                label(heading, 19)
                val details = listOf("status", "amount", "due_date", "priority", "phone", "address", "body", "debit", "credit", "paid_amount", "action").filter { row.has(it) && !row.isNull(it) }.joinToString(" · ") { "$it: ${row.optString(it)}" }
                label(details)
                if (entity in forms.keys && entity !in listOf("invoices", "expenses") && role in listOf("admin", "manager", "accountant", "technician")) button(text("تعديل", "Edit")) { recordForm(entity, row) }
                if (entity == "expenses" && row.optString("status") == "pending" && role in listOf("admin", "manager")) {
                    listOf("approved", "rejected").forEach { decision -> button(text(if (decision == "approved") "اعتماد" else "رفض", decision)) { call("expenses/" + row.getString("id") + "/decide", "POST", JSONObject().put("decision", decision)) { listing(entity) } } }
                }
                if (entity == "expenses" && row.optString("status") == "approved" && row.optDouble("paid_amount") < row.optDouble("amount") && role in listOf("admin", "manager", "accountant")) button(text("تسجيل دفع المصروف", "Pay expense")) { expensePaymentForm(row.getString("id")) }
                if (entity == "notifications" && row.isNull("read_at")) button(text("تمت القراءة", "Mark as read")) { call("notifications/" + row.getString("id") + "/read", "POST", JSONObject()) { listing(entity) } }
                if (entity == "invoices" && role in listOf("admin", "manager", "accountant")) {
                    val id = row.getString("id")
                    if (row.optString("status") == "draft") button(text("إصدار وترحيل", "Issue & post")) { call("invoices/$id/issue", "POST", JSONObject()) { listing(entity) } }
                    if (row.optString("status") == "issued") button(text("تحصيل", "Collect")) { collectForm(id) }
                }
            }
        }
    }
    private fun independentBuildingForm() {
        scaffold(text("إنشاء برج مستقل", "Create independent building"))
        label(text("حسابات وصلاحيات مستقلة ورصيد يبدأ من صفر", "Independent permissions and accounts starting at zero"))
        val name = input(text("اسم البرج", "Building name")); val address = input(text("العنوان", "Address"))
        val country = input(text("رمز الدولة مثل EG أو GB", "Country code e.g. EG or GB")); val currency = input(text("العملة مثل EGP أو GBP", "Currency e.g. EGP or GBP"))
        val layout = input(text("اختياري، كل سطر: الدور: الشقق بفواصل", "Optional, each line: floor: comma-separated units"))
        button(text("إنشاء", "Create")) { try {
            val floors = JSONArray(); layout.text.toString().lines().filter { it.isNotBlank() }.forEach { line -> val parts = line.split(":", limit = 2); require(parts.size == 2); floors.put(JSONObject().put("floor", parts[0].trim().toInt()).put("units", JSONArray(parts[1].split(",").map { it.trim() }))) }
            call("buildings", "POST", JSONObject().put("name", name.text.toString()).put("address", address.text.toString()).put("country", country.text.toString()).put("currency", currency.text.toString()).put("floors", floors)) { raw -> val selected = JSONObject(raw).getJSONObject("organization").getString("id"); call("me") { meRaw -> val me = JSONObject(meRaw); member = me.getJSONObject("user"); organizations = me.getJSONArray("organizations"); org = selected; role = "admin"; home() } }
        } catch (e: Exception) { error(e) } }
        button(text("إلغاء", "Cancel")) { home() }
    }
    private fun treasuryScreen() {
        scaffold(text("الخزائن والحسابات", "Treasury")); button(text("الرئيسية", "Home")) { home() }
        call("treasury") { raw -> val accounts = JSONArray(raw); for (i in 0 until accounts.length()) { val a = accounts.getJSONObject(i); label(a.getString("name") + " · " + a.getString("balance"), 20) }
            button(text("إضافة حساب برصيد صفر", "Add account with zero balance")) { treasuryAccountForm() }
            if (accounts.length() > 1) button(text("تحويل بين حسابات البرج", "Transfer within building")) { treasuryTransferForm(accounts) }
        }
    }
    private fun accountPicker(accounts: JSONArray): Spinner { val spinner = Spinner(this); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until accounts.length()).map { val a = accounts.getJSONObject(it); a.getString("name") + " · " + a.getString("balance") }); screen.addView(spinner); return spinner }
    private fun treasuryAccountForm() { scaffold(text("إضافة خزنة أو بنك أو محفظة", "Add treasury account")); val name = input(text("الاسم", "Name")); val code = input(text("كود فريد يبدأ بـ11", "Unique code starting with 11")); val kind = Spinner(this); kind.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("cash", "bank", "wallet")); screen.addView(kind); button(text("حفظ", "Save")) { call("treasury", "POST", JSONObject().put("name", name.text.toString()).put("code", code.text.toString()).put("treasury_kind", kind.selectedItem.toString())) { treasuryScreen() } } }
    private fun treasuryTransferForm(accounts: JSONArray) {
        scaffold(text("تحويل داخل البرج فقط", "Transfer within this building only")); label(text("من الحساب", "From account")); val source = accountPicker(accounts); label(text("إلى الحساب", "To account")); val target = accountPicker(accounts); val amount = input(text("المبلغ", "Amount")); val date = input(text("التاريخ YYYY-MM-DD", "Date YYYY-MM-DD")).apply { setText(java.time.LocalDate.now().toString()) }; val reference = java.util.UUID.randomUUID().toString()
        label(text("التحويل لا يسجل إيرادًا أو مصروفًا جديدًا", "Transfer does not recognize income or expense"))
        button(text("تأكيد التحويل", "Confirm transfer")) { call("treasury/transfers", "POST", JSONObject().put("source_id", accounts.getJSONObject(source.selectedItemPosition).getString("id")).put("target_id", accounts.getJSONObject(target.selectedItemPosition).getString("id")).put("amount", amount.text.toString()).put("transferred_on", date.text.toString()).put("reference", reference)) { treasuryScreen() } }
    }
    private fun expensePaymentForm(id: String) {
        scaffold(text("دفع المصروف المعتمد", "Pay approved expense")); label(text("الدفع يخفض النقدية والمتبقي. الاعتماد وحده لا يخفض النقدية.", "Payment reduces cash and unpaid expense. Approval alone does not reduce cash."))
        call("treasury") { raw -> val accounts = JSONArray(raw); if (accounts.length() == 0) { label(text("لا يوجد حساب مالي متاح", "No available treasury account")); return@call }; val selected = accountPicker(accounts); val amount = input(text("المبلغ", "Amount")); val date = input(text("التاريخ YYYY-MM-DD", "Date YYYY-MM-DD")).apply { setText(java.time.LocalDate.now().toString()) }; val reference = java.util.UUID.randomUUID().toString()
            button(text("تأكيد الدفع", "Confirm payment")) { call("expenses/$id/pay", "POST", JSONObject().put("account_id", accounts.getJSONObject(selected.selectedItemPosition).getString("id")).put("amount", amount.text.toString()).put("paid_on", date.text.toString()).put("reference", reference)) { listing("expenses") } }
        }
    }
    private fun propertyForm() { scaffold(text("عقار جديد", "New property")); val name = input(text("الاسم", "Name")); val address = input(text("العنوان", "Address")); button(text("حفظ", "Save")) { call("properties", "POST", JSONObject().put("name", name.text.toString()).put("address", address.text.toString())) { listing("properties") } }; button(text("إلغاء", "Cancel")) { listing("properties") } }
    private fun ticketForm() {
        scaffold(text("طلب صيانة", "Maintenance request")); val heading = input(text("عنوان المشكلة", "Issue title")); val description = input(text("التفاصيل", "Details")); val spinner = Spinner(this); screen.addView(spinner)
        call("units") { raw -> val units = JSONArray(raw); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until units.length()).map { units.getJSONObject(it).getString("name") }); button(text("إرسال", "Submit")) { val payload = JSONObject().put("title", heading.text.toString()).put("description", description.text.toString()); if (units.length() > 0) payload.put("unit_id", units.getJSONObject(spinner.selectedItemPosition).getString("id")); call("tickets", "POST", payload) { listing("tickets") } } }
        button(text("إلغاء", "Cancel")) { listing("tickets") }
    }
    private fun collectForm(id: String) { scaffold(text("تسجيل تحصيل", "Record receipt")); val amount = input(text("المبلغ", "Amount")).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }; val reference = input(text("مرجع فريد", "Unique reference")); button(text("حفظ", "Save")) { call("invoices/$id/collect", "POST", JSONObject().put("amount", amount.text.toString()).put("reference", reference.text.toString())) { listing("invoices") } }; button(text("إلغاء", "Cancel")) { listing("invoices") } }

    private fun googleLogin() {
        val bytes = ByteArray(32); SecureRandom().nextBytes(bytes)
        val verifier = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val proof = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        getPreferences(0).edit().putString("oauth_verifier", verifier).apply()
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base + "/api/auth/google/start?client=android&challenge=" + proof))) } catch (e: Exception) { error(e) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleAuth(intent); if ((intent.hasExtra("open_notifications") || intent.hasExtra("notification_id")) && token.isNotEmpty()) listing("notifications") }
    private fun handleAuth(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "towerflow" || uri.host != "auth") return
        val code = uri.getQueryParameter("code") ?: return
        val verifier = getPreferences(0).getString("oauth_verifier", null) ?: return
        getPreferences(0).edit().remove("oauth_verifier").apply()
        call("auth/mobile/exchange", "POST", JSONObject().put("code", code).put("verifier", verifier)) { raw -> token = JSONObject(raw).getString("token"); loadMe() }
    }
    private fun profile() {
        scaffold(text("ملفي الشخصي", "My profile")); avatar = if (member.isNull("avatar")) null else member.optString("avatar").ifEmpty { null }
        avatar?.let { value -> try { val bytes = Base64.decode(value.substringAfter(','), Base64.DEFAULT); val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size); screen.addView(ImageView(this).apply { setImageBitmap(bitmap); adjustViewBounds = true }, LinearLayout.LayoutParams(dp(130), dp(130))) } catch (_: Exception) {} }
        label(member.optString("email"))
        val name = input(text("الاسم الكامل", "Full name")).apply { setText(member.optString("name")) }
        val phone = input(text("رقم الهاتف", "Phone")).apply { setText(member.optString("phone")); inputType = InputType.TYPE_CLASS_PHONE }
        val bio = input(text("نبذة عنك", "About you")).apply { setText(member.optString("bio")); setSingleLine(false); maxLines = 5 }
        button(text("حفظ البيانات", "Save details")) { call("me/profile", "PATCH", JSONObject().put("name", name.text.toString()).put("phone", phone.text.toString()).put("bio", bio.text.toString()).put("locale", if (arabic) "ar" else "en")) { raw -> member = JSONObject(raw).getJSONObject("user"); profile() } }
        button(text("رفع صورة", "Upload photo")) { try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 201) } catch (e: Exception) { error(e) } }
        button(text("التقاط بالكاميرا", "Take photo")) { try { startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 202) } catch (e: Exception) { error(e) } }
        button(text("إزالة الصورة", "Remove photo")) { call("me/profile", "PATCH", JSONObject().put("avatar", JSONObject.NULL)) { raw -> member = JSONObject(raw).getJSONObject("user"); profile() } }
        label(text("ربط جوجل بحساب بريد قائم متاح من الملف الشخصي في موقع TowerFlow.", "Link Google to an existing email account from your profile on the TowerFlow website."))
        button(text("الرئيسية", "Home")) { home() }
    }
    @Deprecated("Platform activity result compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || requestCode !in listOf(201, 202)) return
        worker.execute { try {
            val bitmap = if (requestCode == 202) { @Suppress("DEPRECATION") (data?.extras?.get("data") as? Bitmap) } else {
                val uri = data?.data ?: return@execute
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
                val options = BitmapFactory.Options().apply { inSampleSize = 1; while (bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) inSampleSize *= 2 }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            } ?: throw IllegalArgumentException("Unable to read photo")
            val side = minOf(bitmap.width, bitmap.height)
            val crop = Bitmap.createBitmap(bitmap, (bitmap.width-side)/2, (bitmap.height-side)/2, side, side)
            val scaled = Bitmap.createScaledBitmap(crop, minOf(side, 512), minOf(side, 512), true)
            val bytes = ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
            val image = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            runOnUiThread { AlertDialog.Builder(this).setTitle(text("حفظ الصورة الشخصية؟", "Save profile photo?" )).setPositiveButton(text("حفظ", "Save")) { _, _ -> call("me/profile", "PATCH", JSONObject().put("avatar", image)) { raw -> member = JSONObject(raw).getJSONObject("user"); profile() } }.setNegativeButton(text("إلغاء", "Cancel"), null).show() }
        } catch (e: Exception) { runOnUiThread { error(e) } } }
    }
    private val forms = mapOf(
        "properties" to listOf("name", "address"), "units" to listOf("property_id", "name", "floor", "area", "status"),
        "parties" to listOf("name", "kind", "email", "phone"), "leases" to listOf("unit_id", "party_id", "start_date", "end_date", "rent", "status"),
        "ownerships" to listOf("unit_id", "party_id", "share"), "accounts" to listOf("code", "name", "kind"),
        "invoices" to listOf("number", "party_id", "unit_id", "description", "amount", "due_date"), "expenses" to listOf("description", "amount"),
        "assets" to listOf("property_id", "name", "serial", "next_service"), "technicians" to listOf("name", "specialty", "phone"),
        "tickets" to listOf("title", "description", "unit_id", "asset_id", "priority", "status", "technician_id"), "announcements" to listOf("title", "body"))
    private val fieldNames = mapOf("name" to "الاسم", "address" to "العنوان", "property_id" to "العقار", "unit_id" to "الوحدة", "party_id" to "المالك / المستأجر", "floor" to "الطابق", "area" to "المساحة", "status" to "الحالة", "email" to "البريد", "phone" to "الهاتف", "kind" to "النوع", "start_date" to "بداية العقد YYYY-MM-DD", "end_date" to "نهاية العقد YYYY-MM-DD", "rent" to "الإيجار", "share" to "الملكية %", "code" to "الكود", "number" to "رقم الفاتورة", "description" to "الوصف", "amount" to "المبلغ", "due_date" to "الاستحقاق YYYY-MM-DD", "title" to "العنوان", "body" to "المحتوى", "priority" to "الأولوية", "asset_id" to "الأصل", "technician_id" to "الفني", "specialty" to "التخصص", "serial" to "الرقم التسلسلي", "next_service" to "الصيانة القادمة YYYY-MM-DD")
    private fun recordForm(entity: String, record: JSONObject? = null) {
        scaffold(title(entity)); val values = linkedMapOf<String, () -> String>()
        val keys = forms[entity] ?: return
        fun addField(index: Int) {
            if (index >= keys.size) {
                button(text("حفظ", "Save")) { val payload = JSONObject(); values.forEach { (key, getter) -> val value = getter(); if (value.isNotBlank()) payload.put(key, value) }; call(entity + (record?.let { "/" + it.getString("id") } ?: ""), if (record == null) "POST" else "PATCH", payload) { listing(entity) } }
                button(text("إلغاء", "Cancel")) { listing(entity) }; return
            }
            val key = keys[index]; val initial = if (record?.isNull(key) == false) record.optString(key) else ""
            val relation = mapOf("property_id" to "properties", "unit_id" to "units", "party_id" to "parties", "asset_id" to "assets", "technician_id" to "technicians")[key]
            val options = when { key == "kind" && entity == "parties" -> listOf("tenant", "owner", "vendor"); key == "kind" && entity == "accounts" -> listOf("asset", "liability", "equity", "income", "expense"); key == "status" && entity == "units" -> listOf("vacant", "occupied", "maintenance"); key == "status" && entity == "leases" -> listOf("active", "ended"); key == "status" -> listOf("open", "assigned", "in_progress", "resolved", "closed"); key == "priority" -> listOf("normal", "low", "urgent"); else -> emptyList() }
            if (relation != null || options.isNotEmpty()) {
                label(text(fieldNames[key] ?: key, key.replace('_', ' ')))
                fun select(items: List<Pair<String, String>>) { val spinner = Spinner(this); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.second }); spinner.setSelection(items.indexOfFirst { it.first == initial }.coerceAtLeast(0)); screen.addView(spinner); values[key] = { items.getOrNull(spinner.selectedItemPosition)?.first ?: "" }; addField(index+1) }
                if (relation != null) call(relation) { raw -> val rows = JSONArray(raw); select(listOf("" to "—") + (0 until rows.length()).map { val row = rows.getJSONObject(it); row.getString("id") to row.optString("name") }) } else select(options.map { it to (if (arabic) when (it) { "occupied" -> "متأجرة"; "vacant" -> "غير متأجرة"; "maintenance" -> "تحت الصيانة"; else -> it } else if (it == "occupied") "Rented" else if (it == "vacant") "Not rented" else it) })
            } else {
                val control = input(text(fieldNames[key] ?: key, key.replace('_', ' '))).apply { setText(initial); if (key in listOf("amount", "rent", "area", "share", "floor")) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED }
                values[key] = { control.text.toString() }; addField(index+1)
            }
        }; addField(0)
    }

    private fun enablePush() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 301) } else registerPush(true)
    }
    private fun registerPush(showResult: Boolean = false) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { device -> getSharedPreferences("push_device", MODE_PRIVATE).edit().putString("token", device).apply(); if (token.isNotEmpty()) call("push/devices", "POST", JSONObject().put("token", device)) { if (showResult) Toast.makeText(this, text("تم تسجيل الهاتف للإشعارات. تأكد من السماح بالإشعارات في إعدادات الهاتف.", "Phone registered. Ensure notifications are allowed in phone settings."), Toast.LENGTH_LONG).show() } }.addOnFailureListener { if (showResult) error(IllegalStateException(text("تعذر تسجيل الهاتف. تحقق من الإنترنت وخدمات Google Play.", "Could not register phone. Check connectivity and Google Play services."))) }
    }
    @Deprecated("Platform permission callback compatibility")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 301 && grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) registerPush(true) }
    private fun disablePushAndLogout() {
        val device = getSharedPreferences("push_device", MODE_PRIVATE).getString("token", null)
        fun finishLogout() { call("auth/logout", "POST", JSONObject()) { token = ""; org = ""; getSystemService(NotificationManager::class.java).cancelAll(); login() } }
        if (device != null) call("push/devices/remove", "POST", JSONObject().put("token", device)) { finishLogout() } else finishLogout()
    }
    override fun onDestroy() { super.onDestroy(); worker.shutdownNow() }
}
