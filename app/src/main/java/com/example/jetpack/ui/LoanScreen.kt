@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.jetpack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.jetpack.domain.LoanCalculator
import com.example.jetpack.domain.LoanResult
import com.example.jetpack.domain.LoanType
import com.example.jetpack.domain.formatPercent
import com.example.jetpack.domain.formatRub
import com.example.jetpack.domain.termInWords
import java.math.BigDecimal
import java.math.RoundingMode

private const val DEFAULT_AMOUNT = "1000000"
private const val DEFAULT_RATE = "15"
private const val DEFAULT_MONTHS = "24"
private const val MAX_MONTHS = 480

private val termPresets = listOf(
    6 to "6 мес",
    12 to "1 год",
    24 to "2 года",
    36 to "3 года",
    60 to "5 лет",
    84 to "7 лет",
    120 to "10 лет",
    180 to "15 лет",
    240 to "20 лет",
    360 to "30 лет",
)

private data class LoanInput(
    val amount: BigDecimal?,
    val rate: BigDecimal?,
    val months: Int?,
    val error: String?,
)

@Composable
fun LoanScreen() {
    var amount by rememberSaveable { mutableStateOf(DEFAULT_AMOUNT) }
    var rate by rememberSaveable { mutableStateOf(DEFAULT_RATE) }
    var monthsText by rememberSaveable { mutableStateOf(DEFAULT_MONTHS) }
    var type by rememberSaveable { mutableStateOf(LoanType.ANNUITY) }

    val input = parseInput(amount, rate, monthsText)
    val result = if (input.error == null && input.amount != null && input.rate != null && input.months != null) {
        LoanCalculator.calculate(input.amount, input.rate, input.months, type)
    } else {
        null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Кредитный калькулятор") },
                actions = {
                    TextButton(onClick = {
                        amount = DEFAULT_AMOUNT
                        rate = DEFAULT_RATE
                        monthsText = DEFAULT_MONTHS
                        type = LoanType.ANNUITY
                    }) {
                        Text("Сброс")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Платёж, переплата и график в рублях",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ResultHero(
                    result = result,
                    type = type,
                    months = input.months,
                    rate = input.rate,
                    amount = input.amount,
                    error = input.error,
                )
            }
            item {
                InputCard(
                    amount = amount,
                    onAmount = { amount = it.filter { char -> char.isDigit() }.take(12) },
                    rate = rate,
                    onRate = { rate = sanitizeRate(it) },
                    monthsText = monthsText,
                    onMonths = { monthsText = it.filter { char -> char.isDigit() }.take(3) },
                    type = type,
                    onType = { type = it },
                )
            }
            if (result != null) {
                item {
                    Text(
                        "График платежей",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Каждый месяц: платёж, основной долг, проценты и остаток",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(result.schedule, key = { it.month }) { row ->
                    ScheduleCard(row)
                }
                item {
                    Text(
                        "Расчёт ориентировочный: банк может округлять копейки иначе.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
            } else {
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun ResultHero(
    result: LoanResult?,
    type: LoanType,
    months: Int?,
    rate: BigDecimal?,
    amount: BigDecimal?,
    error: String?,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result == null || months == null || rate == null || amount == null) {
                Text("Ежемесячный платёж", style = MaterialTheme.typography.labelLarge)
                Text(
                    error ?: "Введите сумму, ставку и срок",
                    style = MaterialTheme.typography.headlineSmall,
                )
                return@Column
            }
            val paymentLabel = if (type == LoanType.ANNUITY) "Ежемесячный платёж" else "Первый платёж"
            Text(paymentLabel, style = MaterialTheme.typography.labelLarge)
            Text(
                formatRub(result.monthlyPayment),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            val typeLabel = if (type == LoanType.ANNUITY) "Аннуитет" else "Дифференцированный"
            Text(
                "$typeLabel · ${termInWords(months)} · ${formatPercent(rate)} годовых",
                style = MaterialTheme.typography.bodyMedium,
            )
            val paymentGap = result.monthlyPayment.subtract(result.lastPayment).abs()
            if (type == LoanType.DIFFERENTIATED || paymentGap > BigDecimal.ONE) {
                Text(
                    "Последний платёж ${formatRub(result.lastPayment)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Переплата", formatRub(result.totalInterest), Modifier.weight(1f))
                StatPill("Всего выплат", formatRub(result.totalPayment), Modifier.weight(1f))
            }
            Text(
                "Переплата ${formatPercent(overpaymentShare(result, amount))} от суммы кредита",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InputCard(
    amount: String,
    onAmount: (String) -> Unit,
    rate: String,
    onRate: (String) -> Unit,
    monthsText: String,
    onMonths: (String) -> Unit,
    type: LoanType,
    onType: (LoanType) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyField("Сумма кредита, ₽", amount, onAmount, KeyboardType.Number)
            MoneyField("Ставка, % годовых", rate, onRate, KeyboardType.Decimal)
            MoneyField("Срок, месяцев", monthsText, onMonths, KeyboardType.Number)
            val months = monthsText.toIntOrNull()
            if (months != null && months > 0) {
                Text(
                    termInWords(months),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                termPresets.forEach { (value, label) ->
                    FilterChip(
                        selected = monthsText == value.toString(),
                        onClick = { onMonths(value.toString()) },
                        label = { Text(label) },
                    )
                }
            }
            Text("Тип платежа", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == LoanType.ANNUITY,
                    onClick = { onType(LoanType.ANNUITY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Аннуитет") },
                )
                SegmentedButton(
                    selected = type == LoanType.DIFFERENTIATED,
                    onClick = { onType(LoanType.DIFFERENTIATED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Дифференц.") },
                )
            }
            Text(
                if (type == LoanType.ANNUITY) {
                    "Одинаковый платёж каждый месяц. Удобно, когда нужен фиксированный бюджет."
                } else {
                    "Основной долг равными частями, платёж уменьшается к концу срока."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboardType: KeyboardType,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ScheduleCard(row: com.example.jetpack.domain.PaymentRow) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${row.month}-й месяц", style = MaterialTheme.typography.titleSmall)
                Text(formatRub(row.payment), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScheduleBit("Долг", formatRub(row.principal), Modifier.weight(1f))
                ScheduleBit("Проценты", formatRub(row.interest), Modifier.weight(1f))
                ScheduleBit("Остаток", formatRub(row.balance), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScheduleBit(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun parseInput(amountRaw: String, rateRaw: String, monthsRaw: String): LoanInput {
    if (amountRaw.isBlank() || rateRaw.isBlank() || monthsRaw.isBlank()) {
        return LoanInput(null, null, null, null)
    }
    val amount = amountRaw.toBigDecimalOrNull()
    val rate = rateRaw.replace(',', '.').toBigDecimalOrNull()
    val months = monthsRaw.toIntOrNull()
    val error = when {
        amount == null || amount.signum() <= 0 -> "Укажите сумму больше нуля"
        rate == null || rate.signum() < 0 -> "Ставка не может быть отрицательной"
        rate > BigDecimal(1000) -> "Проверьте ставку"
        months == null || months <= 0 -> "Срок — хотя бы один месяц"
        months > MAX_MONTHS -> "Срок не больше 40 лет"
        else -> null
    }
    return LoanInput(amount, rate, months, error)
}

private fun sanitizeRate(raw: String): String {
    val cleaned = buildString {
        var separator = false
        raw.forEach { char ->
            when {
                char.isDigit() -> append(char)
                (char == '.' || char == ',') && !separator -> {
                    append(',')
                    separator = true
                }
            }
        }
    }
    val head = cleaned.substringBefore(',')
    val tail = cleaned.substringAfter(',', "")
    return if (',' in cleaned) head.take(4) + "," + tail.take(2) else head.take(4)
}

private fun overpaymentShare(result: LoanResult, amount: BigDecimal): BigDecimal {
    if (amount.signum() == 0) return BigDecimal.ZERO
    return result.totalInterest.multiply(BigDecimal(100)).divide(amount, 1, RoundingMode.HALF_UP)
}
