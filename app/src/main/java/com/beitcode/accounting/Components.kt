package com.beitcode.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Section(val label: String) {
    HOME("الرئيسية"),
    ACCOUNTS("الحسابات"),
    JOURNAL("القيود"),
    REPORTS("التقارير"),
    SETTINGS("الإعدادات")
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun Stat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Text(label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Title(text: String) {
    Text(text, color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    hint: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (hint.isNotEmpty()) Text(hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun Header(title: String, sub: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (sub.isNotEmpty()) Text(sub, color = Color(0xFFC5D1E3), fontSize = 12.sp)
    }
}

@Composable
fun BottomBar(current: Section, onSelect: (Section) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Divider(color = Rule)
        Row(Modifier.fillMaxWidth()) {
            Section.values().forEach { s ->
                val sel = s == current
                Column(
                    Modifier.weight(1f).clickable { onSelect(s) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (sel) Navy else Color.Transparent)
                    )
                    Text(
                        s.label,
                        color = if (sel) Navy else Muted,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EntryRow(l: Ledger, e: Entry, onClick: () -> Unit) {
    val total = e.lines.sumOf { it.debit }
    Panel(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("قيد رقم " + e.number, fontWeight = FontWeight.SemiBold)
            Text(e.date, color = Muted)
        }
        Spacer(Modifier.height(4.dp))
        Text(e.description)
        Spacer(Modifier.height(4.dp))
        Text(l.fmt(total), color = Muted)
    }
}
