package com.ad.towerflow

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
    private var base = ""
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
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); base = getPreferences(0).getString("base", "") ?: ""; login() }
    private fun scaffold(heading: String) {
        val scroll = ScrollView(this)
        screen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(36), dp(22), dp(24)); layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR; setBackgroundColor(Color.parseColor(if (dark) "#102331" else "#F4F7F9")) }
        scroll.addView(screen); setContentView(scroll)
        label("AD · TowerFlow", 26); label(heading, 21)
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
        val url = input(text("عنوان المنصة HTTPS", "Platform HTTPS URL")).apply { setText(base); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }
        val name = if (register) input(text("الاسم", "Name")) else null
        val organization = if (register) input(text("المؤسسة", "Organization")) else null
        val email = input(text("البريد الإلكتروني", "Email")).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val pass = input(text("كلمة المرور · 12 حرفًا على الأقل", "Password · minimum 12 characters"), true)
        button(text("متابعة", "Continue")) {
            try {
                val uri = URI(url.text.toString().trim().trimEnd('/'))
                require(uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) { "A valid HTTPS origin is required" }
                base = uri.toString().trimEnd('/'); getPreferences(0).edit().putString("base", base).apply()
                val payload = JSONObject().put("email", email.text.toString()).put("password", pass.text.toString())
                if (register) payload.put("name", name?.text.toString()).put("organization", organization?.text.toString())
                call("auth/" + if (register) "register" else "login", "POST", payload) { raw -> token = JSONObject(raw).getString("token"); pass.text.clear(); loadMe() }
            } catch (e: Exception) { error(e) }
        }
        button(text(if (register) "لدي حساب" else "إنشاء حساب ومؤسسة", if (register) "I have an account" else "Create account & workspace")) { login(!register) }
    }
    private fun loadMe() { call("me") { raw -> organizations = JSONObject(raw).getJSONArray("organizations"); if (organizations.length() > 0) { val o = organizations.getJSONObject(0); org = o.getString("id"); role = o.getString("role") }; home() } }
    private fun home() {
        scaffold(text("إدارة عقاراتك بوضوح", "Your properties. In perspective."))
        if (organizations.length() > 0) {
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until organizations.length()).map { organizations.getJSONObject(it).getString("name") })
            spinner.setSelection((0 until organizations.length()).firstOrNull { organizations.getJSONObject(it).getString("id") == org } ?: 0)
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {} ; override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { val o = organizations.getJSONObject(position); org = o.getString("id"); role = o.getString("role") } }
            screen.addView(spinner)
        }
        label(text("بيانات حقيقية من مؤسستك. تُطبق الصلاحيات على الخادم.", "Live workspace data. Permissions are enforced on the server."))
        names.keys.filter { role != "resident" || it in listOf("units", "leases", "invoices", "tickets", "announcements", "notifications") }.forEach { key -> button(title(key)) { listing(key) } }
        button(text("تسجيل الخروج", "Sign out")) { call("auth/logout", "POST", JSONObject()) { token = ""; org = ""; login() } }
    }
    private fun listing(entity: String) {
        scaffold(title(entity)); button(text("الرئيسية", "Home")) { home() }
        if (entity == "properties" && role in listOf("admin", "manager")) button(text("إضافة عقار", "Add property")) { propertyForm() }
        if (entity == "tickets") button(text("طلب صيانة", "Request maintenance")) { ticketForm() }
        call(entity) { raw ->
            val records = JSONArray(raw)
            if (records.length() == 0) label(text("لا توجد سجلات بعد", "No records yet"))
            for (i in 0 until records.length()) {
                val row = records.getJSONObject(i)
                val heading = listOf("name", "title", "number", "description", "message", "entity").map { row.optString(it) }.firstOrNull { it.isNotEmpty() } ?: "Record"
                label(heading, 19)
                val details = listOf("status", "amount", "due_date", "priority", "phone", "address", "body", "debit", "credit", "action").filter { row.has(it) && !row.isNull(it) }.joinToString(" · ") { "$it: ${row.optString(it)}" }
                label(details)
                if (entity == "invoices" && role in listOf("admin", "manager", "accountant")) {
                    val id = row.getString("id")
                    if (row.optString("status") == "draft") button(text("إصدار وترحيل", "Issue & post")) { call("invoices/$id/issue", "POST", JSONObject()) { listing(entity) } }
                    if (row.optString("status") == "issued") button(text("تحصيل", "Collect")) { collectForm(id) }
                }
            }
        }
    }
    private fun propertyForm() { scaffold(text("عقار جديد", "New property")); val name = input(text("الاسم", "Name")); val address = input(text("العنوان", "Address")); button(text("حفظ", "Save")) { call("properties", "POST", JSONObject().put("name", name.text.toString()).put("address", address.text.toString())) { listing("properties") } }; button(text("إلغاء", "Cancel")) { listing("properties") } }
    private fun ticketForm() {
        scaffold(text("طلب صيانة", "Maintenance request")); val heading = input(text("عنوان المشكلة", "Issue title")); val description = input(text("التفاصيل", "Details")); val spinner = Spinner(this); screen.addView(spinner)
        call("units") { raw -> val units = JSONArray(raw); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until units.length()).map { units.getJSONObject(it).getString("name") }); button(text("إرسال", "Submit")) { val payload = JSONObject().put("title", heading.text.toString()).put("description", description.text.toString()); if (units.length() > 0) payload.put("unit_id", units.getJSONObject(spinner.selectedItemPosition).getString("id")); call("tickets", "POST", payload) { listing("tickets") } } }
        button(text("إلغاء", "Cancel")) { listing("tickets") }
    }
    private fun collectForm(id: String) { scaffold(text("تسجيل تحصيل", "Record receipt")); val amount = input(text("المبلغ", "Amount")).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }; val reference = input(text("مرجع فريد", "Unique reference")); button(text("حفظ", "Save")) { call("invoices/$id/collect", "POST", JSONObject().put("amount", amount.text.toString()).put("reference", reference.text.toString())) { listing("invoices") } }; button(text("إلغاء", "Cancel")) { listing("invoices") } }
    override fun onDestroy() { super.onDestroy(); worker.shutdownNow() }
}
