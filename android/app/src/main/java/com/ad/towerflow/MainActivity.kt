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
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.FrameLayout
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

    // --- Executive Design System & Cairo Typography ---
    private val cairoFont: Typeface by lazy {
        try {
            val resId = resources.getIdentifier("cairo", "font", packageName)
            if (resId != 0) resources.getFont(resId) else Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

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

    private fun gradientDrawable(startColor: Int, endColor: Int, cornerRadiusDp: Float = 12f): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(startColor, endColor)).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerRadiusDp.toInt()).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        base = getPreferences(0).getString("base", "https://towerflow.vercel.app") ?: "https://towerflow.vercel.app"
        arabic = getPreferences(0).getBoolean("arabic", true)
        dark = getPreferences(0).getBoolean("dark", false)
        login()
        handleAuth(intent)
        pendingNotifications = intent?.extras?.getBoolean("open_notifications", false) == true || intent?.hasExtra("notification_id") == true
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("towerflow_residents", "TowerFlow · رسائل السكان", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun scaffold(heading: String, showBack: Boolean = false, onBack: (() -> Unit)? = null) {
        // Dynamic Status Bar & Navigation Bar Theming (Matches Executive Web Palette)
        try {
            window.statusBarColor = Color.parseColor(if (dark) "#090A0F" else "#FFFFFF")
            window.navigationBarColor = Color.parseColor(if (dark) "#090A0F" else "#FFFFFF")
            var flags = window.decorView.systemUiVisibility
            flags = if (dark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        } catch (_: Exception) {}

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor(if (dark) "#090A0F" else "#F8FAFC"))
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        // Account for Phone Notch, Status Bar, Clock and Battery Safe Insets
        val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarResId > 0) resources.getDimensionPixelSize(statusBarResId) else dp(28)
        val safeTopPadding = statusBarHeight + dp(12)

        // Account for System Navigation Bar (||| O <) Safe Insets
        val navBarResId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = if (navBarResId > 0) resources.getDimensionPixelSize(navBarResId) else dp(48)
        val safeBottomPadding = navBarHeight + dp(36)

        screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), safeTopPadding, dp(16), safeBottomPadding)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        scroll.addView(screen)
        setContentView(scroll)

        // Modern Header TopBar with absolute RTL/LTR side-switching support
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dp(2), 0, dp(2), dp(14))
            layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        if (showBack && onBack != null) {
            val backBtn = Button(this).apply {
                text = text("‹ عودة", "‹ Back")
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#4F46E5"))
                textSize = 13.5f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                background = roundedDrawable(Color.TRANSPARENT, 10f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                    if (arabic) marginStart = dp(2) else marginEnd = dp(6)
                }
                setOnClickListener { onBack() }
            }
            topBar.addView(backBtn)
        }

        // Brand Layout: [Logo] + [Column: TowerFlow & Subtitle] (Right in Arabic, Left in English)
        val brandLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val logoView = ImageView(this).apply {
            setImageResource(R.drawable.towerflow_logo)
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginEnd = dp(10)
            }
        }

        val brandCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        brandCol.addView(TextView(this).apply {
            text = "TowerFlow"
            textSize = 18f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        })
        brandCol.addView(TextView(this).apply {
            text = heading
            textSize = 11.5f
            typeface = cairoFont
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
        })

        brandLayout.addView(logoView)
        brandLayout.addView(brandCol)
        topBar.addView(brandLayout)

        // Spacer pushing action buttons to the opposite edge
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        topBar.addView(spacer)

        // Actions Group: [Language] [Dark Mode]
        val actionsGroup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val langBtn = Button(this).apply {
            text = if (arabic) "EN" else "عربي"
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            setOnClickListener {
                arabic = !arabic
                getPreferences(0).edit().putBoolean("arabic", arabic).apply()
                if (token.isEmpty()) login() else home()
            }
        }
        val langParams = LinearLayout.LayoutParams(dp(54), dp(36)).apply { marginEnd = dp(6) }
        actionsGroup.addView(langBtn, langParams)

        val themeBtn = Button(this).apply {
            text = if (dark) "☀" else "☾"
            textSize = 14f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#FCD34D" else "#1E293B"))
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            setOnClickListener {
                dark = !dark
                getPreferences(0).edit().putBoolean("dark", dark).apply()
                if (token.isEmpty()) login() else home()
            }
        }
        actionsGroup.addView(themeBtn, LinearLayout.LayoutParams(dp(42), dp(36)))

        topBar.addView(actionsGroup)
        screen.addView(topBar)
    }

    private fun card(fn: LinearLayout.() -> Unit): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedDrawable(
                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                16f,
                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
            )
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        }
        layout.fn()
        screen.addView(layout)
        return layout
    }

    private fun label(value: String, size: Int = 14, isBold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        typeface = if (isBold) Typeface.create(cairoFont, Typeface.BOLD) else cairoFont
        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        setPadding(0, dp(4), 0, dp(4))
        screen.addView(this)
    }

    private fun sectionHeader(titleText: String) {
        val tv = TextView(this).apply {
            text = titleText
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            setPadding(dp(4), dp(16), dp(4), dp(8))
        }
        screen.addView(tv)
    }

    private fun input(hintText: String, password: Boolean = false): EditText = EditText(this).apply {
        this.hint = hintText
        setSingleLine()
        typeface = cairoFont
        textSize = 14f
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
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
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
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
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
        val navIcons = mapOf(
            "properties" to "🏢",
            "units" to "🚪",
            "parties" to "👥",
            "leases" to "📑",
            "ownerships" to "🔑",
            "invoices" to "💳",
            "receipts" to "🧾",
            "payment_proofs" to "📤",
            "credit_balances" to "💰",
            "fines" to "⚠️",
            "money_accounts" to "🏦",
            "expenses" to "📉",
            "journals" to "📊",
            "trial_balance" to "📈",
            "tickets" to "🛠️",
            "assets" to "🛗",
            "technicians" to "👷",
            "vendor_contracts" to "📝",
            "budgets" to "📋",
            "expense_distributions" to "⚖️",
            "announcements" to "📢",
            "notifications" to "🔔",
            "support_tickets" to "🎫",
            "audit_logs" to "📜"
        )
        val iconGlyph = navIcons[key] ?: "📁"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedDrawable(
                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                14f,
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

        // Squircle Item Icon Badge
        val iconBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginEnd = dp(12)
            }
            background = roundedDrawable(
                if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"),
                10f,
                if (dark) Color.parseColor("#312E81") else Color.parseColor("#C7D2FE")
            )
            addView(TextView(this@MainActivity).apply {
                text = iconGlyph
                textSize = 17f
                gravity = Gravity.CENTER
            })
        }
        row.addView(iconBox)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                setPadding(dp(6), 0, dp(6), 0)
            }
        }
        textCol.addView(TextView(this).apply {
            text = titleText
            textSize = 14f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        })
        if (subtitleText.isNotEmpty()) {
            textCol.addView(TextView(this).apply {
                text = subtitleText
                textSize = 11f
                typeface = cairoFont
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, dp(2), 0, 0)
            })
        }
        row.addView(textCol)

        if (badgeCount > 0) {
            val badge = TextView(this).apply {
                text = badgeCount.toString()
                textSize = 11f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor("#B45309"))
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = roundedDrawable(Color.parseColor("#FEF3C7"), 99f, Color.parseColor("#FDE68A"))
            }
            row.addView(badge)
        }

        val chevron = TextView(this).apply {
            text = if (arabic) "‹" else "›"
            textSize = 18f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
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

    private fun call(path: String, method: String = "GET", data: JSONObject? = null, onError: ((Exception) -> Unit)? = null, done: (String) -> Unit) {
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
                    runOnUiThread { try { done(raw) } catch (e: Exception) { onError?.invoke(e) ?: error(e) } }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread { onError?.invoke(e) ?: error(e) }
            }
        }
    }

    private fun login(register: Boolean = false) {
        scaffold(text(if (register) "إنشاء حساب ومؤسسة" else "تسجيل الدخول", if (register) "Create Account" else "Sign In"))

        // SaaS Auth Form Card (Exact Web Component)
        val authCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
            background = roundedDrawable(
                if (dark) Color.parseColor("#11131A") else Color.WHITE,
                20f,
                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
            )
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(12)
            }
        }

        // Center Logo Badge (Exact Website Logo Vector)
        val symbolBadge = ImageView(this).apply {
            setImageResource(R.drawable.towerflow_logo)
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
        }
        authCard.addView(symbolBadge)

        // Title
        val titleView = TextView(this).apply {
            text = text(if (register) "ابدأ مع TowerFlow" else "أهلًا بعودتك", if (register) "Start with TowerFlow" else "Welcome back")
            textSize = 20f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.parseColor(if (dark) "#F8FAFC" else "#0F172A"))
        }
        authCard.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = text(
                if (register) "أنشئ حسابك وابدأ تنظيم محفظتك العقارية." else "سجّل دخولك لمتابعة عقاراتك وأعمالك اليومية.",
                if (register) "Create an account and organize your portfolio." else "Sign in to your workspace and pick up where you left off."
            )
            textSize = 12.5f
            typeface = cairoFont
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(dp(6), dp(4), dp(6), dp(16))
        }
        authCard.addView(subtitleView)

        fun addFormField(lblText: String, hintText: String, isPass: Boolean = false, isEmail: Boolean = false): EditText {
            val lbl = TextView(this).apply {
                text = lblText
                textSize = 12f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#475569"))
                setPadding(dp(2), dp(6), dp(2), dp(4))
            }
            authCard.addView(lbl)

            val et = EditText(this).apply {
                hint = hintText
                setSingleLine()
                typeface = cairoFont
                textSize = 14f
                inputType = when {
                    isPass -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    isEmail -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    else -> InputType.TYPE_CLASS_TEXT
                }
                setTextColor(Color.parseColor(if (dark) "#F8FAFC" else "#0F172A"))
                setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                background = roundedDrawable(
                    if (dark) Color.parseColor("#0C0E14") else Color.parseColor("#F8FAFC"),
                    12f,
                    if (dark) Color.parseColor("#242838") else Color.parseColor("#CBD5E1")
                )
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                    bottomMargin = dp(6)
                }
            }
            authCard.addView(et)
            return et
        }

        val name = if (register) addFormField(text("الاسم الكامل", "Full Name"), text("اسمك الكامل", "Your full name")) else null
        val organization = if (register) addFormField(text("اسم المؤسسة أو البرج", "Organization / Tower"), text("اسم شركتك أو مؤسستك", "Your organization name")) else null
        val email = addFormField(text("البريد الإلكتروني", "Email Address"), "name@company.com", isEmail = true)
        val pass = addFormField(text("كلمة المرور", "Password"), text("12 حرفًا على الأقل", "At least 12 characters"), isPass = true)

        // Primary Submit Button with Indigo Gradient
        val submitBtn = Button(this).apply {
            val arrow = if (arabic) "‹" else "›"
            text = text(
                if (register) "إنشاء حساب ومؤسسة  $arrow" else "تسجيل الدخول  $arrow",
                if (register) "Create Account  $arrow" else "Sign In  $arrow"
            )
            setTextColor(Color.WHITE)
            textSize = 14.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                topMargin = dp(14)
                bottomMargin = dp(4)
            }
            setOnClickListener {
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
        }
        authCard.addView(submitBtn)

        // Divider (.auth-divider)
        val dividerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
                setBackgroundColor(Color.parseColor(if (dark) "#1E2330" else "#E2E8F0"))
            })
            addView(TextView(this@MainActivity).apply {
                text = text("أو", "or")
                textSize = 11.5f
                typeface = cairoFont
                setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                setPadding(dp(12), 0, dp(12), 0)
            })
            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f)
                setBackgroundColor(Color.parseColor(if (dark) "#1E2330" else "#E2E8F0"))
            })
        }
        authCard.addView(dividerRow)

        // Google Login Button
        val googleBtn = Button(this).apply {
            text = text("G  المتابعة باستخدام Google", "G  Continue with Google")
            setTextColor(Color.parseColor(if (dark) "#F8FAFC" else "#0F172A"))
            textSize = 13.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            background = roundedDrawable(
                if (dark) Color.parseColor("#151824") else Color.WHITE,
                12f,
                if (dark) Color.parseColor("#27324A") else Color.parseColor("#CBD5E1")
            )
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { googleLogin() }
        }
        authCard.addView(googleBtn)

        // Switch Auth Row
        val switchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            }
            addView(TextView(this@MainActivity).apply {
                text = text(
                    if (register) "لديك حساب بالفعل؟ " else "ليس لديك حساب بعد؟ ",
                    if (register) "Already have an account? " else "New to TowerFlow? "
                )
                textSize = 12.5f
                typeface = cairoFont
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            })
            val switchBtn = TextView(this@MainActivity).apply {
                text = text(if (register) "تسجيل الدخول" else "إنشاء حساب", if (register) "Sign in" else "Create account")
                textSize = 13f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                setPadding(dp(4), dp(4), dp(4), dp(4))
                isClickable = true
                isFocusable = true
                setOnClickListener { login(!register) }
            }
            addView(switchBtn)
        }
        authCard.addView(switchRow)

        screen.addView(authCard)
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
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
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
            val heroRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val userName = member.optString("name", "User")
            val initial = if (userName.isNotEmpty()) userName.take(1) else "U"
            val avatarBox = LinearLayout(this@MainActivity).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                    marginEnd = dp(12)
                }
                background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 14f)
                addView(TextView(this@MainActivity).apply {
                    text = initial
                    textSize = 18f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                })
            }
            heroRow.addView(avatarBox)

            val infoCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setPadding(dp(4), 0, dp(4), 0)
                }
            }
            val greeting = TextView(this@MainActivity).apply {
                text = text("مرحبًا، ", "Welcome back, ") + userName
                textSize = 16.5f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            }
            infoCol.addView(greeting)

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
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor("#4F46E5"))
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = roundedDrawable(Color.parseColor(if (dark) "#1E1B4B" else "#EEF2FF"), 99f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(2)
                }
            }
            infoCol.addView(roleBadge)
            heroRow.addView(infoCol)
            addView(heroRow)
        }

        // Live KPI Container (Positioned permanently at the TOP directly below Hero greeting)
        val kpiContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(4)
            }
        }
        screen.addView(kpiContainer)

        if (role != "resident") {
            fun renderKpis(dash: JSONObject) {
                kpiContainer.removeAllViews()
                val unitsObj = dash.optJSONObject("units") ?: JSONObject()
                val totalUnits = unitsObj.optInt("total", 0)
                val occupiedUnits = unitsObj.optInt("occupied", 0)
                val vacantUnits = unitsObj.optInt("vacant", 0)
                val ownedUnits = unitsObj.optInt("owned", 0)
                val rentedUnits = unitsObj.optInt("rented", occupiedUnits)
                val maintUnits = unitsObj.optInt("maintenance", 0)
                val pct = if (totalUnits > 0) ((rentedUnits + ownedUnits) * 100 / totalUnits) else 0

                fun makeKpi(kpiTitle: String, kpiValue: String, kpiSub: String, isFirst: Boolean): LinearLayout {
                    return LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                        background = roundedDrawable(
                            if (dark) Color.parseColor("#11131A") else Color.WHITE,
                            14f,
                            if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
                        )
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            if (isFirst) marginEnd = dp(4) else marginStart = dp(4)
                        }
                        addView(TextView(this@MainActivity).apply {
                            text = kpiTitle
                            textSize = 11f
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = kpiValue
                            textSize = 19f
                            setTextColor(Color.parseColor("#4F46E5"))
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setPadding(0, dp(2), 0, dp(2))
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = kpiSub
                            textSize = 9.5f
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            typeface = cairoFont
                            setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                        })
                    }
                }

                val kpiRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    weightSum = 2f
                }
                kpiRow.addView(makeKpi(text("إجمالي الشقق", "Total Units"), totalUnits.toString(), text("$rentedUnits مؤجرة · $ownedUnits ملك", "$rentedUnits rent · $ownedUnits own"), true))
                kpiRow.addView(makeKpi(text("نسبة الإشغال", "Occupancy"), "$pct%", text("$vacantUnits شاغرة · $maintUnits صيانة", "$vacantUnits vacant"), false))
                kpiContainer.addView(kpiRow)

                val kpiRow2 = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(6)
                    }
                    weightSum = 2f
                }
                kpiRow2.addView(makeKpi(text("وحدات تمليك (ملك)", "Owned Units"), ownedUnits.toString(), text("ملاك مقيمين", "Owner units"), true))
                kpiRow2.addView(makeKpi(text("وحدات مؤجرة", "Rented Units"), rentedUnits.toString(), text("عقود إيجار نشطة", "Active leases"), false))
                kpiContainer.addView(kpiRow2)
            }

            renderKpis(JSONObject())

            call("dashboard", onError = { /* non-critical dashboard kpi metrics */ }) { raw ->
                try {
                    renderKpis(JSONObject(raw))
                } catch (_: Exception) {}
            }
        }

        // Quick Actions Row
        sectionHeader(text("إجراءات سريعة", "Quick Actions"))
        val quickGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            weightSum = 2f
        }

        val q1 = Button(this).apply {
            text = text(if (role == "resident") "📤 إثبات سداد" else "⚡ تحصيل", if (role == "resident") "Proof" else "Collect")
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { if (role == "resident") proofForm() else listing("invoices") }
        }
        quickGrid.addView(q1)

        val q2 = Button(this).apply {
            text = text("🔧 بلاغ صيانة", "Ticket")
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
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
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
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
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
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
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    topMargin = dp(2)
                                    bottomMargin = dp(2)
                                }
                            }
                            r.addView(TextView(this@MainActivity).apply {
                                text = lbl
                                textSize = 11.5f
                                typeface = cairoFont
                                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                                layoutParams = LinearLayout.LayoutParams(dp(75), -2)
                            })
                            r.addView(TextView(this@MainActivity).apply {
                                text = value
                                textSize = 11.5f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                            })
                            detailsBox.addView(r)
                        }
                        addView(detailsBox)
                    }

                    // Action Buttons Row
                    val actionsRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                    }

                    if (entity in forms.keys && entity !in listOf("invoices", "expenses", "payment_proofs", "fines") && role in listOf("admin", "manager", "accountant", "technician")) {
                        val editBtn = Button(this@MainActivity).apply {
                            text = text("تعديل", "Edit")
                            textSize = 12f
                            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                            background = roundedDrawable(Color.TRANSPARENT, 8f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
                            setOnClickListener { recordForm(entity, row) }
                        }
                        actionsRow.addView(editBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
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
                        actionsRow.addView(approveBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })

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
                        actionsRow.addView(rejectBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
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
                        actionsRow.addView(exemptBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
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
                            actionsRow.addView(issueBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
                        }
                        if (row.optString("status") == "issued") {
                            val collectBtn = Button(this@MainActivity).apply {
                                text = text("تسجيل تحصيل", "Collect")
                                textSize = 12f
                                setTextColor(Color.WHITE)
                                background = roundedDrawable(Color.parseColor("#059669"), 8f)
                                setOnClickListener { collectForm(id) }
                            }
                            actionsRow.addView(collectBtn, LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
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
        "name" to Pair("الاسم", "Name"),
        "address" to Pair("العنوان", "Address"),
        "property_id" to Pair("العقار", "Property"),
        "unit_id" to Pair("الوحدة", "Unit"),
        "party_id" to Pair("الطرف المعني", "Party / Resident"),
        "floor" to Pair("الطابق", "Floor"),
        "area" to Pair("المساحة (م²)", "Area (m²)"),
        "status" to Pair("الحالة", "Status"),
        "ownership_type" to Pair("نوع الحيازة والملكية", "Ownership Type"),
        "email" to Pair("البريد الإلكتروني", "Email"),
        "phone" to Pair("رقم الهاتف", "Phone"),
        "kind" to Pair("النوع", "Type / Kind"),
        "start_date" to Pair("تاريخ البدء (YYYY-MM-DD)", "Start Date (YYYY-MM-DD)"),
        "end_date" to Pair("تاريخ الانتهاء (YYYY-MM-DD)", "End Date (YYYY-MM-DD)"),
        "rent" to Pair("مبلغ الإيجار", "Rent Amount"),
        "share" to Pair("نسبة الملكية %", "Ownership Share %"),
        "code" to Pair("كود الحساب", "Account Code"),
        "number" to Pair("الرقم التسلسلي", "Serial Number"),
        "description" to Pair("الوصف والتفاصيل", "Description"),
        "amount" to Pair("المبلغ الإجمالي", "Total Amount"),
        "due_date" to Pair("تاريخ الاستحقاق (YYYY-MM-DD)", "Due Date (YYYY-MM-DD)"),
        "title" to Pair("العنوان", "Title"),
        "body" to Pair("المحتوى والبيان", "Body / Message"),
        "priority" to Pair("الأولوية", "Priority"),
        "asset_id" to Pair("الأصل أو المرفق", "Asset / Equipment"),
        "technician_id" to Pair("الفني المعتمد", "Assigned Technician"),
        "specialty" to Pair("التخصص المهني", "Specialty"),
        "serial" to Pair("الرقم التسلسلي", "Serial Number"),
        "next_service" to Pair("تاريخ الصيانة القادمة", "Next Service Date"),
        "reason" to Pair("السبب أو المبرر", "Reason"),
        "category" to Pair("التصنيف", "Category"),
        "vendor_id" to Pair("المورد / شركة الخدمات", "Vendor / Service Company"),
        "service_type" to Pair("نوع الخدمة", "Service Type"),
        "frequency" to Pair("الدورية", "Frequency"),
        "period" to Pair("الفترة المالية", "Financial Period"),
        "destination" to Pair("الصندوق المحول إليه", "Destination Fund"),
        "bank_name" to Pair("اسم البنك", "Bank Name"),
        "account_number" to Pair("رقم الحساب", "Account Number")
    )

    private fun fieldTitle(k: String) = fieldNames[k]?.let { if (arabic) it.first else it.second } ?: k.replace('_', ' ')

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
                label(fieldTitle(key), 13, true)
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
                val control = input(fieldTitle(key)).apply {
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
                call("push/devices", "POST", JSONObject().put("token", device), onError = {}) {
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
