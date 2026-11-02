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
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
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
        "rental" to Pair("إيجار", "Rental"),
        "recurring_charges" to Pair("الرسوم المتكررة والدورية", "Recurring Charges"),
        "maintenance_plans" to Pair("خطط الصيانة الوقائية", "Maintenance Plans"),
        "accounting_periods" to Pair("الفترات المالية والمحاسبية", "Accounting Periods"),
        "members" to Pair("فريق العمل والأعضاء", "Team Members"),
        "resident_messages" to Pair("رسائل واستفسارات السكان", "Resident Inquiries"),
        "reports" to Pair("التقارير المالية والتشغيلية", "Financial & Operational Reports"),
        "profile" to Pair("الملف الشخصي", "Profile"),
        "settings" to Pair("الإعدادات العامة", "Settings")
    )

    private fun text(ar: String, en: String) = if (arabic) ar else en
    private fun title(k: String) = names[k]?.let { if (arabic) it.first else it.second } ?: k

    private fun optionTitle(k: String): String {
        return when (k) {
            "admin" -> text("مدير النظام", "System Admin")
            "manager" -> text("مدير البرج", "Building Manager")
            "accountant" -> text("المحاسب المالي", "Accountant")
            "technician" -> text("فني صيانة", "Technician")
            "resident" -> text("ساكن / مالك / مستأجر", "Resident")
            "viewer" -> text("مراقب ومستعرض", "Viewer")

            "tenant" -> text("مستأجر", "Tenant")
            "owner" -> text("مالك", "Owner")
            "vendor" -> text("مورد / مقاول صيانة", "Vendor")

            "asset" -> text("أصول", "Asset")
            "liability" -> text("التزامات / خصوم", "Liability")
            "equity" -> text("حقوق ملكية", "Equity")
            "income" -> text("إيرادات", "Income")
            "expense" -> text("مصروفات", "Expense")

            "cash" -> text("خزينة نقدية (كاش)", "Cash")
            "bank" -> text("حساب بنكي", "Bank")
            "wallet" -> text("محفظة إلكترونية", "E-Wallet")
            "card" -> text("بطاقة دفع إلكتروني", "Card")
            "bank_transfer" -> text("تحويل بنكي", "Bank Transfer")
            "other" -> text("طريقة أخرى", "Other")
            "maintenance_fund" -> text("صندوق الصيانة والخدمات", "Maintenance Fund")
            "fines_fund" -> text("صندوق الغرامات والجزاءات", "Fines Fund")

            "vacant" -> text("شاغرة وغير مشغولة", "Vacant")
            "occupied" -> text("مشغولة / مؤجرة", "Occupied")
            "owned" -> text("وحدة ملك (تمليك)", "Owned")
            "rental" -> text("إيجار", "Rental")
            "maintenance" -> text("تحت أعمال الصيانة", "Under Maintenance")

            "open" -> text("مفتوح", "Open")
            "assigned" -> text("تم إسنادها للفني", "Assigned")
            "in_progress" -> text("قيد التنفيذ والمتابعة", "In Progress")
            "resolved" -> text("تم الحل والمعالجة", "Resolved")
            "closed" -> text("مغلق", "Closed")
            "active" -> text("نشط وسارٍ", "Active")
            "ended" -> text("منتهٍ", "Ended")
            "draft" -> text("مسودة غير معتمدة", "Draft")
            "issued" -> text("صادرة للمطالبة", "Issued")
            "paid" -> text("مدفوعة بالكامل", "Paid")
            "pending" -> text("قيد المراجعة والتدقيق", "Pending")
            "approved" -> text("معتمد ومسدد", "Approved")
            "rejected" -> text("مرفوض", "Rejected")
            "exempted" -> text("معفى بقرار إداري", "Exempted")
            "cancelled" -> text("ملغى", "Cancelled")

            "normal" -> text("عادي", "Normal")
            "low" -> text("منخفض", "Low")
            "high" -> text("مرتفع", "High")
            "urgent" -> text("عاجل وطارئ", "Urgent")
            "critical" -> text("حرج للغاية", "Critical")

            "technical" -> text("دعم فني وتقني", "Technical")
            "billing" -> text("فواتير ومدفوعات", "Billing")
            "accounting" -> text("استفسار مالي ومحاسبي", "Accounting")
            "security" -> text("أمن وحراسة", "Security")
            "general" -> text("استفسار عام", "General")

            "monthly" -> text("شهري", "Monthly")
            "quarterly" -> text("ربع سنوي (كل 3 أشهر)", "Quarterly")
            "yearly" -> text("سنوي", "Yearly")
            "annual" -> text("سنوي", "Annual")
            "one_time" -> text("مرة واحدة", "One-Time")

            else -> k.replace('_', ' ')
        }
    }
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
        token = getPreferences(0).getString("session_token", "") ?: ""
        if (token.isNotEmpty()) {
            scaffold(text("جاري التحميل...", "Loading..."))
            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("جاري مزامنة مساحة العمل وتحديث البيانات...", "Syncing workspace data...")
                    textSize = 13.5f
                    typeface = cairoFont
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                    setPadding(0, dp(32), 0, dp(32))
                })
            }
            loadMe()
        } else {
            login()
        }
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
                textSize = 13f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                isAllCaps = false
                includeFontPadding = false
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = roundedDrawable(Color.TRANSPARENT, 10f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(6), dp(10), dp(6))
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
        val langParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) }
        actionsGroup.addView(langBtn, langParams)

        val themeBtn = Button(this).apply {
            text = if (dark) "☀" else "☾"
            textSize = 14f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#FCD34D" else "#1E293B"))
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(6), dp(10), dp(6))
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
        actionsGroup.addView(themeBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

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
            textSize = 13.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            setOnClickListener { fn() }
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedDrawable(
                Color.TRANSPARENT,
                12f,
                Color.parseColor(if (dark) "#334155" else "#CBD5E1")
            )
            setOnClickListener { fn() }
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
        if (isFinishing || isDestroyed) return
        try {
            AlertDialog.Builder(this)
                .setTitle(text("تنبيه", "Notice"))
                .setMessage(e.message ?: text("حدث خطأ غير متوقع", "An unexpected error occurred"))
                .setPositiveButton(text("حسناً", "OK"), null)
                .show()
        } catch (_: Exception) {}
    }

    private fun call(path: String, method: String = "GET", data: JSONObject? = null, onError: ((Exception) -> Unit)? = null, done: (String) -> Unit) {
        worker.execute {
            try {
                val hasOrg = org.isNotEmpty() && !listOf("auth/", "me").any { path.startsWith(it) && !path.startsWith("me/profile") }
                val urlStr = base + "/api/" + path + (if (hasOrg) (if (path.contains('?')) "&" else "?") + "org=" + org else "")
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                try {
                    if (method == "PATCH") {
                        try {
                            connection.requestMethod = "PATCH"
                        } catch (_: Exception) {
                            connection.requestMethod = "POST"
                            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                        }
                    } else {
                        connection.requestMethod = method
                    }
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-TowerFlow-Client", "android")
                    if (method == "PATCH") {
                        connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                    }
                    if (token.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")
                    if (data != null) {
                        connection.doOutput = true
                        connection.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val code = connection.responseCode
                    if (code == 401) {
                        token = ""
                        getPreferences(0).edit().remove("session_token").apply()
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                login()
                            }
                        }
                        return@execute
                    }
                    val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                    if (code !in 200..299) {
                        val errMsg = try {
                            val json = JSONObject(raw)
                            val e = json.optString("error", "")
                            if (e.isNotEmpty()) e else json.optString("message", "HTTP $code")
                        } catch (_: Exception) {
                            "HTTP $code"
                        }
                        throw IllegalStateException(errMsg)
                    }
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            try { done(raw) } catch (e: Exception) { onError?.invoke(e) ?: error(e) }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        onError?.invoke(e) ?: error(e)
                    }
                }
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
            textSize = 14f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
                        getPreferences(0).edit().putString("session_token", token).apply()
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
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = roundedDrawable(
                if (dark) Color.parseColor("#151824") else Color.WHITE,
                12f,
                if (dark) Color.parseColor("#27324A") else Color.parseColor("#CBD5E1")
            )
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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
                registerPush()
                if (pendingNotifications) {
                    pendingNotifications = false
                    listing("notifications")
                } else {
                    home()
                }
            } else {
                newOrgPrompt()
            }
        }
    }

    private fun newOrgPrompt() {
        scaffold(text("إنشاء مساحة العمل الأولى", "Create First Workspace"))
        card {
            addView(TextView(this@MainActivity).apply {
                text = text("مرحبًا بك في TowerFlow!", "Welcome to TowerFlow!")
                textSize = 17f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            })
            addView(TextView(this@MainActivity).apply {
                text = text("ابدأ بتسمية مؤسستك أو برجك السكني لتفعيل حسابك وإدارته بسهولة.", "Start by naming your organization or residential tower to activate your workspace.")
                textSize = 12.5f
                typeface = cairoFont
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, dp(6), 0, dp(14))
            })
        }
        val orgInput = input(text("اسم المؤسسة أو البرج", "Organization / Tower Name"))
        primaryButton(text("إنشاء والبدء الآن", "Create Workspace")) {
            val name = orgInput.text.toString().trim()
            if (name.isEmpty()) {
                error(IllegalStateException(text("يرجى إدخال اسم المؤسسة", "Please enter a workspace name")))
                return@primaryButton
            }
            call("organizations", "POST", JSONObject().put("name", name)) {
                loadMe()
            }
        }
    }

    private fun hubGrid(items: List<Pair<String, Pair<String, String>>>): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        for (chunk in items.chunked(2)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(4)
                    bottomMargin = dp(4)
                }
                weightSum = 2f
            }
            for ((index, item) in chunk.withIndex()) {
                val key = item.first
                val glyph = item.second.first
                val titleText = item.second.second
                val pill = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = roundedDrawable(
                        if (dark) Color.parseColor("#17243B") else Color.parseColor("#F8FAFC"),
                        12f,
                        if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
                    )
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index == 0 && chunk.size == 2) marginEnd = dp(4)
                        else if (index == 1) marginStart = dp(4)
                    }
                    layoutParams = lp
                    minimumHeight = dp(50)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { listing(key) }
                    addView(TextView(this@MainActivity).apply {
                        text = glyph
                        textSize = 16f
                        includeFontPadding = false
                        setPadding(if (arabic) dp(6) else 0, 0, if (arabic) 0 else dp(6), 0)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = titleText
                        textSize = 12f
                        maxLines = 2
                        includeFontPadding = false
                        ellipsize = TextUtils.TruncateAt.END
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#1E293B"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                }
                row.addView(pill)
            }
            if (chunk.size == 1) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
                })
            }
            container.addView(row)
        }
        return container
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
                    "admin" -> text("مدير النظام", "System Admin")
                    "manager" -> text("مدير البرج", "Building Manager")
                    "accountant" -> text("المحاسب المالي", "Accountant")
                    "technician" -> text("فني الصيانة", "Technician")
                    "resident" -> text("ساكن / مالك / مستأجر", "Resident")
                    "viewer" -> text("مراقب ومستعرض", "Viewer")
                    else -> optionTitle(role)
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
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
            setOnClickListener { if (role == "resident") proofForm() else listing("invoices") }
        }
        quickGrid.addView(q1)

        val q2 = Button(this).apply {
            text = text("🔧 بلاغ صيانة", "Ticket")
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundedDrawable(
                Color.TRANSPARENT,
                12f,
                Color.parseColor(if (dark) "#334155" else "#CBD5E1")
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
            setOnClickListener { ticketForm() }
        }
        quickGrid.addView(q2)
        screen.addView(quickGrid)

        // Organized Hub Cards (Simplified for Superior Mobile Usability)
        if (role != "resident") {
            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("🏢 المحفظة العقارية", "Property & Units")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "properties" to Pair("🏢", title("properties")),
                    "units" to Pair("🚪", title("units")),
                    "parties" to Pair("👥", title("parties")),
                    "leases" to Pair("📑", title("leases"))
                )))
            }

            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("💳 المالية والتحصيل", "Finance & Cash Flow")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "invoices" to Pair("💳", title("invoices")),
                    "receipts" to Pair("🧾", title("receipts")),
                    "payment_proofs" to Pair("📤", title("payment_proofs")),
                    "money_accounts" to Pair("🏦", title("money_accounts")),
                    "expenses" to Pair("📉", title("expenses")),
                    "fines" to Pair("⚠️", title("fines"))
                )))
            }

            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("🛠️ الصيانة والتشغيل", "Maintenance & Operations")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "tickets" to Pair("🛠️", title("tickets")),
                    "technicians" to Pair("👷", title("technicians")),
                    "assets" to Pair("🛗", title("assets")),
                    "vendor_contracts" to Pair("📝", title("vendor_contracts"))
                )))
            }

            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("📢 التواصل والدعم", "Workspace & Support")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "announcements" to Pair("📢", title("announcements")),
                    "notifications" to Pair("🔔", title("notifications")),
                    "support_tickets" to Pair("🎫", title("support_tickets")),
                    "audit_logs" to Pair("📜", title("audit_logs"))
                )))
            }
        } else {
            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("🏠 وحدتي وعقدي السكني", "My Home & Lease")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "units" to Pair("🚪", title("units")),
                    "leases" to Pair("📑", title("leases"))
                )))
            }

            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("💳 فواتيري وإثباتات السداد", "Invoices & Payments")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "invoices" to Pair("💳", title("invoices")),
                    "payment_proofs" to Pair("📤", title("payment_proofs")),
                    "credit_balances" to Pair("💰", title("credit_balances")),
                    "fines" to Pair("⚠️", title("fines"))
                )))
            }

            card {
                addView(TextView(this@MainActivity).apply {
                    text = text("🛠️ الصيانة والتواصل", "Maintenance & Notices")
                    textSize = 13.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                    setPadding(0, 0, 0, dp(6))
                })
                addView(hubGrid(listOf(
                    "tickets" to Pair("🛠️", title("tickets")),
                    "announcements" to Pair("📢", title("announcements")),
                    "notifications" to Pair("🔔", title("notifications")),
                    "support_tickets" to Pair("🎫", title("support_tickets"))
                )))
            }
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

        val relationMap = mapOf(
            "units" to listOf("properties"),
            "leases" to listOf("units", "parties", "properties"),
            "ownerships" to listOf("units", "parties", "properties"),
            "invoices" to listOf("units", "parties", "properties"),
            "payment_proofs" to listOf("units", "parties", "properties"),
            "fines" to listOf("units", "parties", "properties"),
            "tickets" to listOf("units", "technicians"),
            "assets" to listOf("properties"),
            "money_accounts" to listOf("properties"),
            "vendor_contracts" to listOf("properties", "parties"),
            "budgets" to listOf("properties"),
            "credit_balances" to listOf("parties", "properties", "units")
        )
        val neededRels = relationMap[entity] ?: emptyList()

        // Search Bar & Counter Header
        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            }
        }
        val searchInput = EditText(this).apply {
            hint = text("🔍 ابحث في السجلات...", "🔍 Search records...")
            textSize = 13f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            background = roundedDrawable(if (dark) Color.parseColor("#11131A") else Color.WHITE, 12f, Color.parseColor(if (dark) "#1E2330" else "#E2E8F0"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) }
        }
        val countBadge = TextView(this).apply {
            text = text("0 سجل", "0 items")
            textSize = 11.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor("#4F46E5"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedDrawable(Color.parseColor(if (dark) "#1E1B4B" else "#EEF2FF"), 10f)
        }
        searchRow.addView(searchInput)
        searchRow.addView(countBadge)
        screen.addView(searchRow)

        val recordsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        screen.addView(recordsContainer)

        fun renderCards(records: List<JSONObject>, relCaches: Map<String, Map<String, String>>) {
            recordsContainer.removeAllViews()
            countBadge.text = "${records.size} " + text("سجل", "items")

            if (records.isEmpty()) {
                val emptyCard = card {
                    addView(TextView(this@MainActivity).apply {
                        text = text("لا توجد سجلات مطابقة في هذا القسم", "No matching records found")
                        textSize = 13f
                        typeface = cairoFont
                        setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    })
                }
                recordsContainer.addView(emptyCard)
                return
            }

            for (i in records.indices) {
                val row = records[i]
                val propName = relCaches["properties"]?.get(row.optString("property_id")) ?: ""
                val unitName = relCaches["units"]?.get(row.optString("unit_id")) ?: ""
                val partyName = relCaches["parties"]?.get(row.optString("party_id")) ?: ""
                val vendorName = relCaches["parties"]?.get(row.optString("vendor_id")) ?: ""
                val techName = relCaches["technicians"]?.get(row.optString("technician_id")) ?: ""

                val heading = when (entity) {
                    "leases" -> when {
                        unitName.isNotEmpty() && partyName.isNotEmpty() -> text("عقد: $unitName · $partyName", "Lease: $unitName · $partyName")
                        unitName.isNotEmpty() -> text("عقد: $unitName", "Lease: $unitName")
                        else -> text("عقد إيجار #${i + 1}", "Lease #${i + 1}")
                    }
                    "ownerships" -> when {
                        unitName.isNotEmpty() && partyName.isNotEmpty() -> text("ملكية: $unitName · $partyName", "Ownership: $unitName · $partyName")
                        unitName.isNotEmpty() -> text("ملكية: $unitName", "Ownership: $unitName")
                        else -> text("سجل ملكية #${i + 1}", "Ownership #${i + 1}")
                    }
                    "units" -> text("شقة / وحدة ", "Unit ") + row.optString("name", row.optString("id").take(6))
                    "invoices" -> text("فاتورة #", "Invoice #") + row.optString("number", "${i + 1}")
                    "payment_proofs" -> text("إثبات سداد: ", "Payment Proof: ") + (if (unitName.isNotEmpty()) unitName else row.optString("reference", "#${i + 1}"))
                    "fines" -> text("غرامة: ", "Fine: ") + row.optString("title", "#${i + 1}")
                    "vendor_contracts" -> text("عقد: ", "Contract: ") + row.optString("title")
                    "budgets" -> text("موازنة: ", "Budget: ") + row.optString("name")
                    "support_tickets" -> {
                        val num = row.optString("ticket_number")
                        (if (num.isNotEmpty()) "$num · " else "") + row.optString("title")
                    }
                    else -> listOf("name", "title", "number", "description", "message", "entity", "reference", "reason")
                        .map { row.optString(it) }
                        .firstOrNull { it.isNotEmpty() } ?: text("سجل #${i + 1}", "Record #${i + 1}")
                }
                val status = row.optString("status", "")

                val itemCard = card {
                    // Top row: Title and Status badge
                    val topRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    topRow.addView(TextView(this@MainActivity).apply {
                        text = heading
                        textSize = 15f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })

                    if (status.isNotEmpty()) {
                        val badge = TextView(this@MainActivity).apply {
                            text = when (status) {
                                "active" -> text("نشط وسارٍ", "Active")
                                "issued" -> text("صادرة للمطالبة", "Issued")
                                "paid" -> text("مدفوعة بالكامل", "Paid")
                                "draft" -> text("مسودة", "Draft")
                                "pending" -> text("قيد المراجعة", "Pending")
                                "approved" -> text("معتمد ومسدد", "Approved")
                                "rejected" -> text("مرفوض", "Rejected")
                                "exempted" -> text("معفى مسبب", "Exempted")
                                "open" -> text("مفتوح", "Open")
                                "closed" -> text("مغلق", "Closed")
                                "vacant" -> text("شاغرة وغير مشغولة", "Vacant")
                                "occupied" -> text("مؤجرة / مشغولة", "Rented")
                                "owned" -> text("وحدة ملك (تمليك)", "Owned")
                                "maintenance" -> text("صيانة", "Maintenance")
                                else -> optionTitle(status)
                            }
                            textSize = 10.5f
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
                    if (propName.isNotEmpty()) detailsList.add(Pair(text("العقار / البرج:", "Property:"), propName))
                    if (unitName.isNotEmpty()) detailsList.add(Pair(text("الوحدة السكنية:", "Unit:"), unitName))
                    if (partyName.isNotEmpty()) detailsList.add(Pair(text("الطرف المعني:", "Resident / Party:"), partyName))
                    if (techName.isNotEmpty()) detailsList.add(Pair(text("الفني المعتمد:", "Technician:"), techName))
                    if (vendorName.isNotEmpty()) detailsList.add(Pair(text("المورد / المقاول:", "Vendor:"), vendorName))

                    if (row.has("floor") && entity == "units") detailsList.add(Pair(text("الطابق:", "Floor:"), text("الطابق ", "Floor ") + row.optString("floor")))
                    if (row.has("area") && entity == "units") detailsList.add(Pair(text("المساحة:", "Area:"), row.optString("area") + text(" م²", " m²")))
                    if (row.has("phone") && row.optString("phone").isNotEmpty()) detailsList.add(Pair(text("الهاتف:", "Phone:"), row.optString("phone")))
                    if (row.has("email") && row.optString("email").isNotEmpty()) detailsList.add(Pair(text("البريد:", "Email:"), row.optString("email")))
                    if (row.has("rent")) detailsList.add(Pair(text("قيمة الإيجار:", "Rent:"), row.optString("rent") + text(" ج.م", " EGP")))
                    if (row.has("share")) detailsList.add(Pair(text("نسبة الملكية:", "Share:"), row.optString("share") + "%"))
                    if (row.has("amount")) detailsList.add(Pair(text("المبلغ:", "Amount:"), row.optString("amount") + text(" ج.م", " EGP")))
                    if (row.has("balance")) detailsList.add(Pair(text("الرصيد المالي:", "Balance:"), row.optString("balance") + text(" ج.م", " EGP")))
                    if (row.has("start_date") && row.optString("start_date").isNotEmpty()) detailsList.add(Pair(text("تاريخ البدء:", "Start Date:"), row.optString("start_date")))
                    if (row.has("end_date") && row.optString("end_date").isNotEmpty()) detailsList.add(Pair(text("تاريخ الانتهاء:", "End Date:"), row.optString("end_date")))
                    if (row.has("due_date") && row.optString("due_date").isNotEmpty()) detailsList.add(Pair(text("تاريخ الاستحقاق:", "Due Date:"), row.optString("due_date")))
                    if (row.has("received_on") && row.optString("received_on").isNotEmpty()) detailsList.add(Pair(text("تاريخ الاستلام:", "Received On:"), row.optString("received_on")))
                    if (row.has("reference") && row.optString("reference").isNotEmpty()) detailsList.add(Pair(text("المرجع البنكي:", "Ref:"), row.optString("reference")))
                    if (row.has("payment_method") && row.optString("payment_method").isNotEmpty()) detailsList.add(Pair(text("طريقة السداد:", "Payment Method:"), optionTitle(row.optString("payment_method"))))
                    if (row.has("bank_name") && row.optString("bank_name").isNotEmpty()) detailsList.add(Pair(text("اسم البنك:", "Bank:"), row.optString("bank_name")))
                    if (row.has("account_number") && row.optString("account_number").isNotEmpty()) detailsList.add(Pair(text("رقم الحساب:", "Account No:"), row.optString("account_number")))
                    if (row.has("serial") && row.optString("serial").isNotEmpty()) detailsList.add(Pair(text("الرقم التسلسلي:", "Serial:"), row.optString("serial")))
                    if (row.has("next_service") && row.optString("next_service").isNotEmpty()) detailsList.add(Pair(text("الصيانة القادمة:", "Next Service:"), row.optString("next_service")))
                    if (row.has("specialty") && row.optString("specialty").isNotEmpty()) detailsList.add(Pair(text("التخصص المهني:", "Specialty:"), row.optString("specialty")))
                    if (row.has("service_type") && row.optString("service_type").isNotEmpty()) detailsList.add(Pair(text("نوع الخدمة:", "Service:"), row.optString("service_type")))
                    if (row.has("frequency") && row.optString("frequency").isNotEmpty()) detailsList.add(Pair(text("الدورية:", "Frequency:"), optionTitle(row.optString("frequency"))))
                    if (row.has("period") && row.optString("period").isNotEmpty()) detailsList.add(Pair(text("الفترة:", "Period:"), optionTitle(row.optString("period"))))
                    if (row.has("destination") && row.optString("destination").isNotEmpty()) detailsList.add(Pair(text("الصندوق المحول إليه:", "Fund:"), optionTitle(row.optString("destination"))))
                    if (row.has("priority") && row.optString("priority").isNotEmpty()) detailsList.add(Pair(text("الأولوية:", "Priority:"), optionTitle(row.optString("priority"))))
                    if (row.has("category") && row.optString("category").isNotEmpty()) detailsList.add(Pair(text("التصنيف:", "Category:"), optionTitle(row.optString("category"))))
                    if (row.has("ownership_type") && row.optString("ownership_type").isNotEmpty()) detailsList.add(Pair(text("نوع الملكية:", "Ownership:"), optionTitle(row.optString("ownership_type"))))
                    if (row.has("kind") && entity in listOf("parties", "accounts", "money_accounts")) detailsList.add(Pair(text("النوع:", "Type:"), optionTitle(row.optString("kind"))))
                    if (row.has("body") && row.optString("body").isNotEmpty()) detailsList.add(Pair(text("البيان:", "Content:"), row.optString("body")))
                    if (row.has("description") && entity !in listOf("invoices", "expenses") && row.optString("description").isNotEmpty()) detailsList.add(Pair(text("الوصف:", "Description:"), row.optString("description")))
                    if (row.has("action") && row.optString("action").isNotEmpty()) detailsList.add(Pair(text("الإجراء:", "Action:"), row.optString("action")))
                    if (row.has("created_at") && row.optString("created_at").isNotEmpty()) detailsList.add(Pair(text("التاريخ:", "Date:"), row.optString("created_at").take(10)))

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
                                layoutParams = LinearLayout.LayoutParams(dp(95), -2)
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

                    // Action buttons
                    val actionsRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                    }

                    if (entity in forms.keys && entity !in listOf("invoices", "expenses", "payment_proofs", "fines") && role in listOf("admin", "manager", "accountant", "technician")) {
                        val editBtn = Button(this@MainActivity).apply {
                            text = text("تعديل", "Edit")
                            textSize = 11.5f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                            isAllCaps = false
                            includeFontPadding = false
                            minHeight = 0
                            minimumHeight = 0
                            setPadding(dp(4), dp(8), dp(4), dp(8))
                            background = roundedDrawable(Color.TRANSPARENT, 8f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
                            setOnClickListener { recordForm(entity, row) }
                        }
                        actionsRow.addView(editBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                    }

                    // Payment Proofs Review (Approve / Reject)
                    if (entity == "payment_proofs" && row.optString("status") == "pending" && role in listOf("admin", "manager")) {
                        val approveBtn = Button(this@MainActivity).apply {
                            text = text("اعتماد السداد", "Approve")
                            textSize = 11.5f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.WHITE)
                            isAllCaps = false
                            includeFontPadding = false
                            minHeight = 0
                            minimumHeight = 0
                            setPadding(dp(4), dp(8), dp(4), dp(8))
                            background = roundedDrawable(Color.parseColor("#059669"), 8f)
                            setOnClickListener {
                                call("payment_proofs/" + row.optString("id") + "/review", "POST", JSONObject().put("decision", "approved")) { listing(entity) }
                            }
                        }
                        actionsRow.addView(approveBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })

                        val rejectBtn = Button(this@MainActivity).apply {
                            text = text("رفض", "Reject")
                            textSize = 11.5f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor("#E11D48"))
                            isAllCaps = false
                            includeFontPadding = false
                            minHeight = 0
                            minimumHeight = 0
                            setPadding(dp(4), dp(8), dp(4), dp(8))
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
                                        call("payment_proofs/" + row.optString("id") + "/review", "POST", JSONObject().put("decision", "rejected").put("rejection_reason", r)) { listing(entity) }
                                    }
                                    .setNegativeButton(text("إلغاء", "Cancel"), null)
                                    .show()
                            }
                        }
                        actionsRow.addView(rejectBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                    }

                    // Fines Exemption
                    if (entity == "fines" && row.optString("status") == "pending" && role in listOf("admin", "manager")) {
                        val exemptBtn = Button(this@MainActivity).apply {
                            text = text("إعفاء مسبب", "Exempt")
                            textSize = 11.5f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor("#D97706"))
                            isAllCaps = false
                            includeFontPadding = false
                            minHeight = 0
                            minimumHeight = 0
                            setPadding(dp(4), dp(8), dp(4), dp(8))
                            background = roundedDrawable(Color.parseColor("#FEF3C7"), 8f)
                            setOnClickListener { fineExemptForm(row.optString("id")) }
                        }
                        actionsRow.addView(exemptBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                    }

                    // Invoices Issue & Collect
                    if (entity == "invoices" && role in listOf("admin", "manager", "accountant")) {
                        val id = row.optString("id")
                        if (row.optString("status") == "draft") {
                            val issueBtn = Button(this@MainActivity).apply {
                                text = text("إصدار وترحيل", "Issue & Post")
                                textSize = 11.5f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.WHITE)
                                isAllCaps = false
                                includeFontPadding = false
                                minHeight = 0
                                minimumHeight = 0
                                setPadding(dp(4), dp(8), dp(4), dp(8))
                                background = roundedDrawable(Color.parseColor("#4F46E5"), 8f)
                                setOnClickListener { call("invoices/$id/issue", "POST", JSONObject()) { listing(entity) } }
                            }
                            actionsRow.addView(issueBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                        }
                        if (row.optString("status") == "issued") {
                            val collectBtn = Button(this@MainActivity).apply {
                                text = text("تسجيل تحصيل", "Collect")
                                textSize = 11.5f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.WHITE)
                                isAllCaps = false
                                includeFontPadding = false
                                minHeight = 0
                                minimumHeight = 0
                                setPadding(dp(4), dp(8), dp(4), dp(8))
                                background = roundedDrawable(Color.parseColor("#059669"), 8f)
                                setOnClickListener { collectForm(id) }
                            }
                            actionsRow.addView(collectBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                        }
                    }

                    if (actionsRow.childCount > 0) {
                        addView(actionsRow)
                    }
                }
                recordsContainer.addView(itemCard)
            }
        }

        fun fetchAndRender(relCaches: Map<String, Map<String, String>>) {
            call(entity) { raw ->
                val jsonArr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
                val allList = (0 until jsonArr.length()).map { jsonArr.getJSONObject(it) }

                fun applyFilter() {
                    val q = searchInput.text.toString().trim().lowercase()
                    val filtered = if (q.isEmpty()) allList else allList.filter { r ->
                        r.toString().lowercase().contains(q)
                    }
                    renderCards(filtered, relCaches)
                }

                searchInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilter() }
                    override fun afterTextChanged(s: Editable?) {}
                })

                applyFilter()
            }
        }

        if (neededRels.isEmpty()) {
            fetchAndRender(emptyMap())
        } else {
            val relCaches = mutableMapOf<String, Map<String, String>>()
            var remaining = neededRels.size
            neededRels.forEach { rel ->
                call(rel, onError = {
                    remaining--
                    relCaches[rel] = emptyMap()
                    if (remaining == 0) fetchAndRender(relCaches)
                }) { raw ->
                    val rows = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
                    val map = mutableMapOf<String, String>()
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONObject(i)
                        val id = row.optString("id")
                        val label = when {
                            row.has("name") && row.optString("name").isNotEmpty() -> row.optString("name")
                            row.has("unit_number") && row.optString("unit_number").isNotEmpty() -> row.optString("unit_number")
                            row.has("title") && row.optString("title").isNotEmpty() -> row.optString("title")
                            row.has("ticket_number") && row.optString("ticket_number").isNotEmpty() -> row.optString("ticket_number")
                            else -> id
                        }
                        if (id.isNotEmpty()) map[id] = label
                    }
                    relCaches[rel] = map
                    remaining--
                    if (remaining == 0) fetchAndRender(relCaches)
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
                typeface = cairoFont
                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                setPadding(0, 0, 0, dp(12))
            })
        }

        val amount = input(text("المبلغ المحول", "Transferred Amount")).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val reference = input(text("رقم الحوالة / المرجع البنكي", "Transaction Reference"))

        call("properties") { propRaw ->
            val props = try { JSONArray(propRaw) } catch (_: Exception) { JSONArray() }
            val defaultPropId = if (props.length() > 0) props.getJSONObject(0).optString("id") else ""

            call("units") { raw ->
                val units = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
                val unitSpinner = Spinner(this)
                val unitNames = (0 until units.length()).map {
                    val u = units.getJSONObject(it)
                    u.optString("name", u.optString("unit_number", u.optString("id")))
                }
                unitSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (unitNames.isEmpty()) listOf(text("لا توجد وحدات متاحة", "No units available")) else unitNames)
                label(text("الوحدة السكنية", "Unit"), 13, true)
                screen.addView(unitSpinner)

                call("parties", onError = { /* residents have restricted access to parties table */ }) { partyRaw ->
                    val parties = try { JSONArray(partyRaw) } catch (_: Exception) { JSONArray() }
                    val partySpinner = if (parties.length() > 0) Spinner(this) else null
                    if (partySpinner != null) {
                        partySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until parties.length()).map {
                            val p = parties.getJSONObject(it)
                            p.optString("name", p.optString("id"))
                        })
                        label(text("الساكن / المسدد", "Resident / Payer"), 13, true)
                        screen.addView(partySpinner)
                    }

                    primaryButton(text("إرسال الإثبات", "Submit Proof")) {
                        val uId = if (units.length() > 0 && unitSpinner.selectedItemPosition in 0 until units.length()) units.getJSONObject(unitSpinner.selectedItemPosition).optString("id") else ""
                        val pId = if (partySpinner != null && parties.length() > 0 && partySpinner.selectedItemPosition in 0 until parties.length()) parties.getJSONObject(partySpinner.selectedItemPosition).optString("id") else ""
                        val pUnit = if (units.length() > 0 && unitSpinner.selectedItemPosition in 0 until units.length()) units.getJSONObject(unitSpinner.selectedItemPosition) else null
                        val propId = if (pUnit != null && pUnit.has("property_id")) pUnit.optString("property_id") else defaultPropId

                        val payload = JSONObject()
                            .put("property_id", propId)
                            .put("unit_id", uId)
                            .put("amount", amount.text.toString().trim())
                            .put("reference", reference.text.toString().trim())
                            .put("payment_method", "bank_transfer")

                        if (pId.isNotEmpty()) payload.put("party_id", pId)

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
            val units = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until units.length()).map {
                val u = units.getJSONObject(it)
                u.optString("name", u.optString("unit_number", u.optString("id")))
            })
            primaryButton(text("إرسال البلاغ", "Submit Ticket")) {
                val payload = JSONObject().put("title", heading.text.toString().trim()).put("description", description.text.toString().trim())
                if (units.length() > 0 && spinner.selectedItemPosition in 0 until units.length()) {
                    payload.put("unit_id", units.getJSONObject(spinner.selectedItemPosition).optString("id"))
                }
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
        "account_number" to Pair("رقم الحساب", "Account Number"),
        "reference" to Pair("رقم المرجع / الحوالة", "Reference"),
        "payment_method" to Pair("طريقة السداد", "Payment Method"),
        "rejection_reason" to Pair("سبب الرفض", "Rejection Reason"),
        "decision" to Pair("القرار الإداري", "Decision"),
        "amount_to_pay" to Pair("المبلغ المستحق للدفع", "Amount to Pay"),
        "balance" to Pair("الرصيد المتبقي", "Remaining Balance"),
        "entry_date" to Pair("تاريخ القيد", "Entry Date"),
        "debit" to Pair("مدين", "Debit"),
        "credit" to Pair("دائن", "Credit"),
        "received_on" to Pair("تاريخ الاستلام", "Received On"),
        "organization" to Pair("المنشأة أو المؤسسة", "Organization"),
        "floors_count" to Pair("عدد الطوابق", "Floors Count"),
        "units_per_floor" to Pair("عدد الوحدات بكل طابق", "Units Per Floor"),
        "prefix" to Pair("بادئة الترقيم", "Prefix"),
        "invoice_id" to Pair("رقم الفاتورة", "Invoice ID")
    )

    private fun fieldTitle(k: String) = fieldNames[k]?.let { if (arabic) it.first else it.second } ?: k.replace('_', ' ')

    private fun recordForm(entity: String, record: JSONObject? = null) {
        val keys = forms[entity] ?: return
        scaffold(title(entity), true) { listing(entity) }

        val relationMap = mapOf(
            "property_id" to "properties",
            "unit_id" to "units",
            "party_id" to "parties",
            "vendor_id" to "parties",
            "asset_id" to "assets",
            "technician_id" to "technicians"
        )
        val neededRels = keys.mapNotNull { relationMap[it] }.distinct()

        fun renderForm(relData: Map<String, List<Pair<String, String>>>) {
            val values = linkedMapOf<String, () -> String>()

            keys.forEach { key ->
                val initial = if (record?.isNull(key) == false) record.optString(key) else ""
                val relEntity = relationMap[key]

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

                if (relEntity != null) {
                    val items = relData[relEntity] ?: emptyList()
                    label(fieldTitle(key), 13, true)
                    if (items.isEmpty()) {
                        val notice = TextView(this).apply {
                            text = text("⚠️ لا توجد عناصر مسجلة بعد في هذا القسم. يرجى إضافة سجل أولاً.", "⚠️ No records available yet. Please add a record first.")
                            textSize = 12f
                            typeface = cairoFont
                            setTextColor(Color.parseColor("#E11D48"))
                            setPadding(dp(4), dp(4), dp(4), dp(8))
                        }
                        screen.addView(notice)
                        values[key] = { "" }
                    } else {
                        val spinner = Spinner(this)
                        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.second })
                        val defaultIdx = if (initial.isNotEmpty()) {
                            items.indexOfFirst { it.first == initial }.coerceAtLeast(0)
                        } else {
                            0
                        }
                        spinner.setSelection(defaultIdx)
                        screen.addView(spinner)
                        values[key] = { items.getOrNull(spinner.selectedItemPosition)?.first ?: "" }
                    }
                } else if (options.isNotEmpty()) {
                    label(fieldTitle(key), 13, true)
                    val spinner = Spinner(this)
                    spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options.map { optionTitle(it) })
                    val defaultIdx = if (initial.isNotEmpty()) options.indexOf(initial).coerceAtLeast(0) else 0
                    spinner.setSelection(defaultIdx)
                    screen.addView(spinner)
                    values[key] = { options.getOrNull(spinner.selectedItemPosition) ?: "" }
                } else {
                    val control = input(fieldTitle(key)).apply {
                        setText(initial)
                        if (key in listOf("amount", "rent", "area", "share", "floor")) {
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                        }
                    }
                    values[key] = { control.text.toString().trim() }
                }
            }

            primaryButton(text(if (record == null) "حفظ السجل الجديد" else "حفظ التعديلات", if (record == null) "Save New Record" else "Save Changes")) {
                val payload = JSONObject()
                values.forEach { (key, getter) ->
                    val value = getter()
                    if (value.isNotBlank()) payload.put(key, value)
                }
                call(entity + (record?.let { "/" + it.optString("id") } ?: ""), if (record == null) "POST" else "PATCH", payload) {
                    listing(entity)
                }
            }
        }

        if (neededRels.isEmpty()) {
            renderForm(emptyMap())
        } else {
            val relResults = mutableMapOf<String, List<Pair<String, String>>>()
            var remaining = neededRels.size
            neededRels.forEach { rel ->
                call(rel, onError = {
                    remaining--
                    relResults[rel] = emptyList()
                    if (remaining == 0) renderForm(relResults)
                }) { raw ->
                    val rows = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
                    val items = (0 until rows.length()).map { idx ->
                        val row = rows.getJSONObject(idx)
                        val lbl = when {
                            row.has("name") && row.optString("name").isNotEmpty() -> row.optString("name")
                            row.has("unit_number") && row.optString("unit_number").isNotEmpty() -> row.optString("unit_number")
                            row.has("title") && row.optString("title").isNotEmpty() -> row.optString("title")
                            row.has("ticket_number") && row.optString("ticket_number").isNotEmpty() -> row.optString("ticket_number")
                            else -> row.optString("id")
                        }
                        row.optString("id") to lbl
                    }
                    relResults[rel] = items
                    remaining--
                    if (remaining == 0) renderForm(relResults)
                }
            }
        }
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
            getPreferences(0).edit().putString("session_token", token).apply()
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
            fun doClear() {
                token = ""
                org = ""
                getPreferences(0).edit().remove("session_token").apply()
                getSystemService(NotificationManager::class.java).cancelAll()
                login()
            }
            call("auth/logout", "POST", JSONObject(), onError = { doClear() }) { doClear() }
        }
        if (device != null) call("push/devices/remove", "POST", JSONObject().put("token", device)) { finishLogout() } else finishLogout()
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdownNow()
    }
}
