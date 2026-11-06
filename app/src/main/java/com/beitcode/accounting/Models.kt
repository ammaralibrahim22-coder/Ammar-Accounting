package com.beitcode.accounting

enum class AccountType(val label: String, val debitNormal: Boolean, val order: Int) {
    ASSET("الأصول", true, 1),
    LIABILITY("الخصوم", false, 2),
    EQUITY("حقوق الملكية", false, 3),
    REVENUE("الإيرادات", false, 4),
    EXPENSE("المصروفات", true, 5)
}

data class Account(val id: Long, val code: String, val name: String, val type: AccountType)

data class JournalLine(val accountId: Long, val debit: Double, val credit: Double)

data class Entry(
    val id: Long,
    val number: Int,
    val date: String,
    val description: String,
    val lines: List<JournalLine>
)
