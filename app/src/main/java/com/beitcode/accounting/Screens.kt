package com.beitcode.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun HomeScreen(l: Ledger) {
    val t = l.totals(null, null)
    fun sum(type: AccountType): Double {
        var s = 0.0
        for (a in l.accounts) if (a.type == type) s += l.balanceOf(a, t)
        return s
    }
    val assets = sum(AccountType.ASSET)
    val liab = sum(AccountType.LIABILITY)
    val eq = sum(AccountType.EQUITY)
    val rev = sum(AccountType.REVENUE)
    val exp = sum(AccountType.EXPENSE)
    val profit = rev - exp
    val recent = l.entries
        .sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.number })
        .take(5)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("الأصول", l.fmt(assets), Ink, Modifier.weight(1f))
            Stat("الخصوم", l.fmt(liab), Ink, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("حقوق الملكية", l.fmt(eq), Ink, Modifier.weight(1f))
            Stat("صافي الربح", l.fmt(profit), if (profit >= 0) Pos else Neg, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("الإيرادات", l.fmt(rev), Pos, Modifier.weight(1f))
            Stat("المصروفات", l.fmt(exp), Neg, Modifier.weight(1f))
        }
        Title("آخر القيود")
        if (recent.isEmpty()) Text("لا توجد قيود مسجلة بعد.", color = Muted)
        recent.forEach { e -> EntryRow(l, e) {} }
    }
}

@Composable
fun AccountsScreen(l: Ledger, onOpen: (Long) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    val t = l.totals(null, null)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("دليل الحسابات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = { adding = true }) { Text("حساب جديد") }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountType.values().sortedBy { it.order }.forEach { type ->
                val list = l.accounts.filter { it.type == type }.sortedBy { it.code }
                item {
                    Text(
                        type.label, color = Navy, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(list, key = { it.id }) { a ->
                    Panel(Modifier.clickable { onOpen(a.id) }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(a.code + "  " + a.name, modifier = Modifier.weight(1f))
                            Text(l.fmt(l.balanceOf(a, t)), color = Muted)
                        }
                    }
                }
            }
        }
    }
    if (adding) AddAccountDialog(l) { adding = false }
}

@Composable
fun AddAccountDialog(l: Ledger, onDone: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.ASSET) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("حساب جديد") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field(code, { code = it }, "رقم الحساب", KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                Field(name, { name = it }, "اسم الحساب")
                Spacer(Modifier.height(8.dp))
                AccountType.values().sortedBy { it.order }.forEach { tp ->
                    Row(
                        Modifier.fillMaxWidth().clickable { type = tp },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == tp, onClick = { type = tp })
                        Text(tp.label)
                    }
                }
                if (error.isNotEmpty()) Text(error, color = Neg)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val msg = l.addAccount(code.trim(), name.trim(), type)
                if (msg == null) onDone() else error = msg
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("إلغاء") } }
    )
}

class StmtRow(val e: Entry, val net: Double, val balance: Double)

@Composable
fun AccountStatement(l: Ledger, acc: Account, onBack: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val related = l.entries
        .filter { e -> e.lines.any { it.accountId == acc.id } }
        .sortedWith(compareBy<Entry> { it.date }.thenBy { it.number })
    val stmt = ArrayList<StmtRow>()
    var run = 0.0
    for (e in related) {
        var d = 0.0
        var c = 0.0
        for (ln in e.lines) if (ln.accountId == acc.id) {
            d += ln.debit
            c += ln.credit
        }
        run += if (acc.type.debitNormal) d - c else c - d
        stmt.add(StmtRow(e, d - c, run))
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(acc.code + "  " + acc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(acc.type.label, color = Muted)
            }
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        if (stmt.isEmpty()) {
            Text("لا توجد حركات على هذا الحساب.", color = Muted, modifier = Modifier.padding(12.dp))
            TextButton(onClick = {
                if (confirm) {
                    l.deleteAccount(acc.id)
                    onBack()
                } else confirm = true
            }) { Text(if (confirm) "تأكيد حذف الحساب" else "حذف الحساب", color = Neg) }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stmt, key = { it.e.id }) { r ->
                Panel {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.e.date, color = Muted)
                        Text("قيد " + r.e.number, color = Muted)
                    }
                    Text(r.e.description)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (r.net >= 0) "مدين " + money(r.net) else "دائن " + money(-r.net))
                        Text("الرصيد " + money(r.balance), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen(l: Ledger, onNew: () -> Unit) {
    var open by remember { mutableStateOf<Entry?>(null) }
    val sorted = l.entries.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.number })
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قيود اليومية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = onNew) { Text("قيد جديد") }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sorted, key = { it.id }) { e -> EntryRow(l, e) { open = e } }
        }
    }
    open?.let { e -> EntryDialog(l, e) { open = null } }
}

@Composable
fun EntryDialog(l: Ledger, e: Entry, onClose: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("قيد رقم " + e.number) },
        text = {
            Column {
                Text(e.date, color = Muted)
                Text(e.description, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                e.lines.forEach { ln ->
                    val a = l.accounts.firstOrNull { it.id == ln.accountId }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(a?.name ?: "حساب محذوف", modifier = Modifier.weight(1f))
                        Text(
                            if (ln.debit > 0) "مدين " + l.fmt(ln.debit) else "دائن " + l.fmt(ln.credit)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (confirm) {
                    l.deleteEntry(e.id)
                    onClose()
                } else confirm = true
            }) { Text(if (confirm) "تأكيد الحذف" else "حذف القيد", color = Neg) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("إغلاق") } }
    )
}

class Draft {
    var accountId by mutableStateOf<Long?>(null)
    var debit by mutableStateOf("")
    var credit by mutableStateOf("")
}

@Composable
fun AccountPicker(l: Ledger, d: Draft) {
    var open by remember { mutableStateOf(false) }
    val sel = l.accounts.firstOrNull { it.id == d.accountId }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (sel == null) "اختر الحساب" else sel.code + "  " + sel.name)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            l.accounts.sortedBy { it.code }.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.code + "  " + a.name) },
                    onClick = {
                        d.accountId = a.id
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
fun EntryEditor(l: Ledger, onClose: () -> Unit) {
    var date by remember { mutableStateOf(today()) }
    var desc by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf(Draft(), Draft()) }
    val td = lines.sumOf { parseAmount(it.debit) }
    val tc = lines.sumOf { parseAmount(it.credit) }
    val diff = abs(td - tc)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قيد يومية جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextButton(onClick = onClose) { Text("إلغاء") }
        }
        Field(date, { date = it }, "التاريخ", hint = "2026-01-01")
        Field(desc, { desc = it }, "البيان")
        lines.forEachIndexed { i, d ->
            Panel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("السطر " + (i + 1), color = Muted)
                    if (lines.size > 2) {
                        TextButton(onClick = { lines.removeAt(i) }) { Text("حذف السطر", color = Neg) }
                    }
                }
                AccountPicker(l, d)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(d.debit, { d.debit = it }, "مدين", KeyboardType.Decimal, Modifier.weight(1f))
                    Field(d.credit, { d.credit = it }, "دائن", KeyboardType.Decimal, Modifier.weight(1f))
                }
            }
        }
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي المدين")
                Text(l.fmt(td))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي الدائن")
                Text(l.fmt(tc))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الفرق")
                Text(l.fmt(diff), color = if (diff < 0.005) Pos else Neg)
            }
        }
        if (error.isNotEmpty()) Text(error, color = Neg)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { lines.add(Draft()) }, modifier = Modifier.weight(1f)) {
                Text("إضافة سطر")
            }
            Button(
                onClick = {
                    val used = lines.filter {
                        it.accountId != null || parseAmount(it.debit) > 0 || parseAmount(it.credit) > 0
                    }
                    val msg = when {
                        !validDate(date) -> "صيغة التاريخ غير صحيحة، استخدم السنة-الشهر-اليوم"
                        desc.isBlank() -> "أدخل بيان القيد"
                        used.size < 2 -> "القيد يحتاج إلى سطرين على الأقل"
                        used.any { it.accountId == null } -> "اختر الحساب في كل سطر"
                        used.any { parseAmount(it.debit) > 0 && parseAmount(it.credit) > 0 } ->
                            "لا يمكن إدخال مدين ودائن في نفس السطر"
                        used.any { parseAmount(it.debit) <= 0 && parseAmount(it.credit) <= 0 } ->
                            "أدخل مبلغاً في كل سطر"
                        diff >= 0.005 -> "القيد غير متوازن"
                        else -> null
                    }
                    if (msg != null) {
                        error = msg
                    } else {
                        l.addEntry(
                            date,
                            desc.trim(),
                            used.map { JournalLine(it.accountId!!, parseAmount(it.debit), parseAmount(it.credit)) }
                        )
                        onClose()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("حفظ القيد") }
        }
    }
}

@Composable
fun ReportsScreen(l: Ledger) {
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(0) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    val names = listOf("ميزان المراجعة", "قائمة الدخل", "الميزانية العمومية")
    val fromV = if (validDate(from)) from else null
    val toV = if (validDate(to)) to else null
    val rows = buildReport(l, kind, fromV, toV)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEachIndexed { i, n ->
                val sel = kind == i
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (sel) Navy else Color.White, RoundedCornerShape(4.dp))
                        .clickable { kind = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        n,
                        color = if (sel) Color.White else Ink,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(from, { from = it }, "من تاريخ", modifier = Modifier.weight(1f), hint = "2026-01-01")
            Field(to, { to = it }, "إلى تاريخ", modifier = Modifier.weight(1f), hint = "2026-12-31")
        }
        if (kind == 2) {
            Text("الميزانية تُحسب حتى تاريخ \"إلى\" ويُهمل تاريخ \"من\".", color = Muted, fontSize = 12.sp)
        }
        Panel {
            rows.forEach { r ->
                if (r.strong) Divider(color = Rule, modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        r.label,
                        modifier = Modifier.weight(if (r.two) 2.2f else 2f),
                        color = if (r.header) Muted else Ink,
                        fontWeight = if (r.strong || r.header) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    Text(
                        r.v1,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = if (r.header) Muted else Ink,
                        fontWeight = if (r.strong || r.header) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (r.two) {
                        Text(
                            r.v2,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            color = if (r.header) Muted else Ink,
                            fontWeight = if (r.strong || r.header) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Button(
            onClick = { shareText(ctx, names[kind], reportText(l, names[kind], rows, fromV, toV)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("مشاركة التقرير") }
    }
}

@Composable
fun SettingsScreen(l: Ledger) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var company by remember { mutableStateOf(l.companyName) }
    var cur by remember { mutableStateOf(l.currency) }
    var msg by remember { mutableStateOf("") }
    var askImport by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Panel {
            Title("بيانات المنشأة")
            Spacer(Modifier.height(8.dp))
            Field(company, { company = it }, "اسم المنشأة")
            Spacer(Modifier.height(8.dp))
            Field(cur, { cur = it }, "العملة")
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                l.updateInfo(company.trim(), cur.trim().ifEmpty { "ل.س" })
                msg = "تم حفظ البيانات"
            }) { Text("حفظ") }
        }
        Panel {
            Title("النسخ الاحتياطي")
            Spacer(Modifier.height(4.dp))
            Text(
                "البيانات محفوظة على هذا الجهاز فقط. صدّر نسخة احتياطية وأرسلها إلى نفسك، ولاستعادتها انسخ النص ثم اضغط استيراد.",
                color = Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { shareText(ctx, "نسخة احتياطية", l.toJson()) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تصدير نسخة احتياطية") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { askImport = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("استيراد من الحافظة") }
        }
        Panel {
            Title("حول البرنامج")
            Spacer(Modifier.height(4.dp))
            Text("المحاسب المتكامل - الإصدار 1.0")
            Text("تصميم وتطوير: عمار الإبراهيم", fontWeight = FontWeight.SemiBold)
            Text("Beit Code", color = Muted)
        }
        if (msg.isNotEmpty()) Text(msg, color = Pos)
    }

    if (askImport) {
        AlertDialog(
            onDismissRequest = { askImport = false },
            title = { Text("استيراد نسخة احتياطية") },
            text = { Text("سيتم استبدال جميع البيانات الحالية بالنسخة الموجودة في الحافظة.") },
            confirmButton = {
                TextButton(onClick = {
                    val text = clip.getText()?.text ?: ""
                    if (l.importJson(text)) {
                        company = l.companyName
                        cur = l.currency
                        msg = "تم استيراد البيانات"
                    } else {
                        msg = "النص الموجود في الحافظة ليس نسخة احتياطية صالحة"
                    }
                    askImport = false
                }) { Text("استيراد") }
            },
            dismissButton = { TextButton(onClick = { askImport = false }) { Text("إلغاء") } }
        )
    }
}
