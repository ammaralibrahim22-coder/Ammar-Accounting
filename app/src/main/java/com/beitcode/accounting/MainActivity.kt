package com.beitcode.accounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private val ledger by lazy { Ledger(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme {
                    App(ledger)
                }
            }
        }
    }
}

@Composable
fun App(l: Ledger) {
    var section by remember { mutableStateOf(Section.HOME) }
    var editing by remember { mutableStateOf(false) }
    var ledgerOf by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = editing || ledgerOf != null) {
        if (editing) editing = false else ledgerOf = null
    }

    val title = when {
        editing -> "قيد جديد"
        ledgerOf != null -> "كشف حساب"
        else -> section.label
    }
    val sub = if (l.companyName.isNotEmpty()) l.companyName else "المحاسب المتكامل"

    Column(Modifier.fillMaxSize().background(Bg)) {
        Header(title, sub)
        Box(Modifier.weight(1f)) {
            when {
                editing -> EntryEditor(l) { editing = false }
                ledgerOf != null -> {
                    val acc = l.accounts.firstOrNull { it.id == ledgerOf }
                    if (acc != null) AccountStatement(l, acc) { ledgerOf = null }
                }
                else -> when (section) {
                    Section.HOME -> HomeScreen(l)
                    Section.ACCOUNTS -> AccountsScreen(l) { ledgerOf = it }
                    Section.JOURNAL -> JournalScreen(l) { editing = true }
                    Section.REPORTS -> ReportsScreen(l)
                    Section.SETTINGS -> SettingsScreen(l)
                }
            }
        }
        if (!editing && ledgerOf == null) {
            BottomBar(section) { section = it }
        }
    }
}
