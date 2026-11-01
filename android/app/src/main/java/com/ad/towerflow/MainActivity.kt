package com.ad.towerflow

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors

/**
 * TowerFlow Android Native Experience
 * Executive, modern Material 3 styling with zero webviews or third-party UI dependencies.
 */
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var token = ""
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
        "properties" to Pair("العقارات والأبراج", "Properties"),
        "units" to Pair("الوحدات والشقق", "Units"),
        "parties" to Pair("الملاك والمستأجرون", "Residents & Owners"),
        "leases" to Pair("عقود الإيجار", "Leases"),
        "ownerships" to Pair("سجلات الملكية", "Ownerships"),
        "invoices" to Pair("الفواتير والمطالبات", "Invoices"),
        "receipts" to Pair("سندات التحصيل", "Receipts"),
        "expenses" to Pair("المصروفات التشغيلية", "Expenses"),
        "journals" to Pair("القيود المحاسبية", "Journals"),
        "accounts" to Pair("دليل الحسابات", "Chart of Accounts"),
        "tickets" to Pair("بلاغات الصيانة", "Maintenance"),
        "assets" to Pair("الأصول والمرافق", "Assets"),
        "technicians" to Pair("الفنيون المعتمدون", "Technicians"),
        "announcements" to Pair("الإعلانات والتعميمات", "Announcements"),
        "notifications" to Pair("الإشعارات والتنبيهات", "Notifications"),
        "trial_balance" to Pair("ميزان المراجعة", "Trial Balance"),
        "audit_logs" to Pair("سجل التدقيق المالي", "Audit Log"),
        "payment_proofs" to Pair("إثباتات السداد والتحويل", "Payment Proofs"),
        "fines" to Pair("الغرامات والمخالفات", "Fines & Penalties"),
        "credit_balances" to Pair("الأرصدة الدائنة للسكان", "Credit Balances"),
        "money_accounts" to Pair("الخزائن والحسابات البنكية", "Accounts & Safes"),
        "support_tickets" to Pair("الدعم الفني والمساندة", "Technical Support"),
        "expense_distributions" to Pair("توزيع المصروفات المشتركة", "Expense Distributions"),
        "vendor_contracts" to Pair("عقود الموردين والشركات", "Vendor Contracts"),
        "budgets" to Pair("الميزانيات التقديرية", "Annual Budgets"),
        "ownership_type" to Pair("نوع الحيازة والملكية", "Ownership Type"),
        "owned" to Pair("وحدة ملك (تمليك)", "Owned"),
        "rental" to Pair("إيجار", "Rental")
    )

    private fun text(ar: String, en: String) = if (arabic) ar else en
    private fun title(k: String) = names[k]?.let { if (arabic) it.first else it.second } ?: k
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    // --- Modern Design Helpers ---
    private fun roundedDrawable(bgColor: Int, cornerRadiusDp: Float = 14f, strokeColor: Int? = null, strokeWidthDp: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerRadiusDp.toInt()).toFloat()
            setColor(bgColor)
            if (strokeColor != null) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        base = getPreferences(0).getString("base", "https://towerflow.vercel.app") ?: "https://towerflow.vercel.app"
        login()
        handleAuth(intent)
        pendingNotifications = intent?.extras?.getBoolean("open_notifications", false) == true || intent?.hasExtra("notification_id") == true
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("towerflow_residents", "TowerFlow · رسائل السكان", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun scaffold(heading: String, showBack: Boolean = false, onBack: (() -> Unit)? = null) {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor(if (dark) "#090A0F" else "#F8FAFC"))
        }

        screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(28))
            layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        scroll.addView(screen)
        setContentView(scroll)

        // Modern Header TopBar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(14))
        }

        if (showBack && onBack != null) {
            val backBtn = Button(this).apply {
                text = if (arabic) "‹ عودة" else "‹ Back"
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#4F46E5"))
                textSize = 14f
                background = roundedDrawable(Color.TRANSPARENT, 10f)
                setOnClickListener { onBack() }
            }
            topBar.addView(backBtn)
        }

        val brandCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setPadding(dp(4), 0, dp(4), 0)
        }
        brandCol.addView(TextView(this).apply {
            text = "TowerFlow"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        })
        brandCol.addView(TextView(this).apply {
            text = heading
            textSize = 13f
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
        })
        topBar.addView(brandCol)

        // Language & Theme Toggle Buttons
        val langBtn = Button(this).apply {
            text = if (arabic) "EN" else "عربي"
            textSize = 12f
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            setOnClickListener {
                arabic = !arabic
                if (token.isEmpty()) login() else home()
            }
        }
        val langParams = LinearLayout.LayoutParams(dp(54), dp(38)).apply { marginEnd = dp(6) }
        topBar.addView(langBtn, langParams)

        val themeBtn = Button(this).apply {
            text = if (dark) "☀" else "☾"
            textSize = 14f
            setTextColor(Color.parseColor(if (dark) "#FCD34D" else "#1E293B"))
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            setOnClickListener {
                dark = !dark
                if (token.isEmpty()) login() else home()
            }
        }
        topBar.addView(themeBtn, LinearLayout.LayoutParams(dp(42), dp(38)))
        screen.addView(topBar)
    }

    private fun card(fn: LinearLayout.() -> Unit): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedDrawable(
                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                16f,
                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
            )
            val lp = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            layoutParams = lp
        }
        layout.fn()
        screen.addView(layout)
        return layout
    }

    private fun label(value: String, size: Int = 14, isBold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        if (isBold) setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        setPadding(0, dp(4), 0, dp(4))
        screen.addView(this)
    }

    private fun sectionHeader(titleText: String) {
        val tv = TextView(this).apply {
            text = titleText
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            setPadding(dp(4), dp(16), dp(4), dp(8))
        }
        screen.addView(tv)
    }

    private fun input(hintText: String, password: Boolean = false): EditText = EditText(this).apply {
        this.hint = hintText
        setSingleLine()
        inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
        background = roundedDrawable(
            if (dark) Color.parseColor("#0F172A") else Color.WHITE,
            12f,
            if (dark) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")
        )
        setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(4)
            bottomMargin = dp(12)
        }
        screen.addView(this, lp)
    }

    private fun primaryButton(value: String, fn: () -> Unit) {
        val btn = Button(this).apply {
            text = value
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            background = roundedDrawable(Color.parseColor("#4F46E5"), 12f)
            setOnClickListener { fn() }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(8)
            bottomMargin = dp(4)
        }
        screen.addView(btn, lp)
    }

    private fun outlineButton(value: String, fn: () -> Unit) {
        val btn = Button(this).apply {
            text = value
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            background = roundedDrawable(
                Color.TRANSPARENT,
                12f,
                Color.parseColor(if (dark) "#334155" else "#CBD5E1")
            )
            setOnClickListener { fn() }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(44)).apply {
            topMargin = dp(6)
            bottomMargin = dp(4)
        }
        screen.addView(btn, lp)
    }

    private fun navTile(key: String, titleText: String, subtitleText: String = "", badgeCount: Int = 0) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedDrawable(
                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                12f,
                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
            )
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener { listing(key) }
        }

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        textCol.addView(TextView(this).apply {
            text = titleText
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        })
        if (subtitleText.isNotEmpty()) {
            textCol.addView(TextView(this).apply {
                text = subtitleText
                textSize = 11f
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, dp(2), 0, 0)
            })
        }
        row.addView(textCol)

        if (badgeCount > 0) {
            val badge = TextView(this).apply {
                text = badgeCount.toString()
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#B45309"))
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = roundedDrawable(Color.parseColor("#FEF3C7"), 99f, Color.parseColor("#FDE68A"))
            }
            row.addView(badge)
        }

        val chevron = TextView(this).apply {
            text = if (arabic) "‹" else "›"
            textSize = 18f
            setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            setPadding(dp(6), 0, dp(2), 0)
        }
        row.addView(chevron)

        screen.addView(row)
    }

    private fun error(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle(text("تنبيه", "Notice"))
            .setMessage(e.message)
            .setPositiveButton(text("حسناً", "OK"), null)
            .show()
    }

    private fun call(path: String, method: String = "GET", data: JSONObject? = null, done: (String) -> Unit) {
        worker.execute {
            try {
                val connection = URL(base + "/api/" + path + (if (path.contains('?')) "&" else "?") + "org=" + org).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-TowerFlow-Client", "android")
                    if (token.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")
                    if (data != null) {
                        connection.doOutput = true
                        connection.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val code = connection.responseCode
                    val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                    if (code !in 200..299) throw IllegalStateException(JSONObject(raw).optString("error", "HTTP $code"))
                    runOnUiThread { try { done(raw) } catch (e: Exception) { error(e) } }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread { error(e) }
            }
        }
    }

    private fun login(register: Boolean = false) {
        scaffold(text(if (register) "إنشاء حساب جديد" else "تسجيل الدخول", if (register) "Create Account" else "Sign In"))

        card {
            val titleView = TextView(this@MainActivity).apply {
                text = text(if (register) "انضم إلى TowerFlow" else "مرحبًا بعودتك", if (register) "Join TowerFlow" else "Welcome Back")
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            }
            addView(titleView)

            val subtitleView = TextView(this@MainActivity).apply {
                text = text("منصة إدارة الأبراج والعقارات الذكية", "Smart Property & Tower Management Platform")
                textSize = 12f
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, dp(4), 0, dp(16))
            }
            addView(subtitleView)
        }

        val name = if (register) input(text("الاسم الكامل", "Full Name")) else null
        val organization = if (register) input(text("اسم المؤسسة أو البرج", "Workspace or Tower Name")) else null
        val email = input(text("البريد الإلكتروني", "Email Address")).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val pass = input(text("كلمة المرور (12 حرفًا على الأقل)", "Password (min 12 chars)"), true)

        primaryButton(text("متابعة", "Continue")) {
            try {
                val uri = URI(base)
                require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty()) { "A valid HTTP/HTTPS origin is required" }
                base = uri.toString().trimEnd('/')
                getPreferences(0).edit().putString("base", base).apply()
                val payload = JSONObject().put("email", email.text.toString().trim()).put("password", pass.text.toString())
                if (register) payload.put("name", name?.text.toString().trim()).put("organization", organization?.text.toString().trim())
                call("auth/" + if (register) "register" else "login", "POST", payload) { raw ->
                    token = JSONObject(raw).getString("token")
                    pass.text.clear()
                    loadMe()
                }
            } catch (e: Exception) {
                error(e)
            }
        }

        outlineButton(text("المتابعة باستخدام Google", "Continue with Google")) { googleLogin() }
        outlineButton(text(if (register) "لدي حساب بالفعل" else "إنشاء حساب ومؤسسة جديدة", if (register) "Already have an account" else "Create new account")) {
            login(!register)
        }

        // Server URL Configuration
        outlineButton(text("⚙ رابط الخادم: ", "⚙ Server: ") + base) {
            val urlInput = EditText(this).apply {
                setText(base)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }
            AlertDialog.Builder(this)
                .setTitle(text("إعداد رابط الخادم", "Server URL Configuration"))
                .setMessage(text("أدخل رابط خادم TowerFlow (مثال: https://towerflow.vercel.app أو http://10.0.2.2:3000)", "Enter TowerFlow backend origin"))
                .setView(urlInput)
                .setPositiveButton(text("حفظ وتحديث", "Save")) { _, _ ->
                    var newUrl = urlInput.text.toString().trim().trimEnd('/')
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        newUrl = "https://$newUrl"
                    }
                    base = newUrl
                    getPreferences(0).edit().putString("base", base).apply()
                    login(register)
                }
                .setNegativeButton(text("إلغاء", "Cancel"), null)
                .show()
        }
    }

    private fun loadMe() {
        call("me") { raw ->
            member = JSONObject(raw).getJSONObject("user")
            organizations = JSONObject(raw).getJSONArray("organizations")
            if (organizations.length() > 0) {
                val o = organizations.getJSONObject(0)
                org = o.getString("id")
                role = o.getString("role")
            }
            registerPush()
            if (pendingNotifications) {
                pendingNotifications = false
                listing("notifications")
            } else {
                home()
            }
        }
    }

    private fun home() {
        scaffold(text("لوحة التحكم", "Dashboard"))

        // Active Workspace Selector
        if (organizations.length() > 0) {
            card {
                val h = TextView(this@MainActivity).apply {
                    text = text("المؤسسة / البرج النشط", "Active Workspace / Tower")
                    textSize = 11f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                }
                addView(h)

                val spinner = Spinner(this@MainActivity).apply {
                    adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, (0 until organizations.length()).map { organizations.getJSONObject(it).getString("name") })
                    setSelection((0 until organizations.length()).firstOrNull { organizations.getJSONObject(it).getString("id") == org } ?: 0)
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val o = organizations.getJSONObject(position)
                            org = o.getString("id")
                            role = o.getString("role")
                        }
                    }
                }
                addView(spinner)
            }
        }

        // Executive Hero Greeting Card
        card {
            val greeting = TextView(this@MainActivity).apply {
                text = text("مرحبًا، ", "Hello, ") + member.optString("name", "User")
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            }
            addView(greeting)

            val roleBadge = TextView(this@MainActivity).apply {
                val roleName = when (role) {
                    "admin" -> text("مدير النظام (Admin)", "System Admin")
                    "manager" -> text("مدير البرج (Manager)", "Building Manager")
                    "accountant" -> text("المحاسب (Accountant)", "Accountant")
                    "resident" -> text("ساكن / مستأجر (Resident)", "Resident")
                    else -> role
                }
                text = roleName
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#4F46E5"))
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = roundedDrawable(Color.parseColor(if (dark) "#1E1B4B" else "#EEF2FF"), 99f)
            }
            addView(roleBadge)
        }

        // Live KPI Grid for Managers
        if (role != "resident") {
            call("dashboard") { raw ->
                try {
                    val dash = JSONObject(raw)
                    val unitsObj = dash.optJSONObject("units") ?: JSONObject()
                    val totalUnits = unitsObj.optInt("total", 0)
                    val occupiedUnits = unitsObj.optInt("occupied", 0)
                    val vacantUnits = unitsObj.optInt("vacant", 0)

                    val kpiRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        weightSum = 2f
                    }

                    fun makeKpi(kpiTitle: String, kpiValue: String, kpiSub: String, isFirst: Boolean): LinearLayout {
                        return LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(14), dp(12), dp(14), dp(12))
                            background = roundedDrawable(
                                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                                14f,
                                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
                            )
                            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                                if (isFirst) marginEnd = dp(4) else marginStart = dp(4)
                            }
                            addView(TextView(this@MainActivity).apply {
                                text = kpiTitle
                                textSize = 11f
                                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                                setTypeface(typeface, Typeface.BOLD)
                            })
                            addView(TextView(this@MainActivity).apply {
                                text = kpiValue
                                textSize = 20f
                                setTextColor(Color.parseColor("#4F46E5"))
                                setTypeface(typeface, Typeface.BOLD)
                                setPadding(0, dp(2), 0, dp(2))
                            })
                            addView(TextView(this@MainActivity).apply {
                                text = kpiSub
                                textSize = 10f
                                setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                            })
                        }
                    }

                    val ownedUnits = unitsObj.optInt("owned", 0)
                    val rentedUnits = unitsObj.optInt("rented", occupiedUnits)
                    val maintUnits = unitsObj.optInt("maintenance", 0)
                    val pct = if (totalUnits > 0) ((rentedUnits + ownedUnits) * 100 / totalUnits) else 0
                    kpiRow.addView(makeKpi(text("إجمالي الشقق", "Total Units"), totalUnits.toString(), text("$rentedUnits مؤجرة · $ownedUnits ملك", "$rentedUnits rented · $ownedUnits owned"), true))
                    kpiRow.addView(makeKpi(text("نسبة الإشغال", "Occupancy"), "$pct%", text("$vacantUnits شاغرة · $maintUnits صيانة", "$vacantUnits vacant"), false))
                    screen.addView(kpiRow)

                    val kpiRow2 = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        weightSum = 2f
                        setPadding(0, dp(8), 0, 0)
                    }
                    kpiRow2.addView(makeKpi(text("وحدات تمليك (ملك)", "Owned Units"), ownedUnits.toString(), text("ملاك مقيمين", "Owner units"), true))
                    kpiRow2.addView(makeKpi(text("وحدات مؤجرة", "Rented Units"), rentedUnits.toString(), text("عقود إيجار نشطة", "Active leases"), false))
                    screen.addView(kpiRow2)
                } catch (_: Exception) {}
            }
        }

        // Quick Actions Row
        sectionHeader(text("إجراءات سريعة", "Quick Actions"))
        val quickGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        val q1 = Button(this).apply {
            text = text(if (role == "resident") "📤 إثبات سداد" else "⚡ تحصيل", if (role == "resident") "Proof" else "Collect")
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            background = roundedDrawable(Color.parseColor("#4F46E5"), 12f)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { if (role == "resident") proofForm() else listing("invoices") }
        }
        quickGrid.addView(q1)

        val q2 = Button(this).apply {
            text = text("🔧 بلاغ صيانة", "Ticket")
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            background = roundedDrawable(
                Color.TRANSPARENT,
                12f,
                Color.parseColor(if (dark) "#334155" else "#CBD5E1")
            )
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) }
            setOnClickListener { ticketForm() }
        }
        quickGrid.addView(q2)
        screen.addView(quickGrid)

        // Categorized Sections
        if (role != "resident") {
            sectionHeader(text("🏢 إدارة العقار والوحدات", "Property & Units"))
            navTile("properties", title("properties"), text("سجل الأبراج والمباني وبياناتها", "Towers and buildings directory"))
            navTile("units", title("units"), text("حصر الشقق والمساحات وحالة الإشغال", "Apartments, areas and occupancy"))
            navTile("parties", title("parties"), text("سجل الملاك والمستأجرين وبيانات التواصل", "Owners and tenants registry"))
            navTile("leases", title("leases"), text("عقود الإيجار والمدد وتاريخ التجديد", "Lease contracts and renewals"))
            navTile("ownerships", title("ownerships"), text("حصص الملكية المشتركة في العقار", "Ownership shares"))

            sectionHeader(text("💳 المالية والتحصيل", "Finance & Cash Flow"))
            navTile("invoices", title("invoices"), text("الفواتير الشهرية والخدمات والمطالبات", "Issued invoices and dues"))
            navTile("receipts", title("receipts"), text("سندات القبض النقدية والبنكية", "Payment receipts ledger"))
            navTile("payment_proofs", title("payment_proofs"), text("مراجعة واعتماد إيصالات التحويل البنكي", "Review transfer slips"))
            navTile("credit_balances", title("credit_balances"), text("أرصدة السكان الدائنة المتاحة للتسوية", "Available resident credit balances"))
            navTile("fines", title("fines"), text("غرامات التأخير والمخالفات والإعفاءات", "Late penalties and waivers"))
            navTile("money_accounts", title("money_accounts"), text("الخزينة والحسابات البنكية وصناديق الصيانة", "Cash, bank and maintenance reserves"))
            navTile("expenses", title("expenses"), text("أوامر الصرف ومصروفات التشغيل", "Operational expenses"))
            navTile("journals", title("journals"), text("دفتر اليومية العامة والقيود المحاسبية", "General journal entries"))
            navTile("trial_balance", title("trial_balance"), text("ميزان المراجعة والأرصدة الختامية", "Trial balance ledger"))

            sectionHeader(text("🛠️ الصيانة والتشغيل", "Maintenance & Operations"))
            navTile("tickets", title("tickets"), text("طلبات وبلاغات الصيانة وتتبع إنجازها", "Maintenance tickets tracking"))
            navTile("assets", title("assets"), text("سجل المصاعد والمضخات ومعدات البرج", "Elevators, pumps and equipment"))
            navTile("technicians", title("technicians"), text("فنيو الصيانة والموردون المعتمدون", "Approved service technicians"))
            navTile("vendor_contracts", title("vendor_contracts"), text("عقود الصيانة الدورية والنظافة والأمن", "Periodic service contracts"))
            navTile("budgets", title("budgets"), text("الميزانيات التقديرية السنوية للبرج", "Annual budget targets"))
            navTile("expense_distributions", title("expense_distributions"), text("توزيع المصروفات الطارئة على الشقق", "Shared expense distributions"))

            sectionHeader(text("📢 الإدارة والدعم", "Management & Support"))
            navTile("announcements", title("announcements"), text("نشر التعميمات والإعلانات للسكان", "Resident announcements"))
            navTile("notifications", title("notifications"), text("التنبيهات والإشعارات اللحظية", "App notifications"))
            navTile("support_tickets", title("support_tickets"), text("تذاكر الدعم الفني لمشرفي النظام", "Technical support tickets"))
            navTile("audit_logs", title("audit_logs"), text("سجل الحركات الإدارية والمالية غير القابل للتعديل", "Immutable audit trail"))
        } else {
            sectionHeader(text("خدماتي", "My Services"))
            navTile("units", title("units"), text("بيانات وحدتي السكنية", "My Apartment Details"))
            navTile("leases", title("leases"), text("عقد الإيجار النشط", "My Active Lease"))
            navTile("invoices", title("invoices"), text("الفواتير والمستحقات الشهرية", "My Invoices & Dues"))
            navTile("payment_proofs", title("payment_proofs"), text("إرسال إثبات تحويل بنكي", "Submit Payment Slip"))
            navTile("credit_balances", title("credit_balances"), text("رصيدي الدائن المتاح", "My Credit Balance"))
            navTile("fines", title("fines"), text("الغرامات والمخالفات إن وجدت", "My Fines"))
            navTile("tickets", title("tickets"), text("طلب صيانة أو إصلاح", "Maintenance Requests"))
            navTile("announcements", title("announcements"), text("إعلانات وتعليمات إدارة البرج", "Building Announcements"))
            navTile("notifications", title("notifications"), text("التنبيهات المستلمة", "Notifications"))
            navTile("support_tickets", title("support_tickets"), text("تواصل مع الدعم الفني", "Customer Support"))
        }

        sectionHeader(text("حسابي", "My Profile"))
        outlineButton(text("الملف الشخصي والإعدادات", "My Profile & Settings")) { profile() }
        outlineButton(text("تسجيل الخروج", "Sign Out")) { disablePushAndLogout() }
    }

    private fun listing(entity: String) {
        scaffold(title(entity), true) { home() }

        // Action Toolbar
        if (entity in forms.keys && role in listOf("admin", "manager", "accountant", "technician")) {
            primaryButton(text("＋ إضافة سجل جديد", "＋ Add New Record")) { recordForm(entity) }
        }
        if (entity == "tickets") {
            primaryButton(text("🔧 طلب صيانة جديد", "🔧 New Maintenance Request")) { ticketForm() }
        }
        if (entity == "payment_proofs") {
            primaryButton(text("📤 إرسال إثبات سداد بنكي", "📤 Submit Payment Proof")) { proofForm() }
        }
        if (entity == "support_tickets") {
            primaryButton(text("💬 تذكرة دعم فني جديدة", "💬 New Support Ticket")) { supportTicketForm() }
        }

        call(entity) { raw ->
            val records = JSONArray(raw)
            if (records.length() == 0) {
                card {
                    addView(TextView(this@MainActivity).apply {
                        text = text("لا توجد سجلات بعد في هذا القسم", "No records found in this section")
                        textSize = 13f
                        setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    })
                }
            }

            for (i in 0 until records.length()) {
                val row = records.getJSONObject(i)
                val heading = listOf("name", "title", "number", "description", "message", "entity", "reference", "reason").map { row.optString(it) }.firstOrNull { it.isNotEmpty() } ?: "Record #${i + 1}"
                val status = row.optString("status", "")

                card {
                    // Card Top: Title & Status Badge
                    val topRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val titleView = TextView(this@MainActivity).apply {
                        text = heading
                        textSize = 15f
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    }
                    topRow.addView(titleView)

                    if (status.isNotEmpty()) {
                        val badge = TextView(this@MainActivity).apply {
                            text = when (status) {
                                "active" -> text("نشط", "Active")
                                "issued" -> text("صادرة", "Issued")
                                "paid" -> text("مدفوعة", "Paid")
                                "draft" -> text("مسودة", "Draft")
                                "pending" -> text("قيد المراجعة", "Pending")
                                "approved" -> text("معتمد", "Approved")
                                "rejected" -> text("مرفوض", "Rejected")
                                "exempted" -> text("معفى", "Exempted")
                                "open" -> text("مفتوح", "Open")
                                "closed" -> text("مغلق", "Closed")
                                "vacant" -> text("شاغرة", "Vacant")
                                "occupied" -> text("مؤجرة", "Rented")
                                "owned" -> text("ملك (تمليك)", "Owned")
                                "maintenance" -> text("صيانة", "Maintenance")
                                else -> status
                            }
                            textSize = 11f
                            setTypeface(typeface, Typeface.BOLD)
                            setPadding(dp(8), dp(3), dp(8), dp(3))
                            val (bgColor, textColor) = when (status) {
                                "active", "paid", "approved", "owned" -> Pair(Color.parseColor("#ECFDF5"), Color.parseColor("#059669"))
                                "pending", "draft", "open" -> Pair(Color.parseColor("#EEF2FF"), Color.parseColor("#4F46E5"))
                                "exempted" -> Pair(Color.parseColor("#FEF3C7"), Color.parseColor("#D97706"))
                                "rejected" -> Pair(Color.parseColor("#FFF1F2"), Color.parseColor("#E11D48"))
                                else -> Pair(Color.parseColor("#F1F5F9"), Color.parseColor("#64748B"))
                            }
                            setTextColor(textColor)
                            background = roundedDrawable(bgColor, 99f)
                        }
                        topRow.addView(badge)
                    }
                    addView(topRow)

                    // Secondary Details Grid
                    val detailsList = mutableListOf<Pair<String, String>>()
                    if (row.has("amount")) detailsList.add(Pair(text("المبلغ:", "Amount:"), row.optString("amount") + " EGP"))
                    if (row.has("balance")) detailsList.add(Pair(text("الرصيد:", "Balance:"), row.optString("balance") + " EGP"))
                    if (row.has("due_date")) detailsList.add(Pair(text("الاستحقاق:", "Due:"), row.optString("due_date")))
                    if (row.has("reference")) detailsList.add(Pair(text("المرجع:", "Ref:"), row.optString("reference")))
                    if (row.has("priority")) detailsList.add(Pair(text("الأولوية:", "Priority:"), row.optString("priority")))
                    if (row.has("created_at")) detailsList.add(Pair(text("التاريخ:", "Date:"), row.optString("created_at").take(10)))

                    if (detailsList.isNotEmpty()) {
                        val detailsBox = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(0, dp(8), 0, dp(4))
                        }
                        detailsList.forEach { (lbl, value) ->
                            val r = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                            }
                            r.addView(TextView(this@MainActivity).apply {
                                text = lbl
                                textSize = 12f
                                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                                layoutParams = LinearLayout.LayoutParams(dp(80), -2)
                            })
                            r.addView(TextView(this@MainActivity).apply {
                                text = value
                                textSize = 12f
                                setTypeface(typeface, Typeface.BOLD)
                                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                            })
                            detailsBox.addView(r)
                        }
                        addView(detailsBox)
                    }

                    // Action Buttons Row
                    val actionsRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(10), 0, 0)
                    }

                    if (entity in forms.keys && entity !in listOf("invoices", "expenses", "payment_proofs", "fines") && role in listOf("admin", "manager", "accountant", "technician")) {
                        val editBtn = Button(this@MainActivity).apply {
                            text = text("تعديل", "Edit")
                            textSize = 12f
                            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                            background = roundedDrawable(Color.TRANSPARENT, 8f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
                            setOnClickListener { recordForm(entity, row) }
                        }
                        actionsRow.addView(editBtn, LinearLayout.LayoutParams(-2, dp(36)).apply { marginEnd = dp(8) })
                    }

                    // Payment Proofs Review (Approve / Reject)
                    if (entity == "payment_proofs" && row.optString("status") == "pending" && role in listOf("admin", "manager")) {
                        val approveBtn = Button(this@MainActivity).apply {
                            text = text("اعتماد السداد", "Approve")
                            textSize = 12f
                            setTextColor(Color.WHITE)
                            background = roundedDrawable(Color.parseColor("#059669"), 8f)
                            setOnClickListener {
                                call("payment_proofs/" + row.getString("id") + "/review", "POST", JSONObject().put("decision", "approved")) { listing(entity) }
                            }
                        }
                        actionsRow.addView(approveBtn, LinearLayout.LayoutParams(-2, dp(36)).apply { marginEnd = dp(8) })

                        val rejectBtn = Button(this@MainActivity).apply {
                            text = text("رفض", "Reject")
                            textSize = 12f
                            setTextColor(Color.parseColor("#E11D48"))
                            background = roundedDrawable(Color.parseColor("#FFF1F2"), 8f)
                            setOnClickListener {
                                val reasonInput = EditText(this@MainActivity).apply {
                                    hint = text("سبب الرفض الإلزامي", "Rejection Reason")
                                }
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle(text("رفض إثبات السداد", "Reject Proof"))
                                    .setView(reasonInput)
                                    .setPositiveButton(text("تأكيد الرفض", "Confirm Reject")) { _, _ ->
                                        val r = reasonInput.text.toString().trim()
                                        call("payment_proofs/" + row.getString("id") + "/review", "POST", JSONObject().put("decision", "rejected").put("rejection_reason", r)) { listing(entity) }
                                    }
                                    .setNegativeButton(text("إلغاء", "Cancel"), null)
                                    .show()
                            }
                        }
                        actionsRow.addView(rejectBtn, LinearLayout.LayoutParams(-2, dp(36)))
                    }

                    // Fines Exemption
                    if (entity == "fines" && row.optString("status") == "pending" && role in listOf("admin", "manager")) {
                        val exemptBtn = Button(this@MainActivity).apply {
                            text = text("إعفاء مسبب", "Exempt")
                            textSize = 12f
                            setTextColor(Color.parseColor("#D97706"))
                            background = roundedDrawable(Color.parseColor("#FEF3C7"), 8f)
                            setOnClickListener { fineExemptForm(row.getString("id")) }
                        }
                        actionsRow.addView(exemptBtn, LinearLayout.LayoutParams(-2, dp(36)))
                    }

                    // Invoices Issue & Collect
                    if (entity == "invoices" && role in listOf("admin", "manager", "accountant")) {
                        val id = row.getString("id")
                        if (row.optString("status") == "draft") {
                            val issueBtn = Button(this@MainActivity).apply {
                                text = text("إصدار وترحيل", "Issue & Post")
                                textSize = 12f
                                setTextColor(Color.WHITE)
                                background = roundedDrawable(Color.parseColor("#4F46E5"), 8f)
                                setOnClickListener { call("invoices/$id/issue", "POST", JSONObject()) { listing(entity) } }
                            }
                            actionsRow.addView(issueBtn, LinearLayout.LayoutParams(-2, dp(36)).apply { marginEnd = dp(8) })
                        }
                        if (row.optString("status") == "issued") {
                            val collectBtn = Button(this@MainActivity).apply {
                                text = text("تسجيل تحصيل", "Collect")
                                textSize = 12f
                                setTextColor(Color.WHITE)
                                background = roundedDrawable(Color.parseColor("#059669"), 8f)
                                setOnClickListener { collectForm(id) }
                            }
                            actionsRow.addView(collectBtn, LinearLayout.LayoutParams(-2, dp(36)))
                        }
                    }

                    if (actionsRow.childCount > 0) {
                        addView(actionsRow)
                    }
                }
            }
        }
    }

    private fun proofForm() {
        scaffold(text("إرسال إثبات سداد", "Submit Payment Proof"), true) { listing("payment_proofs") }

        card {
            addView(TextView(this@MainActivity).apply {
                text = text("إرفاق بيانات التحويل البنكي أو المحفظة", "Attach Bank or Wallet Transfer Details")
                textSize = 13f
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, 0, 0, dp(12))
            })
        }

        val amount = input(text("المبلغ المحول", "Transferred Amount")).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val reference = input(text("رقم الحوالة / المرجع البنكي", "Transaction Reference"))

        call("properties") { propRaw ->
            val props = JSONArray(propRaw)
            if (props.length() == 0) {
                error(IllegalStateException(text("لا توجد عقارات مسجلة بعد", "No properties registered yet")))
                return@call
            }
            val defaultPropId = props.getJSONObject(0).getString("id")

            call("units") { raw ->
                val units = JSONArray(raw)
                val unitSpinner = Spinner(this)
                unitSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until units.length()).map { units.getJSONObject(it).getString("name") })
                label(text("الوحدة السكنية", "Unit"), 13, true)
                screen.addView(unitSpinner)

                call("parties") { partyRaw ->
                    val parties = JSONArray(partyRaw)
                    val partySpinner = Spinner(this)
                    partySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until parties.length()).map { parties.getJSONObject(it).getString("name") })
                    label(text("الساكن / المسدد", "Resident / Payer"), 13, true)
                    screen.addView(partySpinner)

                    primaryButton(text("إرسال الإثبات", "Submit Proof")) {
                        val uId = if (units.length() > 0) units.getJSONObject(unitSpinner.selectedItemPosition).getString("id") else ""
                        val pId = if (parties.length() > 0) parties.getJSONObject(partySpinner.selectedItemPosition).getString("id") else ""
                        val pUnit = if (units.length() > 0) units.getJSONObject(unitSpinner.selectedItemPosition) else null
                        val propId = if (pUnit != null && pUnit.has("property_id")) pUnit.getString("property_id") else defaultPropId

                        val payload = JSONObject()
                            .put("property_id", propId)
                            .put("unit_id", uId)
                            .put("party_id", pId)
                            .put("amount", amount.text.toString().trim())
                            .put("reference", reference.text.toString().trim())
                            .put("payment_method", "bank_transfer")

                        call("payment_proofs", "POST", payload) { listing("payment_proofs") }
                    }
                }
            }
        }
    }

    private fun ticketForm() {
        scaffold(text("طلب صيانة جديد", "Maintenance Request"), true) { listing("tickets") }
        val heading = input(text("عنوان المشكلة", "Issue Title"))
        val description = input(text("تفاصيل المشكلة", "Detailed Description")).apply { setSingleLine(false); maxLines = 4 }
        val spinner = Spinner(this)
        screen.addView(spinner)
        call("units") { raw ->
            val units = JSONArray(raw)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until units.length()).map { units.getJSONObject(it).getString("name") })
            primaryButton(text("إرسال البلاغ", "Submit Ticket")) {
                val payload = JSONObject().put("title", heading.text.toString().trim()).put("description", description.text.toString().trim())
                if (units.length() > 0) payload.put("unit_id", units.getJSONObject(spinner.selectedItemPosition).getString("id"))
                call("tickets", "POST", payload) { listing("tickets") }
            }
        }
    }

    private fun supportTicketForm() {
        scaffold(text("تذكرة دعم فني", "Support Ticket"), true) { listing("support_tickets") }
        val title = input(text("عنوان الاستفسار أو المشكلة", "Issue Title"))
        val desc = input(text("شرح تفصيلي", "Description")).apply { setSingleLine(false); maxLines = 4 }
        primaryButton(text("إرسال التذكرة", "Submit")) {
            val payload = JSONObject().put("title", title.text.toString().trim()).put("description", desc.text.toString().trim()).put("category", "technical").put("priority", "normal")
            call("support_tickets", "POST", payload) { listing("support_tickets") }
        }
    }

    private fun fineExemptForm(fineId: String) {
        scaffold(text("إعفاء مسبب من الغرامة", "Exempt Fine"), true) { listing("fines") }
        val reason = input(text("سبب الإعفاء الإلزامي", "Mandatory Exemption Rationale"))
        primaryButton(text("تأكيد الإعفاء والتوثيق", "Confirm Exemption")) {
            call("fines/$fineId/exempt", "POST", JSONObject().put("reason", reason.text.toString().trim())) { listing("fines") }
        }
    }

    private fun collectForm(id: String) {
        scaffold(text("تسجيل تحصيل", "Record Receipt"), true) { listing("invoices") }
        val amount = input(text("المبلغ المحصل", "Collected Amount")).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val reference = input(text("المرجع الفريد للحركة", "Unique Reference"))
        primaryButton(text("حفظ وترحيل الإيصال", "Save & Post")) {
            call("invoices/$id/collect", "POST", JSONObject().put("amount", amount.text.toString().trim()).put("reference", reference.text.toString().trim())) { listing("invoices") }
        }
    }

    private val forms = mapOf(
        "properties" to listOf("name", "address"),
        "units" to listOf("property_id", "name", "floor", "area", "status", "ownership_type"),
        "parties" to listOf("name", "kind", "email", "phone"),
        "leases" to listOf("unit_id", "party_id", "start_date", "end_date", "rent", "status"),
        "ownerships" to listOf("unit_id", "party_id", "share"),
        "accounts" to listOf("code", "name", "kind"),
        "invoices" to listOf("number", "party_id", "unit_id", "description", "amount", "due_date"),
        "expenses" to listOf("description", "amount"),
        "assets" to listOf("property_id", "name", "serial", "next_service"),
        "technicians" to listOf("name", "specialty", "phone"),
        "tickets" to listOf("title", "description", "unit_id", "asset_id", "priority", "status", "technician_id"),
        "announcements" to listOf("title", "body"),
        "money_accounts" to listOf("property_id", "name", "kind", "bank_name", "account_number"),
        "fines" to listOf("property_id", "unit_id", "party_id", "title", "amount", "destination"),
        "support_tickets" to listOf("title", "description", "category", "priority"),
        "vendor_contracts" to listOf("property_id", "vendor_id", "title", "service_type", "amount", "frequency", "start_date", "end_date"),
        "budgets" to listOf("property_id", "name", "period", "start_date", "end_date")
    )

    private val fieldNames = mapOf(
        "name" to "الاسم", "address" to "العنوان", "property_id" to "العقار", "unit_id" to "الوحدة", "party_id" to "الطرف المعني",
        "floor" to "الطابق", "area" to "المساحة (م²)", "status" to "الحالة", "ownership_type" to "نوع الحيازة والملكية", "email" to "البريد الإلكتروني", "phone" to "رقم الهاتف",
        "kind" to "النوع", "start_date" to "تاريخ البدء (YYYY-MM-DD)", "end_date" to "تاريخ الانتهاء (YYYY-MM-DD)", "rent" to "مبلغ الإيجار",
        "share" to "نسبة الملكية %", "code" to "كود الحساب", "number" to "الرقم التسلسلي", "description" to "الوصف والتفاصيل",
        "amount" to "المبلغ الإجمالي", "due_date" to "تاريخ الاستحقاق (YYYY-MM-DD)", "title" to "العنوان", "body" to "المحتوى والبيان",
        "priority" to "الأولوية", "asset_id" to "الأصل أو المرفق", "technician_id" to "الفني المعتمد", "specialty" to "التخصص المهني",
        "serial" to "الرقم التسلسلي", "next_service" to "تاريخ الصيانة القادمة", "reason" to "السبب أو المبرر", "category" to "التصنيف",
        "vendor_id" to "المورد / شركة الخدمات", "service_type" to "نوع الخدمة", "frequency" to "الدورية",
        "period" to "الفترة المالية", "destination" to "الصندوق المحول إليه", "bank_name" to "اسم البنك", "account_number" to "رقم الحساب"
    )

    private fun recordForm(entity: String, record: JSONObject? = null) {
        scaffold(title(entity), true) { listing(entity) }
        val values = linkedMapOf<String, () -> String>()
        val keys = forms[entity] ?: return

        fun addField(index: Int) {
            if (index >= keys.size) {
                primaryButton(text("حفظ السجل", "Save Record")) {
                    val payload = JSONObject()
                    values.forEach { (key, getter) ->
                        val value = getter()
                        if (value.isNotBlank()) payload.put(key, value)
                    }
                    call(entity + (record?.let { "/" + it.getString("id") } ?: ""), if (record == null) "POST" else "PATCH", payload) {
                        listing(entity)
                    }
                }
                return
            }
            val key = keys[index]
            val initial = if (record?.isNull(key) == false) record.optString(key) else ""
            val relation = mapOf(
                "property_id" to "properties",
                "unit_id" to "units",
                "party_id" to "parties",
                "vendor_id" to "parties",
                "asset_id" to "assets",
                "technician_id" to "technicians"
            )[key]

            val options = when {
                key == "kind" && entity == "parties" -> listOf("tenant", "owner", "vendor")
                key == "kind" && entity == "accounts" -> listOf("asset", "liability", "equity", "income", "expense")
                key == "kind" && entity == "money_accounts" -> listOf("cash", "bank", "wallet", "maintenance_fund")
                key == "destination" && entity == "fines" -> listOf("maintenance_fund", "fines_fund")
                key == "frequency" && entity == "vendor_contracts" -> listOf("monthly", "quarterly", "yearly", "one_time")
                key == "period" && entity == "budgets" -> listOf("monthly", "quarterly", "annual")
                key == "status" && entity == "units" -> listOf("vacant", "occupied", "owned", "maintenance")
                key == "ownership_type" && entity == "units" -> listOf("rental", "owned")
                key == "status" && entity == "leases" -> listOf("active", "ended")
                key == "priority" -> listOf("normal", "low", "urgent")
                key == "category" && entity == "support_tickets" -> listOf("technical", "billing", "accounting", "general")
                key == "status" -> listOf("open", "assigned", "in_progress", "resolved", "closed")
                else -> emptyList()
            }

            if (relation != null || options.isNotEmpty()) {
                label(text(fieldNames[key] ?: key, key.replace('_', ' ')), 13, true)
                fun select(items: List<Pair<String, String>>) {
                    val spinner = Spinner(this)
                    spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.second })
                    spinner.setSelection(items.indexOfFirst { it.first == initial }.coerceAtLeast(0))
                    screen.addView(spinner)
                    values[key] = { items.getOrNull(spinner.selectedItemPosition)?.first ?: "" }
                    addField(index + 1)
                }
                if (relation != null) {
                    call(relation) { raw ->
                        val rows = JSONArray(raw)
                        select(listOf("" to "—") + (0 until rows.length()).map {
                            val row = rows.getJSONObject(it)
                            row.getString("id") to row.optString("name")
                        })
                    }
                } else {
                    select(options.map { it to it })
                }
            } else {
                val control = input(text(fieldNames[key] ?: key, key.replace('_', ' '))).apply {
                    setText(initial)
                    if (key in listOf("amount", "rent", "area", "share", "floor")) {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    }
                }
                values[key] = { control.text.toString().trim() }
                addField(index + 1)
            }
        }
        addField(0)
    }

    private fun googleLogin() {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val verifier = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val proof = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        getPreferences(0).edit().putString("oauth_verifier", verifier).apply()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base + "/api/auth/google/start?client=android&challenge=" + proof)))
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuth(intent)
        if ((intent.hasExtra("open_notifications") || intent.hasExtra("notification_id")) && token.isNotEmpty()) {
            listing("notifications")
        }
    }

    private fun handleAuth(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "towerflow" || uri.host != "auth") return
        val code = uri.getQueryParameter("code") ?: return
        val verifier = getPreferences(0).getString("oauth_verifier", null) ?: return
        getPreferences(0).edit().remove("oauth_verifier").apply()
        call("auth/mobile/exchange", "POST", JSONObject().put("code", code).put("verifier", verifier)) { raw ->
            token = JSONObject(raw).getString("token")
            loadMe()
        }
    }

    private fun profile() {
        scaffold(text("الملف الشخصي", "My Profile"), true) { home() }
        avatar = if (member.isNull("avatar")) null else member.optString("avatar").ifEmpty { null }
        avatar?.let { value ->
            try {
                val bytes = Base64.decode(value.substringAfter(','), Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                screen.addView(ImageView(this).apply {
                    setImageBitmap(bitmap)
                    adjustViewBounds = true
                }, LinearLayout.LayoutParams(dp(100), dp(100)))
            } catch (_: Exception) {}
        }
        label(member.optString("email"), 14, true)
        val name = input(text("الاسم الكامل", "Full name")).apply { setText(member.optString("name")) }
        val phone = input(text("رقم الهاتف", "Phone")).apply { setText(member.optString("phone")); inputType = InputType.TYPE_CLASS_PHONE }
        val bio = input(text("نبذة عنك", "About you")).apply { setText(member.optString("bio")); setSingleLine(false); maxLines = 4 }
        primaryButton(text("حفظ التغييرات", "Save Profile")) {
            call("me/profile", "PATCH", JSONObject().put("name", name.text.toString().trim()).put("phone", phone.text.toString().trim()).put("bio", bio.text.toString().trim()).put("locale", if (arabic) "ar" else "en")) { raw ->
                member = JSONObject(raw).getJSONObject("user")
                profile()
            }
        }
    }

    private fun registerPush(showResult: Boolean = false) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { device ->
            getSharedPreferences("push_device", MODE_PRIVATE).edit().putString("token", device).apply()
            if (token.isNotEmpty()) {
                call("push/devices", "POST", JSONObject().put("token", device)) {
                    if (showResult) Toast.makeText(this, text("تم تفعيل الإشعارات بنجاح", "Notifications enabled successfully"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Platform permission callback compatibility")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 301 && grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) registerPush(true)
    }

    private fun disablePushAndLogout() {
        val device = getSharedPreferences("push_device", MODE_PRIVATE).getString("token", null)
        fun finishLogout() {
            call("auth/logout", "POST", JSONObject()) {
                token = ""
                org = ""
                getSystemService(NotificationManager::class.java).cancelAll()
                login()
            }
        }
        if (device != null) call("push/devices/remove", "POST", JSONObject().put("token", device)) { finishLogout() } else finishLogout()
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdownNow()
    }
}
