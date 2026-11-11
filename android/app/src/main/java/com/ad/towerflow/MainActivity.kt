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
import android.view.ViewGroup
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
        "owned" to Pair("وحدة تمليك", "Owned"),
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

            "vacant" -> text("مغلقة 🟡", "Closed 🟡")
            "occupied" -> text("مشغولة ومقيمة 🟢", "Residing 🟢")
            "owned" -> text("تمليك", "Owned")
            "rental" -> text("إيجار", "Rental")
            "maintenance" -> text("تحت التشطيب 🔵", "Under Finishing 🔵")
            "residing" -> text("مشغولة ومقيمة 🟢", "Residing 🟢")
            "closed" -> text("مغلقة 🟡", "Closed 🟡")
            "finishing" -> text("تحت التشطيب 🔵", "Finishing 🔵")

            "open" -> text("مفتوح", "Open")
            "assigned" -> text("تم إسنادها للفني", "Assigned")
            "in_progress" -> text("قيد التنفيذ والمتابعة", "In Progress")
            "resolved" -> text("تم الحل والمعالجة", "Resolved")
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

    private fun buildBottomNav(currentHeading: String, navBarHeight: Int): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            weightSum = 5f
            layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            background = roundedDrawable(
                if (dark) Color.parseColor("#0C0E14") else Color.WHITE,
                0f,
                if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
            )
            val bottomPad = if (navBarHeight > 0) navBarHeight + dp(4) else dp(10)
            setPadding(dp(4), dp(8), dp(4), bottomPad)
        }

        data class NavTab(val id: String, val icon: String, val titleAr: String, val titleEn: String, val action: () -> Unit)

        val isResident = role == "resident"
        val tabs = if (isResident) {
            listOf(
                NavTab("home", "🏠", "الرئيسية", "Home") { home() },
                NavTab("invoices", "⚡", "مستحقاتي", "Invoices") { listing("invoices") },
                NavTab("tickets", "🛠️", "الصيانة", "Tickets") { listing("tickets") },
                NavTab("announcements", "📢", "الإعلانات", "News") { listing("announcements") },
                NavTab("profile", "👤", "حسابي", "Profile") { profile() }
            )
        } else {
            listOf(
                NavTab("collections", "⚡", "التحصيل", "Collect") { monthlyCollections() },
                NavTab("properties", "🏢", "الأبراج", "Towers") { manageTowers() },
                NavTab("units", "🚪", "الشقق", "Units") { listing("units") },
                NavTab("expenses", "💸", "المصروفات", "Expenses") { listing("expenses") },
                NavTab("tickets", "🛠️", "الصيانة", "Tickets") { listing("tickets") }
            )
        }

        fun isTabActive(tabId: String): Boolean {
            val h = currentHeading.lowercase()
            return when (tabId) {
                "collections" -> h.contains("تحصيل") || h.contains("collection")
                "properties" -> h.contains("أبراج") || h.contains("عمارات") || h.contains("tower") || h.contains("propert")
                "units" -> h.contains("شقق") || h.contains("وحدات") || h.contains("unit")
                "expenses" -> h.contains("مصروف") || h.contains("expense")
                "tickets" -> h.contains("صيان") || h.contains("تذكر") || h.contains("ticket") || h.contains("mainten")
                "invoices" -> h.contains("مستحق") || h.contains("فواتير") || h.contains("invoice")
                "announcements" -> h.contains("إعلان") || h.contains("announc")
                "profile" -> h.contains("ملف") || h.contains("حساب") || h.contains("profile")
                "home" -> h.contains("لوحة") || h.contains("dash") || h.contains("home")
                else -> false
            }
        }

        for (tab in tabs) {
            val active = isTabActive(tab.id)
            val tabView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(2), dp(4), dp(2), dp(4))
                isClickable = true
                isFocusable = true
                if (active) {
                    background = roundedDrawable(
                        if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"),
                        10f
                    )
                }

                val iconTv = TextView(this@MainActivity).apply {
                    text = tab.icon
                    textSize = 17f
                    gravity = Gravity.CENTER
                }
                addView(iconTv)

                val labelTv = TextView(this@MainActivity).apply {
                    text = text(tab.titleAr, tab.titleEn)
                    textSize = 10f
                    typeface = Typeface.create(cairoFont, if (active) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(Color.parseColor(if (active) (if (dark) "#818CF8" else "#4F46E5") else (if (dark) "#64748B" else "#94A3B8")))
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, dp(2), 0, 0)
                }
                addView(labelTv)

                setOnClickListener {
                    tab.action()
                }
            }
            nav.addView(tabView)
        }

        return nav
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

        if (token.isNotEmpty()) {
            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.parseColor(if (dark) "#090A0F" else "#F8FAFC"))
            }
            scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            rootLayout.addView(scroll)
            rootLayout.addView(buildBottomNav(heading, navBarHeight))
            setContentView(rootLayout)
        } else {
            setContentView(scroll)
        }

        // Modern Header TopBar with absolute RTL/LTR side-switching support
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, dp(14))
            layoutDirection = if (arabic) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        if (showBack && onBack != null) {
            // Sub-screen Navigation Header: [Back Button] + [Breadcrumb / Title Column]
            val navGroup = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val backBtn = TextView(this).apply {
                text = if (arabic) "› عودة" else "‹ Back"
                textSize = 13.5f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                gravity = Gravity.CENTER
                background = roundedDrawable(
                    if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"),
                    10f,
                    if (dark) Color.parseColor("#312E81") else Color.parseColor("#C7D2FE")
                )
                setPadding(dp(12), dp(6), dp(12), dp(6))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener { onBack() }
            }
            navGroup.addView(backBtn)

            val titleCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleCol.addView(TextView(this).apply {
                text = "TowerFlow"
                textSize = 10.5f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#6366F1"))
                gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
            })
            titleCol.addView(TextView(this).apply {
                text = heading
                textSize = 16f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
            })
            navGroup.addView(titleCol)

            topBar.addView(navGroup)
        } else {
            // Root Screen Brand Header: [Logo] + [TowerFlow & Subtitle Column]
            val brandLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val logoView = ImageView(this).apply {
                setImageResource(R.drawable.towerflow_logo)
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    marginEnd = dp(10)
                }
            }
            brandLayout.addView(logoView)

            val brandCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (arabic) Gravity.RIGHT else Gravity.LEFT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
            brandLayout.addView(brandCol)

            topBar.addView(brandLayout)
        }

        // Actions Group: [Language] [Dark Mode]
        val actionsGroup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val langBtn = TextView(this).apply {
            text = if (arabic) "EN" else "عربي"
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36)).apply {
                marginEnd = dp(6)
            }
            setOnClickListener {
                arabic = !arabic
                getPreferences(0).edit().putBoolean("arabic", arabic).apply()
                if (token.isEmpty()) login() else home()
            }
        }
        actionsGroup.addView(langBtn)

        val themeBtn = TextView(this).apply {
            text = if (dark) "☀️" else "🌙"
            textSize = 15f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#FCD34D" else "#1E293B"))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedDrawable(
                if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"),
                10f,
                if (dark) Color.parseColor("#27394E") else Color.parseColor("#E2E8F0")
            )
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setOnClickListener {
                dark = !dark
                getPreferences(0).edit().putBoolean("dark", dark).apply()
                if (token.isEmpty()) login() else home()
            }
        }
        actionsGroup.addView(themeBtn)

        topBar.addView(actionsGroup)
        screen.addView(topBar)
    }

    private fun safeAddView(parent: ViewGroup, child: View, params: ViewGroup.LayoutParams? = null) {
        (child.parent as? ViewGroup)?.removeView(child)
        if (params != null) parent.addView(child, params) else parent.addView(child)
    }

    private fun card(addToScreen: Boolean = true, fn: LinearLayout.() -> Unit): LinearLayout {
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
        if (addToScreen) {
            safeAddView(screen, layout)
        }
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

    private fun navTile(key: String, titleText: String, subtitleText: String = "", badgeCount: Int = 0, onClick: (() -> Unit)? = null) {
        val navIcons = mapOf(
            "monthly_collections" to "⚡",
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
            setOnClickListener { onClick?.invoke() ?: listing(key) }
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
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                AlertDialog.Builder(this)
                    .setTitle(text("تنبيه", "Notice"))
                    .setMessage(e.message ?: text("حدث خطأ غير متوقع", "An unexpected error occurred"))
                    .setPositiveButton(text("حسناً", "OK"), null)
                    .show()
            }
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

                if (role in listOf("admin", "manager")) {
                    val propActionsRow = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                        weightSum = 2f
                    }
                    val addTowerQuick = Button(this@MainActivity).apply {
                        text = text("＋ إضافة برج جديد", "＋ Add Tower")
                        textSize = 11.5f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor("#4F46E5"))
                        isAllCaps = false
                        includeFontPadding = false
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(4), dp(7), dp(4), dp(7))
                        background = roundedDrawable(if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"), 8f, Color.parseColor(if (dark) "#312E81" else "#C7D2FE"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
                        setOnClickListener { showTowerDialog(null) { loadMe() } }
                    }
                    propActionsRow.addView(addTowerQuick)

                    val manageTowersQuick = Button(this@MainActivity).apply {
                        text = text("🏢 إدارة الأبراج", "🏢 Manage Towers")
                        textSize = 11.5f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#475569"))
                        isAllCaps = false
                        includeFontPadding = false
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(4), dp(7), dp(4), dp(7))
                        background = roundedDrawable(if (dark) Color.parseColor("#1E2330") else Color.parseColor("#F1F5F9"), 8f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener { manageTowers() }
                    }
                    propActionsRow.addView(manageTowersQuick)
                    addView(propActionsRow)
                }
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
                kpiRow.addView(makeKpi(text("إجمالي الشقق", "Total Units"), totalUnits.toString(), text("$rentedUnits مؤجرة · $ownedUnits تمليك", "$rentedUnits rent · $ownedUnits own"), true))
                kpiRow.addView(makeKpi(text("نسبة الإشغال", "Occupancy"), "$pct%", text("$vacantUnits شاغرة · $maintUnits صيانة", "$vacantUnits vacant"), false))
                kpiContainer.addView(kpiRow)

                val kpiRow2 = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(6)
                    }
                    weightSum = 2f
                }
                kpiRow2.addView(makeKpi(text("وحدات تمليك", "Owned Units"), ownedUnits.toString(), text("ملاك مقيمين", "Owner units"), true))
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

        // 1. Featured Monthly Collection Hero Card (For Managers/Admins)
        if (role != "resident") {
            card {
                val heroCollect = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    background = gradientDrawable(Color.parseColor("#4338CA"), Color.parseColor("#312E81"), 16f)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { monthlyCollections() }
                }
                val topRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                topRow.addView(TextView(this@MainActivity).apply {
                    text = "⚡"
                    textSize = 22f
                    setPadding(0, 0, dp(8), 0)
                })
                val titleWrap = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                titleWrap.addView(TextView(this@MainActivity).apply {
                    text = text("⚡ التحصيل الشهري لاشتراكات صيانة البرج", "Monthly Tower Maintenance Collections")
                    textSize = 16f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                titleWrap.addView(TextView(this@MainActivity).apply {
                    text = text("متابعة وسداد اشتراكات الصيانة والخدمات الشهرية للشقق فورياً", "Track & collect apartment monthly maintenance dues")
                    textSize = 11.5f
                    typeface = cairoFont
                    setTextColor(Color.parseColor("#C7D2FE"))
                })
                topRow.addView(titleWrap)
                topRow.addView(TextView(this@MainActivity).apply {
                    text = if (arabic) "‹" else "›"
                    textSize = 22f
                    setTextColor(Color.parseColor("#A5B4FC"))
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                })
                heroCollect.addView(topRow)
                addView(heroCollect)
            }
        }

        // 2. Streamlined Fast Action Bar
        sectionHeader(text("⚡ إجراءات سريعة", "⚡ Quick Actions"))
        val quickGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
                bottomMargin = dp(6)
            }
            weightSum = 3f
        }

        val q1 = Button(this).apply {
            text = text(if (role == "resident") "📤 إثبات سداد" else "⚡ تحصيل الصيانة", if (role == "resident") "Proof" else "⚡ Maintenance")
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), dp(10), dp(4), dp(10))
            background = gradientDrawable(Color.parseColor("#059669"), Color.parseColor("#10B981"), 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) }
            setOnClickListener { if (role == "resident") proofForm() else monthlyCollections() }
        }
        quickGrid.addView(q1)

        val q2 = Button(this).apply {
            text = text(if (role == "resident") "💳 فواتيري" else "＋ إضافة وحدة", if (role == "resident") "My Dues" else "＋ Add Unit")
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), dp(10), dp(4), dp(10))
            background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) }
            setOnClickListener { if (role == "resident") listing("invoices") else recordForm("units") }
        }
        quickGrid.addView(q2)

        val q3 = Button(this).apply {
            text = text("🔧 بلاغ صيانة", "Ticket")
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), dp(10), dp(4), dp(10))
            background = roundedDrawable(
                Color.TRANSPARENT,
                12f,
                Color.parseColor(if (dark) "#334155" else "#CBD5E1")
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { ticketForm() }
        }
        quickGrid.addView(q3)
        screen.addView(quickGrid)

        // 3. Essential, Useful Sections Only (No Empty Ghost Sections)
        if (role != "resident") {
            sectionHeader(text("🏢 العمليات وإدارة البرج الأساسية", "Core Operations"))
            navTile("properties", text("الأبراج والعقارات", "Towers & Properties"), text("إضافة وتعديل وحذف الأبراج السكنية ومباني الإدارة", "Manage residential towers")) { manageTowers() }
            navTile("monthly_collections", text("التحصيل الشهري لاشتراكات الصيانة", "Monthly Maintenance Collections"), text("تحصيل وسداد اشتراكات الصيانة والخدمات للبرج", "Apartment maintenance subscriptions & collections")) { monthlyCollections() }
            navTile("units", title("units"), text("حصر الشقق، أرقام الوحدات، وحالة الإشغال", "Apartments, unit numbers and occupancy"))
            navTile("parties", title("parties"), text("سجل السكان، الملاك، وجهات التواصل", "Residents, owners and contacts registry"))
            navTile("invoices", title("invoices"), text("الفواتير الشهرية والخدمات والمطالبات", "Issued invoices and dues"))
            navTile("tickets", title("tickets"), text("طلبات وبلاغات الصيانة وتتبع إنجازها", "Maintenance tickets tracking"))

            sectionHeader(text("📂 السجلات والتقارير", "Records & Reports"))
            // leases removed
            navTile("receipts", title("receipts"), text("سندات القبض والتحصيل المسددة", "Payment receipts and collections"))
            navTile("expenses", title("expenses"), text("أوامر الصرف ومصروفات التشغيل والصيانة", "Operational and maintenance expenses"))
            navTile("reports", title("reports"), text("التقارير المالية والتحصيل وميزان الحركة", "Financial and collection reports"))
            navTile("profile", text("الملف الشخصي والإعدادات", "My Profile & Settings"), text("بيانات حسابك وتفضيلات التطبيق", "Account details and app preferences")) { profile() }
            navTile("logout", text("تسجيل الخروج", "Sign Out"), text("الخروج الآمن من الحساب الحالي", "Sign out of your account")) { disablePushAndLogout() }
        } else {
            sectionHeader(text("خدماتي السكنية", "My Resident Services"))
            navTile("units", title("units"), text("بيانات وحدتي السكنية ورقم الشقة", "My apartment details"))
            // resident lease removed
            navTile("invoices", title("invoices"), text("الفواتير والمستحقات الشهرية", "My invoices and dues"))
            navTile("payment_proofs", title("payment_proofs"), text("إرسال إثبات تحويل بنكي للمراجعة", "Submit bank transfer slip"))
            navTile("tickets", title("tickets"), text("طلب صيانة أو إصلاح داخل الوحدة", "Maintenance requests"))
            navTile("profile", text("الملف الشخصي والإعدادات", "My Profile & Settings"), text("بيانات حسابك واللغة والمظهر", "Account details and preferences")) { profile() }
            navTile("logout", text("تسجيل الخروج", "Sign Out"), text("الخروج الآمن من التطبيق", "Sign out of account")) { disablePushAndLogout() }
        }
    }

    private fun manageTowers() {
        scaffold(text("الأبراج والعقارات", "Towers & Properties"), true) { home() }

        if (role in listOf("admin", "manager")) {
            primaryButton(text("＋ إضافة برج سكني جديد", "＋ Add New Tower")) {
                showTowerDialog(null) { manageTowers() }
            }
        }

        val loading = TextView(this).apply {
            text = text("جاري تحميل قائمة الأبراج...", "Loading towers...")
            textSize = 13.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, dp(30), 0, dp(30))
        }
        screen.addView(loading)

        call("properties") { propRaw ->
            val props = try { JSONArray(propRaw) } catch (_: Exception) { JSONArray() }
            call("units") { unitRaw ->
                val units = try { JSONArray(unitRaw) } catch (_: Exception) { JSONArray() }
                val unitsByProp = mutableMapOf<String, Int>()
                for (i in 0 until units.length()) {
                    val pId = units.getJSONObject(i).optString("property_id")
                    unitsByProp[pId] = (unitsByProp[pId] ?: 0) + 1
                }

                loading.visibility = View.GONE
                if (props.length() == 0) {
                    val empty = TextView(this@MainActivity).apply {
                        text = text("لا توجد أبراج مسجلة حالياً. اضغط على الزر أعلاه لإضافة برجك الأول.", "No towers registered yet. Tap above to add your first tower.")
                        textSize = 13f
                        typeface = cairoFont
                        setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                        gravity = Gravity.CENTER
                        setPadding(dp(16), dp(40), dp(16), dp(40))
                    }
                    screen.addView(empty)
                    return@call
                }

                for (i in 0 until props.length()) {
                    val prop = props.getJSONObject(i)
                    val pId = prop.optString("id")
                    val pName = prop.optString("name", text("برج بدون اسم", "Unnamed Tower"))
                    val pAddr = prop.optString("address", "")
                    val count = unitsByProp[pId] ?: 0

                    card {
                        val headerRow = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        val iconView = TextView(this@MainActivity).apply {
                            text = "🏢"
                            textSize = 24f
                            setPadding(0, 0, dp(10), 0)
                        }
                        headerRow.addView(iconView)

                        val infoCol = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val titleView = TextView(this@MainActivity).apply {
                            text = pName
                            textSize = 16f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                        }
                        infoCol.addView(titleView)

                        if (pAddr.isNotEmpty()) {
                            val addrView = TextView(this@MainActivity).apply {
                                text = "📍 $pAddr"
                                textSize = 11.5f
                                typeface = cairoFont
                                setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                            }
                            infoCol.addView(addrView)
                        }

                        val countBadge = TextView(this@MainActivity).apply {
                            text = text("$count شقة مسجلة", "$count units registered")
                            textSize = 11f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor("#4F46E5"))
                            setPadding(dp(8), dp(2), dp(8), dp(2))
                            background = roundedDrawable(Color.parseColor(if (dark) "#1E1B4B" else "#EEF2FF"), 8f)
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                topMargin = dp(4)
                            }
                        }
                        infoCol.addView(countBadge)
                        headerRow.addView(infoCol)
                        addView(headerRow)

                        if (role in listOf("admin", "manager")) {
                            val btnRow = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    topMargin = dp(12)
                                }
                                weightSum = 3f
                            }

                            val editBtn = Button(this@MainActivity).apply {
                                text = text("✏️ تعديل", "✏️ Edit")
                                textSize = 12f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                                isAllCaps = false
                                includeFontPadding = false
                                minHeight = 0
                                minimumHeight = 0
                                setPadding(dp(4), dp(8), dp(4), dp(8))
                                background = roundedDrawable(if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"), 10f, Color.parseColor(if (dark) "#312E81" else "#C7D2FE"))
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
                                setOnClickListener { showTowerDialog(prop) { manageTowers() } }
                            }
                            btnRow.addView(editBtn)

                            val unitsBtn = Button(this@MainActivity).apply {
                                text = text("🏢 الشقق", "🏢 Units")
                                textSize = 12f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.parseColor(if (dark) "#34D399" else "#059669"))
                                isAllCaps = false
                                includeFontPadding = false
                                minHeight = 0
                                minimumHeight = 0
                                setPadding(dp(4), dp(8), dp(4), dp(8))
                                background = roundedDrawable(if (dark) Color.parseColor("#064E3B") else Color.parseColor("#ECFDF5"), 10f, Color.parseColor(if (dark) "#047857" else "#A7F3D0"))
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
                                setOnClickListener { listing("units") }
                            }
                            btnRow.addView(unitsBtn)

                            val delBtn = Button(this@MainActivity).apply {
                                text = text("🗑️ حذف", "🗑️ Delete")
                                textSize = 12f
                                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                                setTextColor(Color.parseColor("#EF4444"))
                                isAllCaps = false
                                includeFontPadding = false
                                minHeight = 0
                                minimumHeight = 0
                                setPadding(dp(4), dp(8), dp(4), dp(8))
                                background = roundedDrawable(if (dark) Color.parseColor("#450A0A") else Color.parseColor("#FEF2F2"), 10f, Color.parseColor(if (dark) "#991B1B" else "#FECACA"))
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                setOnClickListener { confirmDeleteTower(prop) { manageTowers() } }
                            }
                            btnRow.addView(delBtn)

                            addView(btnRow)
                        }
                    }
                }
            }
        }
    }

    private fun showTowerDialog(prop: JSONObject? = null, onSaved: (() -> Unit)? = null) {
        val isEdit = prop != null
        val dlgTitle = if (isEdit) text("تعديل بيانات البرج", "Edit Tower") else text("إضافة برج سكني جديد", "Add Residential Tower")
        val formLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        val nameLabel = TextView(this).apply {
            text = text("اسم البرج السكني *", "Tower Name *")
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#334155"))
            setPadding(0, 0, 0, dp(4))
        }
        formLayout.addView(nameLabel)

        val nameInput = EditText(this).apply {
            hint = text("مثال: برج عبد الرازق، برج الصفوة", "e.g. Tower 1")
            setText(prop?.optString("name", "") ?: "")
            textSize = 14f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            background = roundedDrawable(if (dark) Color.parseColor("#1E2330") else Color.WHITE, 10f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        formLayout.addView(nameInput)

        val addrLabel = TextView(this).apply {
            text = text("العنوان / الموقع", "Address / Location")
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#334155"))
            setPadding(0, dp(12), 0, dp(4))
        }
        formLayout.addView(addrLabel)

        val addrInput = EditText(this).apply {
            hint = text("مثال: شارع الجمهورية، الحي الأول", "e.g. Main Street")
            setText(prop?.optString("address", "") ?: "")
            textSize = 14f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            background = roundedDrawable(if (dark) Color.parseColor("#1E2330") else Color.WHITE, 10f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        formLayout.addView(addrInput)

        val ipaLabel = TextView(this).apply {
            text = text("عنوان إنستاباي InstaPay IPA:", "InstaPay IPA:")
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#334155"))
            setPadding(0, dp(12), 0, dp(4))
        }
        formLayout.addView(ipaLabel)

        val ipaInput = EditText(this).apply {
            hint = text("مثال: tower@instapay", "e.g. tower@instapay")
            setText(prop?.optString("instapay_id", "") ?: "")
            textSize = 14f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            background = roundedDrawable(if (dark) Color.parseColor("#1E2330") else Color.WHITE, 10f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        formLayout.addView(ipaInput)

        val walletLabel = TextView(this).apply {
            text = text("رقم المحفظة الإلكترونية (فودافون كاش / إنستاباي / وي):", "E-Wallet Mobile:")
            textSize = 12f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#334155"))
            setPadding(0, dp(12), 0, dp(4))
        }
        formLayout.addView(walletLabel)

        val walletInput = EditText(this).apply {
            hint = text("01xxxxxxxxx", "01xxxxxxxxx")
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(prop?.optString("wallet_number", "") ?: "")
            textSize = 14f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
            setHintTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
            background = roundedDrawable(if (dark) Color.parseColor("#1E2330") else Color.WHITE, 10f, Color.parseColor(if (dark) "#334155" else "#CBD5E1"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        formLayout.addView(walletInput)

        AlertDialog.Builder(this)
            .setTitle(dlgTitle)
            .setView(formLayout)
            .setPositiveButton(text("حفظ البيانات", "Save")) { _, _ ->
                val tName = nameInput.text.toString().trim()
                if (tName.isEmpty()) {
                    Toast.makeText(this, text("يرجى إدخال اسم البرج", "Please enter tower name"), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val payload = JSONObject().apply {
                    put("name", tName)
                    put("address", addrInput.text.toString().trim())
                    put("instapay_id", ipaInput.text.toString().trim())
                    put("wallet_number", walletInput.text.toString().trim())
                }
                val path = if (isEdit) "properties/" + prop!!.optString("id") else "properties"
                val method = if (isEdit) "PATCH" else "POST"
                call(path, method, payload) {
                    Toast.makeText(this, text("تم حفظ بيانات البرج بنجاح", "Tower saved successfully"), Toast.LENGTH_SHORT).show()
                    onSaved?.invoke() ?: home()
                }
            }
            .setNegativeButton(text("إلغاء", "Cancel"), null)
            .show()
    }

    private fun confirmDeleteTower(prop: JSONObject, onDeleted: (() -> Unit)? = null) {
        val pName = prop.optString("name", text("البرج", "Tower"))
        AlertDialog.Builder(this)
            .setTitle(text("حذف البرج نهائياً", "Delete Tower"))
            .setMessage(text("تحذير أمني:\nهل أنت متأكد من حذف \"$pName\" نهائياً؟\nسيتم حذف كافة الوحدات والشقق وسجلات الحسابات والاشتراكات التابعة لهذا البرج بشكل كامل ولا يمكن التراجع.", "Security Warning:\nAre you sure you want to permanently delete \"$pName\"?\nAll associated units, records, and dues will be permanently removed."))
            .setPositiveButton(text("نعم، حذف نهائي", "Yes, Delete")) { _, _ ->
                call("properties/" + prop.optString("id") + "?force=true", "DELETE") {
                    Toast.makeText(this, text("تم حذف البرج بنجاح", "Tower deleted successfully"), Toast.LENGTH_SHORT).show()
                    onDeleted?.invoke() ?: home()
                }
            }
            .setNegativeButton(text("إلغاء", "Cancel"), null)
            .show()
    }

    private fun monthlyCollections(targetMonth: String? = null) {
        val path = "collections/monthly" + if (targetMonth != null) "?month=$targetMonth" else ""
        scaffold(text("التحصيل الشهري لاشتراكات الصيانة", "Monthly Maintenance Collections"), true) { home() }

        val loading = TextView(this).apply {
            text = text("جاري تحميل كشف التحصيل الشهري...", "Loading monthly collections...")
            textSize = 14f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(40), dp(16), dp(40))
        }
        screen.addView(loading)

        call(path) { raw ->
            try {
                val data = JSONObject(raw)
                val currentMonth = data.optString("month", "")
                val summary = data.optJSONObject("summary") ?: JSONObject()
                val unitsArr = data.optJSONArray("units") ?: JSONArray()
                renderMonthlyCollections(currentMonth, summary, unitsArr, data)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    private fun renderMonthlyCollections(currentMonth: String, summary: JSONObject, unitsArr: JSONArray, data: JSONObject = JSONObject()) {
        scaffold(text("التحصيل الشهري للشقق", "Monthly Collections"), true) { home() }

        // 1. Month Switcher Card
        card {
            val monthRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            fun shiftMonth(offset: Int): String {
                val parts = currentMonth.split("-")
                var y = parts[0].toIntOrNull() ?: 2026
                var m = parts[1].toIntOrNull() ?: 1
                m += offset
                if (m < 1) { m = 12; y-- }
                if (m > 12) { m = 1; y++ }
                return "$y-" + (if (m < 10) "0$m" else "$m")
            }

            val prevBtn = Button(this@MainActivity).apply {
                text = text("‹ السابق", "‹ Prev")
                textSize = 12f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                isAllCaps = false
                includeFontPadding = false
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2FF"), 8f)
                setOnClickListener { monthlyCollections(shiftMonth(if (arabic) 1 else -1)) }
            }
            monthRow.addView(prevBtn)

            val monthTitle = TextView(this@MainActivity).apply {
                val parts = currentMonth.split("-")
                val mNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                val yNum = parts.getOrNull(0) ?: ""
                val monthNamesAr = listOf("", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
                val monthNamesEn = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                text = text(monthNamesAr.getOrElse(mNum) { currentMonth } + " $yNum", monthNamesEn.getOrElse(mNum) { currentMonth } + " $yNum")
                textSize = 16f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            monthRow.addView(monthTitle)

            val nextBtn = Button(this@MainActivity).apply {
                text = text("التالي ›", "Next ›")
                textSize = 12f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                isAllCaps = false
                includeFontPadding = false
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2FF"), 8f)
                setOnClickListener { monthlyCollections(shiftMonth(if (arabic) -1 else 1)) }
            }
            monthRow.addView(nextBtn)

            addView(monthRow)
        }

        // 2. Summary KPI Cards
        val totalDue = summary.optString("total_due", "0.00")
        val totalCollected = summary.optString("total_collected", "0.00")
        val totalRemaining = summary.optString("total_remaining", "0.00")
        val rate = summary.optInt("collection_rate", 0)

        fun makeKpiBox(kpiTitle: String, kpiVal: String, valColor: Int, subText: String, isFirst: Boolean): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedDrawable(
                    if (dark) Color.parseColor("#11131A") else Color.WHITE,
                    12f,
                    if (dark) Color.parseColor("#1E2330") else Color.parseColor("#E2E8F0")
                )
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (isFirst) marginEnd = dp(4) else marginStart = dp(4)
                }
                addView(TextView(this@MainActivity).apply {
                    text = kpiTitle
                    textSize = 11f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                })
                addView(TextView(this@MainActivity).apply {
                    text = kpiVal
                    textSize = 17f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(valColor)
                    setPadding(0, dp(2), 0, dp(2))
                })
                addView(TextView(this@MainActivity).apply {
                    text = subText
                    textSize = 9.5f
                    typeface = cairoFont
                    setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                })
            }
        }

        val kpiRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            }
            weightSum = 2f
        }
        kpiRow1.addView(makeKpiBox(text("إجمالي المطلوب", "Total Due"), "$totalDue ج.م", Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"), text("اشتراكات الصيانة والخدمات", "Monthly maintenance dues"), true))
        kpiRow1.addView(makeKpiBox(text("المحصل فعلياً", "Collected"), "$totalCollected ج.م", Color.parseColor("#10B981"), text("نسبة التحصيل $rate%", "Collection rate $rate%"), false))
        screen.addView(kpiRow1)

        val kpiRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(8)
            }
            weightSum = 2f
        }
        kpiRow2.addView(makeKpiBox(text("المتبقي للتحصيل", "Remaining"), "$totalRemaining ج.م", Color.parseColor("#EF4444"), text("مستحقات معلقة", "Pending dues"), true))
        kpiRow2.addView(makeKpiBox(text("حالة الإشغال", "Occupancy"), summary.optInt("occupied_units", 0).toString() + " / " + summary.optInt("total_units", 0), Color.parseColor("#4F46E5"), text("شقة مأهولة", "Occupied units"), false))
        screen.addView(kpiRow2)

        // 3. Batch Generate Monthly Dues Button
        val batchBtn = Button(this).apply {
            text = text("⚡ توليد مطالبات شهر $currentMonth لجميع الشقق", "⚡ Generate $currentMonth Dues for All Units")
            textSize = 12.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            includeFontPadding = false
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = gradientDrawable(Color.parseColor("#4338CA"), Color.parseColor("#3730A3"), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            // InstaPay & Wallets payment banner
            val propSettings = data.optJSONObject("property_settings")
            val ipaId = propSettings?.optString("instapay_id", "") ?: ""
            val walletNo = propSettings?.optString("wallet_number", "") ?: ""
            if (ipaId.isNotEmpty() || walletNo.isNotEmpty()) {
                val payBanner = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = roundedDrawable(if (dark) Color.parseColor("#172554") else Color.parseColor("#EFF6FF"), 12f, Color.parseColor(if (dark) "#1E40AF" else "#BFDBFE"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(10)
                    }
                }
                payBanner.addView(TextView(this@MainActivity).apply {
                    text = text("📱 قنوات الدفع الإلكتروني المعتمدة للبرج:", "📱 Tower Electronic Payment Channels:")
                    textSize = 12f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#93C5FD" else "#1D4ED8"))
                })
                if (ipaId.isNotEmpty()) {
                    payBanner.addView(TextView(this@MainActivity).apply {
                        text = "⚡ InstaPay IPA: " + ipaId
                        textSize = 13.5f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                        setPadding(0, dp(4), 0, dp(2))
                    })
                }
                if (walletNo.isNotEmpty()) {
                    payBanner.addView(TextView(this@MainActivity).apply {
                        text = "💳 " + text("المحفظة الإلكترونية (فودافون كاش / إنستاباي): ", "E-Wallet Phone: ") + walletNo
                        textSize = 12.5f
                        typeface = cairoFont
                        setTextColor(Color.parseColor(if (dark) "#CBD5E1" else "#334155"))
                    })
                }
                screen.addView(payBanner)
            }

                        setOnClickListener {
                val input = EditText(this@MainActivity).apply {
                    hint = text("قيمة اشتراك الصيانة والخدمات للبرج (مثال: 500)", "Default maintenance fee (e.g. 500)")
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setText("500")
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(text("توليد مطالبات الشهر", "Generate Monthly Dues"))
                    .setMessage(text("سيتم إنشاء مطالبات اشتراك صيانة شهر $currentMonth لكافة الشقق (خدمات، أمن، نظافة، مصعد).", "Generate monthly tower maintenance invoices for all unbilled units for $currentMonth."))
                    .setView(input)
                    .setPositiveButton(text("توليد الآن", "Generate")) { _, _ ->
                        val fee = input.text.toString().trim()
                        if (fee.isNotEmpty()) {
                            call("collections/generate-month", "POST", JSONObject().put("month", currentMonth).put("default_fee", fee)) { raw ->
                                val res = JSONObject(raw)
                                val count = res.optInt("generated_count", 0)
                                Toast.makeText(this@MainActivity, text("تم توليد $count مطالبة بنجاح", "Generated $count dues successfully"), Toast.LENGTH_SHORT).show()
                                monthlyCollections(currentMonth)
                            }
                        }
                    }
                    .setNegativeButton(text("إلغاء", "Cancel"), null)
                    .show()
            }
        }
        screen.addView(batchBtn)

        // 4. Units Filter Bar & Container
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
            weightSum = 3f
        }

        var activeFilter = "all"
        val unitsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun refreshUnitsList() {
            unitsContainer.removeAllViews()
            var count = 0
            for (i in 0 until unitsArr.length()) {
                val u = unitsArr.getJSONObject(i)
                val isPaid = u.optBoolean("is_paid", false)
                val fee = u.optDouble("monthly_fee", 0.0)
                val isOwnedUnit = u.optString("ownership_type") == "owned" || u.optString("status") == "owned" || u.optString("resident_kind") == "owner"

                if (activeFilter == "paid" && !isPaid) continue
                if (activeFilter == "unpaid" && isPaid) continue
                if (activeFilter == "owned" && !isOwnedUnit) continue
                count++

                // Unit Card
                val uCard = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = roundedDrawable(
                        if (dark) Color.parseColor("#11131A") else Color.WHITE,
                        14f,
                        if (isPaid) Color.parseColor("#059669") else (if (dark) Color.parseColor("#1E2330") else Color.parseColor("#CBD5E1"))
                    )
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(8)
                    }
                }

                // Header Line: Unit Number + Floor + Status Badge
                val topRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val unitNumber = u.optString("unit_number", u.optString("unit_name", ""))
                val titleTv = TextView(this@MainActivity).apply {
                    text = text("شقة / وحدة " + unitNumber, "Unit " + unitNumber)
                    textSize = 15.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                topRow.addView(titleTv)

                val occState = u.optString("occupancy_state", "residing")
                val occBadgeTv = TextView(this@MainActivity).apply {
                    text = when (occState) {
                        "closed" -> text("🟡 مغلقة", "🟡 Closed")
                        "finishing" -> text("🔵 تشطيب", "🔵 Finishing")
                        else -> text("🟢 مقيمة", "🟢 Residing")
                    }
                    textSize = 10f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(when (occState) {
                        "closed" -> if (dark) "#FDE047" else "#854D0E"
                        "finishing" -> if (dark) "#93C5FD" else "#1E40AF"
                        else -> if (dark) "#86EFAC" else "#166534"
                    }))
                    setPadding(dp(7), dp(3), dp(7), dp(3))
                    background = roundedDrawable(
                        Color.parseColor(when (occState) {
                            "closed" -> if (dark) "#422006" else "#FEF9C3"
                            "finishing" -> if (dark) "#172554" else "#DBEAFE"
                            else -> if (dark) "#052E16" else "#DCFCE7"
                        }),
                        8f
                    )
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = dp(6)
                    }
                }
                topRow.addView(occBadgeTv)

                val badgeTv = TextView(this@MainActivity).apply {
                    text = if (isPaid) {
                        text("✅ تم السداد", "✅ Paid")
                    } else if (fee > 0) {
                        text("⏳ غير مسدد", "⏳ Pending")
                    } else {
                        if (isOwnedUnit) text("🏢 تمليك", "🏢 Owned") else text("⚪ بدون اشتراك", "⚪ No Dues")
                    }
                    textSize = 10.5f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (isPaid) "#10B981" else (if (fee > 0) "#F59E0B" else (if (isOwnedUnit) "#0284C7" else "#94A3B8"))))
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    background = roundedDrawable(
                        Color.parseColor(if (isPaid) (if (dark) "#064E3B" else "#ECFDF5") else (if (fee > 0) (if (dark) "#78350F" else "#FEF3C7") else (if (isOwnedUnit) (if (dark) "#082F49" else "#E0F2FE") else (if (dark) "#1E293B" else "#F1F5F9")))),
                        8f
                    )
                }
                topRow.addView(badgeTv)
                uCard.addView(topRow)

                // Subtitle: Resident Info & Kind (An owned unit NEVER displays as vacant)
                val rawResName = u.optString("resident_name", "")
                val cleanResName = if (isOwnedUnit && (rawResName.isEmpty() || rawResName.contains("شاغرة") || rawResName.contains("vacant"))) {
                    text("مالك الوحدة (تمليك)", "Unit Owner (Owned)")
                } else if (rawResName.isNotEmpty()) {
                    rawResName
                } else {
                    if (isOwnedUnit) text("مالك الوحدة (تمليك)", "Unit Owner (Owned)") else text("شاغرة للإيجار", "Vacant Rental")
                }
                val resKind = if (isOwnedUnit) text("مالك", "Owner") else text("مستأجر", "Tenant")
                val resPhone = u.optString("resident_phone", "")

                val resTv = TextView(this@MainActivity).apply {
                    text = "👤 " + cleanResName + " · " + resKind + (if (resPhone.isNotEmpty()) " · 📞 " + resPhone else "")
                    textSize = 11.5f
                    typeface = cairoFont
                    setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                    setPadding(0, dp(4), 0, dp(4))
                }
                uCard.addView(resTv)

                // Financial Line: Monthly Fee & Status
                val feeRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(2), 0, dp(6))
                }
                feeRow.addView(TextView(this@MainActivity).apply {
                    text = text("المطلوب لشهر " + currentMonth + ": ", "Due for " + currentMonth + ": ")
                    textSize = 12f
                    typeface = cairoFont
                    setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                })
                feeRow.addView(TextView(this@MainActivity).apply {
                    text = u.optString("monthly_fee", "0.00") + " ج.م"
                    textSize = 13f
                    typeface = Typeface.create(cairoFont, Typeface.BOLD)
                    setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                })
                feeRow.addView(TextView(this@MainActivity).apply {
                    text = "  (" + text("اشتراك صيانة البرج", "Tower Maintenance") + ")"
                    textSize = 10f
                    typeface = cairoFont
                    setTextColor(Color.parseColor(if (dark) "#64748B" else "#94A3B8"))
                })
                uCard.addView(feeRow)

                // Action Row
                val actionRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                if (isPaid) {
                    val recRef = u.optString("receipt_reference", "")
                    val colAt = u.optString("collected_at", "")
                    val paidInfo = TextView(this@MainActivity).apply {
                        text = "🧾 " + text("مسدد بسند رقم: " + recRef, "Receipt: " + recRef) + (if (colAt.isNotEmpty()) " (" + colAt + ")" else "")
                        textSize = 11f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor("#10B981"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    actionRow.addView(paidInfo)
                } else {
                    val collectBtn = Button(this@MainActivity).apply {
                        text = text("⚡ تحصيل فوري", "⚡ Collect Now")
                        textSize = 12f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        isAllCaps = false
                        includeFontPadding = false
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        background = gradientDrawable(Color.parseColor("#059669"), Color.parseColor("#10B981"), 10f)
                        setOnClickListener {
                            showQuickCollectDialog(u, currentMonth)
                        }
                    }
                    actionRow.addView(collectBtn)
                }

                // If owner is not assigned yet, show button to set owner directly
                if (u.optString("party_id").isEmpty()) {
                    val assignBtn = Button(this@MainActivity).apply {
                        text = text("👤 بيانات المالك", "👤 Set Owner")
                        textSize = 11f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
                        isAllCaps = false
                        includeFontPadding = false
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        background = roundedDrawable(if (dark) Color.parseColor("#1E1B4B") else Color.parseColor("#EEF2FF"), 10f)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginStart = dp(8)
                        }
                        setOnClickListener {
                            showAssignResidentDialog(u, isOwnedUnit, currentMonth)
                        }
                    }
                    actionRow.addView(assignBtn)
                }
                if (resPhone.isNotEmpty()) {
                    val waBtn = Button(this@MainActivity).apply {
                        text = "💬 واتساب"
                        textSize = 11f
                        typeface = Typeface.create(cairoFont, Typeface.BOLD)
                        setTextColor(Color.parseColor("#059669"))
                        isAllCaps = false
                        includeFontPadding = false
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                        background = roundedDrawable(if (dark) Color.parseColor("#064E3B") else Color.parseColor("#ECFDF5"), 10f, Color.parseColor(if (dark) "#047857" else "#A7F3D0"))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            marginStart = dp(6)
                        }
                        setOnClickListener {
                            try {
                                var cleanPhone = resPhone.replace("+", "").replace(" ", "").replace("-", "")
                                if (cleanPhone.startsWith("0")) cleanPhone = "20" + cleanPhone.substring(1)
                                val msg = if (isPaid) {
                                    text("مرحباً ساكن شقة $unitNumber، تم تأكيد استلام اشتراك صيانة البرج لشهر $currentMonth بسند رقم " + u.optString("receipt_reference") + ". شكراً لتعاونكم - إدارة البرج.", "Hello resident of unit $unitNumber, dues for $currentMonth received.")
                                } else {
                                    text("مرحباً ساكن شقة $unitNumber، تذكير بسداد اشتراك صيانة البرج لشهر $currentMonth بمبلغ " + u.optString("monthly_fee") + " ج.م. شاكرين حسن تعاونكم - إدارة البرج.", "Hello unit $unitNumber, reminder for dues for $currentMonth.")
                                }
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(msg))
                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, text("تعذر فتح واتساب: " + e.message, "Could not open WhatsApp"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    actionRow.addView(waBtn)
                }

                uCard.addView(actionRow)

                unitsContainer.addView(uCard)
            }

            if (count == 0) {
                unitsContainer.addView(TextView(this@MainActivity).apply {
                    text = text("لا توجد وحدات مطابقة للفلتر المحدد", "No units match this filter")
                    textSize = 13f
                    typeface = cairoFont
                    setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                    gravity = Gravity.CENTER
                    setPadding(0, dp(24), 0, dp(24))
                })
            }
        }

        fun makeFilterBtn(fKey: String, fLabel: String): Button {
            return Button(this).apply {
                text = fLabel
                textSize = 11.5f
                typeface = Typeface.create(cairoFont, Typeface.BOLD)
                isAllCaps = false
                includeFontPadding = false
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(4)
                }
                fun updateStyle() {
                    if (activeFilter == fKey) {
                        setTextColor(Color.WHITE)
                        background = gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 10f)
                    } else {
                        setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                        background = roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"), 10f)
                    }
                }
                updateStyle()
                setOnClickListener {
                    activeFilter = fKey
                    for (k in 0 until filterRow.childCount) {
                        val b = filterRow.getChildAt(k) as? Button
                        if (b != null) {
                            val isActive = (k == 0 && fKey == "all") || (k == 1 && fKey == "unpaid") || (k == 2 && fKey == "paid") || (k == 3 && fKey == "owned")
                            b.setTextColor(if (isActive) Color.WHITE else Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                            b.background = if (isActive) gradientDrawable(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1"), 10f)
                                           else roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#EDF2F7"), 10f)
                        }
                    }
                    refreshUnitsList()
                }
            }
        }

        filterRow.addView(makeFilterBtn("all", text("الكل (" + unitsArr.length() + ")", "All (" + unitsArr.length() + ")")))
        filterRow.addView(makeFilterBtn("unpaid", text("غير مسدد ⏳", "Unpaid ⏳")))
        filterRow.addView(makeFilterBtn("paid", text("مسدد ✅", "Paid ✅")))
        filterRow.addView(makeFilterBtn("owned", text("التمليك 🏢", "Owned 🏢")))

        screen.addView(filterRow)
        screen.addView(unitsContainer)
        refreshUnitsList()
    }

    private fun showAssignResidentDialog(u: JSONObject, isOwned: Boolean, currentMonth: String) {
        val unitId = u.optString("unit_id")
        val unitNumber = u.optString("unit_number", u.optString("unit_name", ""))

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(10))
        }

        val titleTv = TextView(this).apply {
            text = text("تحديد بيانات المالك / الساكن لشقة " + unitNumber, "Assign Owner for Unit " + unitNumber)
            textSize = 14.5f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        }
        dialogView.addView(titleTv)

        val nameLabel = TextView(this).apply {
            text = text("اسم المالك أو الساكن بالكامل:", "Full Name:")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(0, dp(10), 0, dp(4))
        }
        dialogView.addView(nameLabel)

        val nameInput = EditText(this).apply {
            hint = text("مثال: أحمد مرجان", "e.g. Ahmed Morgan")
            textSize = 13.5f
            typeface = cairoFont
            background = roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#F1F5F9"), 10f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        }
        dialogView.addView(nameInput)

        val phoneLabel = TextView(this).apply {
            text = text("رقم الهاتف للتواصل:", "Phone Number:")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(0, dp(10), 0, dp(4))
        }
        dialogView.addView(phoneLabel)

        val phoneInput = EditText(this).apply {
            hint = text("01xxxxxxxxx", "Phone")
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            textSize = 13.5f
            typeface = cairoFont
            background = roundedDrawable(if (dark) Color.parseColor("#17243B") else Color.parseColor("#F1F5F9"), 10f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(Color.parseColor(if (dark) "#F1F5F9" else "#0F172A"))
        }
        dialogView.addView(phoneInput)

        val stateLabel = TextView(this).apply {
            text = text("حالة إشغال الشقة:", "Occupancy State:")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(0, dp(10), 0, dp(4))
        }
        dialogView.addView(stateLabel)

        val states = listOf(
            Pair(text("مشغولة (ساكن مقيم) 🟢", "Residing 🟢"), "residing"),
            Pair(text("مغلقة 🟡", "Closed 🟡"), "closed"),
            Pair(text("تحت التشطيب 🔵", "Finishing 🔵"), "finishing")
        )
        val stateSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, states.map { it.first })
            val currentState = u.optString("occupancy_state", "residing")
            val defaultIdx = states.indexOfFirst { it.second == currentState }.takeIf { it >= 0 } ?: 0
            setSelection(defaultIdx)
        }
        dialogView.addView(stateSpinner)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(text("حفظ وتعيين", "Save & Assign")) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isEmpty()) {
                    error(IllegalStateException(text("يرجى إدخال اسم المالك أو الساكن", "Please enter a name")))
                    return@setPositiveButton
                }
                val selectedState = states[stateSpinner.selectedItemPosition].second
                val payload = JSONObject().apply {
                    put("name", name)
                    put("phone", phone)
                    put("kind", if (isOwned) "owner" else "tenant")
                    put("occupancy_state", selectedState)
                }
                call("units/" + unitId + "/assign-resident", "POST", payload) {
                    Toast.makeText(this@MainActivity, text("تم حفظ بيانات المالك بنجاح", "Saved successfully"), Toast.LENGTH_SHORT).show()
                    monthlyCollections(currentMonth)
                }
            }
            .setNegativeButton(text("إلغاء", "Cancel"), null)
            .show()
    }

    private fun showQuickCollectDialog(u: JSONObject, currentMonth: String) {
        val unitNumber = u.optString("unit_number", u.optString("unit_name", ""))
        val resName = u.optString("resident_name", "الساكن")
        val fee = u.optString("monthly_fee", "500.00")

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }

        dialogView.addView(TextView(this).apply {
            text = text("شقة: $unitNumber · الساكن: $resName", "Unit: $unitNumber · Resident: $resName")
            textSize = 13f
            typeface = Typeface.create(cairoFont, Typeface.BOLD)
            setTextColor(Color.parseColor(if (dark) "#818CF8" else "#4F46E5"))
            setPadding(0, 0, 0, dp(10))
        })

        dialogView.addView(TextView(this).apply {
            text = text("المبلغ المراد تحصيله (ج.م)", "Amount to Collect")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
        })
        val amountInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (fee == "0.00" || fee.isEmpty()) "500" else fee)
        }
        dialogView.addView(amountInput)

        dialogView.addView(TextView(this).apply {
            text = text("طريقة الدفع", "Payment Method")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(0, dp(8), 0, 0)
        })
        val methods = listOf(
            Pair(text("نقدي / كاش", "Cash"), "cash"),
            Pair(text("إنستاباي InstaPay ⚡", "InstaPay ⚡"), "instapay"),
            Pair(text("فودافون كاش / محفظة إلكترونية 📱", "E-Wallet 📱"), "wallet"),
            Pair(text("تحويل بنكي مباشر 🏦", "Bank Transfer 🏦"), "bank_transfer")
        )
        val methodSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, methods.map { it.first })
        }
        dialogView.addView(methodSpinner)

        dialogView.addView(TextView(this).apply {
            text = text("رقم المرجع / الإيصال (اختياري)", "Reference / Receipt No (Optional)")
            textSize = 11.5f
            typeface = cairoFont
            setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
            setPadding(0, dp(8), 0, 0)
        })
        val refInput = EditText(this).apply {
            hint = text("تلقائي (مثل: REC-2026-001)", "Auto generated if blank")
        }
        dialogView.addView(refInput)

        AlertDialog.Builder(this)
            .setTitle(text("⚡ تسجيل تحصيل فوري", "⚡ Quick Collection"))
            .setView(dialogView)
            .setPositiveButton(text("تأكيد واستلام المبلغ ✓", "Confirm Receipt ✓")) { _, _ ->
                val amt = amountInput.text.toString().trim()
                if (amt.isEmpty() || (amt.toDoubleOrNull() ?: 0.0) <= 0.0) {
                    Toast.makeText(this, text("يرجى إدخال مبلغ صحيح", "Please enter a valid amount"), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedMethod = methods[methodSpinner.selectedItemPosition].second
                val ref = refInput.text.toString().trim()

                val payload = JSONObject().apply {
                    put("unit_id", u.optString("unit_id"))
                    put("party_id", u.optString("party_id"))
                    put("amount", amt)
                    put("method", selectedMethod)
                    put("payment_channel", selectedMethod)
                    put("reference", ref)
                    put("month", currentMonth)
                }

                call("collections/quick-collect", "POST", payload) { raw ->
                    val resp = JSONObject(raw)
                    val recNo = resp.optString("receipt_reference", "")
                    Toast.makeText(this, text("تم التحصيل بنجاح! سند رقم: $recNo", "Collection successful! Receipt: $recNo"), Toast.LENGTH_LONG).show()
                    monthlyCollections(currentMonth)
                }
            }
            .setNegativeButton(text("إلغاء", "Cancel"), null)
            .show()
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
        safeAddView(screen, searchRow)

        val recordsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        safeAddView(screen, recordsContainer)

        fun renderCards(records: List<JSONObject>, relCaches: Map<String, Map<String, String>>) {
            recordsContainer.removeAllViews()
            countBadge.text = "${records.size} " + text("سجل", "items")

            if (records.isEmpty()) {
                val emptyCard = card(addToScreen = false) {
                    addView(TextView(this@MainActivity).apply {
                        text = text("لا توجد سجلات مطابقة في هذا القسم", "No matching records found")
                        textSize = 13f
                        typeface = cairoFont
                        setTextColor(Color.parseColor(if (dark) "#94A3B8" else "#64748B"))
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    })
                }
                safeAddView(recordsContainer, emptyCard)
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
                    "units" -> {
                        val num = row.optString("unit_number").ifEmpty { row.optString("name", row.optString("id").take(6)) }
                        text("شقة / وحدة ", "Unit ") + num
                    }
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

                val itemCard = card(addToScreen = false) {
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

                    if (entity == "units") {
                        val occ = row.optString("occupancy_state", "residing")
                        val occTv = TextView(this@MainActivity).apply {
                            text = when (occ) {
                                "closed" -> text("🟡 مغلقة", "🟡 Closed")
                                "finishing" -> text("🔵 تشطيب", "🔵 Finishing")
                                else -> text("🟢 مقيمة", "🟢 Residing")
                            }
                            textSize = 10f
                            typeface = Typeface.create(cairoFont, Typeface.BOLD)
                            setTextColor(Color.parseColor(when (occ) {
                                "closed" -> if (dark) "#FDE047" else "#854D0E"
                                "finishing" -> if (dark) "#93C5FD" else "#1E40AF"
                                else -> if (dark) "#86EFAC" else "#166534"
                            }))
                            setPadding(dp(7), dp(3), dp(7), dp(3))
                            background = roundedDrawable(
                                Color.parseColor(when (occ) {
                                    "closed" -> if (dark) "#422006" else "#FEF9C3"
                                    "finishing" -> if (dark) "#172554" else "#DBEAFE"
                                    else -> if (dark) "#052E16" else "#DCFCE7"
                                }),
                                8f
                            )
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                marginEnd = dp(6)
                            }
                        }
                        topRow.addView(occTv)
                    }

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
                                "owned" -> text("وحدة تمليك", "Owned")
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
                    if (entity == "units") {
                        val occ = row.optString("occupancy_state", "residing")
                        val occName = when (occ) {
                            "closed" -> text("مغلقة 🟡", "Closed 🟡")
                            "finishing" -> text("تحت التشطيب 🔵", "Finishing 🔵")
                            else -> text("مشغولة ومقيمة 🟢", "Residing 🟢")
                        }
                        detailsList.add(Pair(text("حالة الإشغال:", "Occupancy:"), occName))
                        val phone = row.optString("resident_phone")
                        if (phone.isNotEmpty()) detailsList.add(Pair(text("هاتف الساكن:", "Phone:"), phone))
                    }
                    if (propName.isNotEmpty()) detailsList.add(Pair(text("العقار / البرج:", "Property:"), propName))
                    if (unitName.isNotEmpty()) detailsList.add(Pair(text("الوحدة السكنية:", "Unit:"), unitName))
                    if (partyName.isNotEmpty()) detailsList.add(Pair(text("الطرف المعني:", "Resident / Party:"), partyName))
                    if (techName.isNotEmpty()) detailsList.add(Pair(text("الفني المعتمد:", "Technician:"), techName))
                    if (vendorName.isNotEmpty()) detailsList.add(Pair(text("المورد / المقاول:", "Vendor:"), vendorName))

                    if (row.has("unit_number") && row.optString("unit_number").isNotEmpty()) detailsList.add(Pair(text("رقم الوحدة:", "Unit Number:"), row.optString("unit_number")))
                    if (row.has("name") && row.optString("name") != row.optString("unit_number") && row.optString("name").isNotEmpty()) detailsList.add(Pair(text("اسم ومسمى الوحدة:", "Unit Label:"), row.optString("name")))
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
                                (reasonInput.parent as? ViewGroup)?.removeView(reasonInput)
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
                safeAddView(recordsContainer, itemCard)
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
                safeAddView(screen, unitSpinner)

                call("parties", onError = { /* residents have restricted access to parties table */ }) { partyRaw ->
                    val parties = try { JSONArray(partyRaw) } catch (_: Exception) { JSONArray() }
                    val partySpinner = if (parties.length() > 0) Spinner(this) else null
                    if (partySpinner != null) {
                        partySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (0 until parties.length()).map {
                            val p = parties.getJSONObject(it)
                            p.optString("name", p.optString("id"))
                        })
                        label(text("الساكن / المسدد", "Resident / Payer"), 13, true)
                        safeAddView(screen, partySpinner)
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
        safeAddView(screen, spinner)
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
        "properties" to listOf("name", "address", "instapay_id", "wallet_number"),
        "units" to listOf("property_id", "unit_number", "floor", "occupancy_state", "resident_phone", "area"),
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
        "unit_number" to Pair("رقم الوحدة / الشقة", "Unit Number"),
        "name" to Pair("اسم ومسمى الوحدة (اختياري)", "Unit Name / Label (Optional)"),
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
        "invoice_id" to Pair("رقم الفاتورة", "Invoice ID"),
        "instapay_id" to Pair("معرّف إنستاباي InstaPay IPA", "InstaPay IPA"),
        "wallet_number" to Pair("رقم المحفظة الإلكترونية", "E-Wallet Number"),
        "occupancy_state" to Pair("حالة إشغال الوحدة", "Occupancy State"),
        "resident_phone" to Pair("رقم هاتف الساكن (واتساب)", "Resident Phone (WhatsApp)")
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
                    key == "occupancy_state" && entity == "units" -> listOf("residing", "closed", "finishing")
                    key == "ownership_type" && entity == "units" -> listOf("owned", "rental")
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
                        safeAddView(screen, notice)
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
                        safeAddView(screen, spinner)
                        values[key] = { items.getOrNull(spinner.selectedItemPosition)?.first ?: "" }
                    }
                } else if (options.isNotEmpty()) {
                    label(fieldTitle(key), 13, true)
                    val spinner = Spinner(this)
                    spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options.map { optionTitle(it) })
                    val defaultIdx = if (initial.isNotEmpty()) options.indexOf(initial).coerceAtLeast(0) else 0
                    spinner.setSelection(defaultIdx)
                    safeAddView(screen, spinner)
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
                if (entity == "units") {
                    val uNum = payload.optString("unit_number").trim()
                    val uName = payload.optString("name").trim()
                    if (uNum.isNotEmpty() && uName.isEmpty()) payload.put("name", uNum)
                    if (uName.isNotEmpty() && uNum.isEmpty()) payload.put("unit_number", uName)
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
