package com.beitcode.accounting

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun money(v: Double): String {
    val x = if (abs(v) < 0.005) 0.0 else v
    return String.format(Locale.US, "%,.2f", x)
}

fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun validDate(s: String): Boolean {
    if (s.length != 10) return false
    return try {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        f.isLenient = false
        f.parse(s) != null
    } catch (e: Exception) {
        false
    }
}

fun parseAmount(s: String): Double = s.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

fun shareText(ctx: Context, title: String, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(i, title))
}

class TableRow(
    val label: String,
    val v1: String = "",
    val v2: String = "",
    val strong: Boolean = false,
    val header: Boolean = false,
    val two: Boolean = false
)

fun buildReport(l: Ledger, kind: Int, from: String?, to: String?): List<TableRow> {
    val rows = ArrayList<TableRow>()
    val sorted = l.accounts.sortedBy { it.code }
    when (kind) {
        0 -> {
            val t = l.totals(from, to)
            var td = 0.0
            var tc = 0.0
            rows.add(TableRow("الحساب", "مدين", "دائن", header = true, two = true))
            for (a in sorted) {
                val p = t[a.id] ?: continue
                val net = p.first - p.second
                if (abs(net) < 0.005) continue
                val d = if (net > 0) net else 0.0
                val c = if (net < 0) -net else 0.0
                td += d
                tc += c
                rows.add(
                    TableRow(
                        a.code + "  " + a.name,
                        if (d > 0) money(d) else "",
                        if (c > 0) money(c) else "",
                        two = true
                    )
                )
            }
            rows.add(TableRow("الإجمالي", money(td), money(tc), strong = true, two = true))
        }
        1 -> {
            val t = l.totals(from, to)
            rows.add(TableRow("البيان", "المبلغ", header = true))
            fun block(title: String, type: AccountType): Double {
                rows.add(TableRow(title, strong = true))
                var s = 0.0
                for (a in sorted.filter { it.type == type }) {
                    val b = l.balanceOf(a, t)
                    if (abs(b) >= 0.005) {
                        s += b
                        rows.add(TableRow("   " + a.name, money(b)))
                    }
                }
                rows.add(TableRow("إجمالي " + title, money(s), strong = true))
                return s
            }
            val rev = block("الإيرادات", AccountType.REVENUE)
            val exp = block("المصروفات", AccountType.EXPENSE)
            val net = rev - exp
            rows.add(
                TableRow(if (net >= 0) "صافي الربح" else "صافي الخسارة", money(abs(net)), strong = true)
            )
        }
        else -> {
            val t = l.totals(null, to)
            rows.add(TableRow("البيان", "المبلغ", header = true))
            fun block(title: String, type: AccountType): Double {
                rows.add(TableRow(title, strong = true))
                var s = 0.0
                for (a in sorted.filter { it.type == type }) {
                    val b = l.balanceOf(a, t)
                    if (abs(b) >= 0.005) {
                        s += b
                        rows.add(TableRow("   " + a.name, money(b)))
                    }
                }
                rows.add(TableRow("إجمالي " + title, money(s), strong = true))
                return s
            }
            block("الأصول", AccountType.ASSET)
            val liab = block("الخصوم", AccountType.LIABILITY)
            val eq = block("حقوق الملكية", AccountType.EQUITY)
            var profit = 0.0
            for (a in sorted) {
                if (a.type == AccountType.REVENUE) profit += l.balanceOf(a, t)
                if (a.type == AccountType.EXPENSE) profit -= l.balanceOf(a, t)
            }
            rows.add(TableRow("صافي الربح (الخسارة) المرحّل", money(profit)))
            rows.add(
                TableRow("مجموع الخصوم وحقوق الملكية", money(liab + eq + profit), strong = true)
            )
        }
    }
    return rows
}

fun reportText(l: Ledger, title: String, rows: List<TableRow>, from: String?, to: String?): String {
    val sb = StringBuilder()
    if (l.companyName.isNotEmpty()) sb.append(l.companyName).append('\n')
    sb.append(title).append('\n')
    val period = when {
        from != null && to != null -> "الفترة: من $from إلى $to"
        from != null -> "الفترة: من $from"
        to != null -> "حتى تاريخ $to"
        else -> "جميع الفترات"
    }
    sb.append(period).append("  (").append(l.currency).append(")\n\n")
    for (r in rows) {
        sb.append(r.label)
        if (r.v1.isNotEmpty()) sb.append("   ").append(r.v1)
        if (r.two && r.v2.isNotEmpty()) sb.append("   ").append(r.v2)
        sb.append('\n')
    }
    return sb.toString()
}
