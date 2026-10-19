package com.example.jetpack.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val russian: Locale = Locale.forLanguageTag("ru-RU")

fun formatRub(value: BigDecimal): String {
    val format = NumberFormat.getNumberInstance(russian).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return format.format(value) + "\u00A0₽"
}

fun formatPercent(value: BigDecimal): String {
    val format = NumberFormat.getNumberInstance(russian).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    return format.format(value) + "%"
}

fun termInWords(months: Int): String {
    if (months <= 0) return "срок не задан"
    val years = months / 12
    val rest = months % 12
    val parts = ArrayList<String>(2)
    if (years > 0) parts += "${years}\u00A0${plural(years, "год", "года", "лет")}"
    if (rest > 0) parts += "${rest}\u00A0${plural(rest, "месяц", "месяца", "месяцев")}"
    return parts.joinToString(" ")
}

fun plural(value: Int, one: String, few: String, many: String): String {
    val n = value % 100
    if (n in 11..14) return many
    return when (value % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}
