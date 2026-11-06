package com.beitcode.accounting

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Ledger(context: Context) {
    private val file = File(context.filesDir, "ledger.json")

    var companyName by mutableStateOf("")
        private set
    var currency by mutableStateOf("ل.س")
        private set

    val accounts = mutableStateListOf<Account>()
    val entries = mutableStateListOf<Entry>()

    init {
        val loaded = file.exists() && try {
            read(file.readText())
            true
        } catch (e: Exception) {
            false
        }
        if (!loaded) {
            seed()
            save()
        }
    }

    fun fmt(v: Double): String = money(v) + " " + currency

    fun updateInfo(name: String, cur: String) {
        companyName = name
        currency = cur
        save()
    }

    fun addAccount(code: String, name: String, type: AccountType): String? {
        if (code.isEmpty()) return "أدخل رقم الحساب"
        if (name.isEmpty()) return "أدخل اسم الحساب"
        if (accounts.any { it.code == code }) return "رقم الحساب مستخدم من قبل"
        accounts.add(Account(System.nanoTime(), code, name, type))
        save()
        return null
    }

    fun hasMovement(accountId: Long): Boolean =
        entries.any { e -> e.lines.any { it.accountId == accountId } }

    fun deleteAccount(id: Long) {
        if (hasMovement(id)) return
        accounts.removeAll { it.id == id }
        save()
    }

    fun addEntry(date: String, description: String, lines: List<JournalLine>) {
        val number = (entries.maxOfOrNull { it.number } ?: 0) + 1
        entries.add(Entry(System.nanoTime(), number, date, description, lines))
        save()
    }

    fun deleteEntry(id: Long) {
        entries.removeAll { it.id == id }
        save()
    }

    fun totals(from: String?, to: String?): Map<Long, Pair<Double, Double>> {
        val m = HashMap<Long, Pair<Double, Double>>()
        for (e in entries) {
            if (!inRange(e.date, from, to)) continue
            for (l in e.lines) {
                val p = m[l.accountId] ?: Pair(0.0, 0.0)
                m[l.accountId] = Pair(p.first + l.debit, p.second + l.credit)
            }
        }
        return m
    }

    fun balanceOf(a: Account, t: Map<Long, Pair<Double, Double>>): Double {
        val p = t[a.id] ?: return 0.0
        return if (a.type.debitNormal) p.first - p.second else p.second - p.first
    }

    private fun inRange(date: String, from: String?, to: String?): Boolean {
        if (!from.isNullOrBlank() && date < from) return false
        if (!to.isNullOrBlank() && date > to) return false
        return true
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("company", companyName)
        root.put("currency", currency)
        val aa = JSONArray()
        accounts.forEach {
            aa.put(
                JSONObject().put("id", it.id).put("code", it.code)
                    .put("name", it.name).put("type", it.type.name)
            )
        }
        root.put("accounts", aa)
        val ea = JSONArray()
        entries.forEach { e ->
            val la = JSONArray()
            e.lines.forEach {
                la.put(
                    JSONObject().put("account", it.accountId)
                        .put("debit", it.debit).put("credit", it.credit)
                )
            }
            ea.put(
                JSONObject().put("id", e.id).put("number", e.number).put("date", e.date)
                    .put("description", e.description).put("lines", la)
            )
        }
        root.put("entries", ea)
        return root.toString()
    }

    fun importJson(text: String): Boolean {
        return try {
            read(text)
            save()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun read(text: String) {
        val root = JSONObject(text)
        val accs = ArrayList<Account>()
        val aa = root.getJSONArray("accounts")
        for (i in 0 until aa.length()) {
            val o = aa.getJSONObject(i)
            accs.add(
                Account(
                    o.getLong("id"), o.getString("code"), o.getString("name"),
                    AccountType.valueOf(o.getString("type"))
                )
            )
        }
        val ens = ArrayList<Entry>()
        val ea = root.getJSONArray("entries")
        for (i in 0 until ea.length()) {
            val o = ea.getJSONObject(i)
            val la = o.getJSONArray("lines")
            val ls = ArrayList<JournalLine>()
            for (j in 0 until la.length()) {
                val x = la.getJSONObject(j)
                ls.add(JournalLine(x.getLong("account"), x.getDouble("debit"), x.getDouble("credit")))
            }
            ens.add(
                Entry(
                    o.getLong("id"), o.getInt("number"), o.getString("date"),
                    o.getString("description"), ls
                )
            )
        }
        companyName = root.optString("company", "")
        currency = root.optString("currency", "ل.س")
        accounts.clear()
        accounts.addAll(accs)
        entries.clear()
        entries.addAll(ens)
    }

    private fun save() {
        try {
            file.writeText(toJson())
        } catch (e: Exception) {
        }
    }

    private fun seed() {
        val defs = listOf(
            Triple("1101", "الصندوق", AccountType.ASSET),
            Triple("1102", "البنك", AccountType.ASSET),
            Triple("1103", "العملاء", AccountType.ASSET),
            Triple("1104", "المخزون", AccountType.ASSET),
            Triple("1201", "الأصول الثابتة", AccountType.ASSET),
            Triple("2101", "الموردون", AccountType.LIABILITY),
            Triple("2102", "قروض", AccountType.LIABILITY),
            Triple("2103", "مصاريف مستحقة", AccountType.LIABILITY),
            Triple("3101", "رأس المال", AccountType.EQUITY),
            Triple("3102", "جاري المالك", AccountType.EQUITY),
            Triple("4101", "إيرادات المبيعات", AccountType.REVENUE),
            Triple("4102", "إيرادات أخرى", AccountType.REVENUE),
            Triple("5101", "تكلفة البضاعة المباعة", AccountType.EXPENSE),
            Triple("5102", "الرواتب والأجور", AccountType.EXPENSE),
            Triple("5103", "الإيجار", AccountType.EXPENSE),
            Triple("5104", "الكهرباء والماء والاتصالات", AccountType.EXPENSE),
            Triple("5105", "مصاريف عمومية", AccountType.EXPENSE)
        )
        accounts.clear()
        defs.forEachIndexed { i, d ->
            accounts.add(Account((i + 1).toLong(), d.first, d.second, d.third))
        }
    }
}
