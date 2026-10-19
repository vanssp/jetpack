package com.example.jetpack.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class LoanType {
    ANNUITY,
    DIFFERENTIATED,
}

data class PaymentRow(
    val month: Int,
    val payment: BigDecimal,
    val principal: BigDecimal,
    val interest: BigDecimal,
    val balance: BigDecimal,
)

data class LoanResult(
    val monthlyPayment: BigDecimal,
    val lastPayment: BigDecimal,
    val totalPayment: BigDecimal,
    val totalInterest: BigDecimal,
    val schedule: List<PaymentRow>,
)

object LoanCalculator {
    private val HUNDRED = BigDecimal(100)
    private val MONTHS_IN_YEAR = BigDecimal(12)
    private val KOPECKS = 2
    private val ZERO = BigDecimal.ZERO.setScale(KOPECKS)

    fun calculate(
        amount: BigDecimal,
        annualRatePercent: BigDecimal,
        months: Int,
        type: LoanType,
    ): LoanResult {
        require(amount.signum() > 0) { "amount" }
        require(annualRatePercent.signum() >= 0) { "rate" }
        require(months > 0) { "months" }

        val principal = money(amount)
        val monthlyRate = annualRatePercent
            .divide(HUNDRED.multiply(MONTHS_IN_YEAR), 12, RoundingMode.HALF_UP)
        return when (type) {
            LoanType.ANNUITY -> annuity(principal, monthlyRate, months)
            LoanType.DIFFERENTIATED -> differentiated(principal, monthlyRate, months)
        }
    }

    private fun annuity(amount: BigDecimal, monthlyRate: BigDecimal, months: Int): LoanResult {
        val annuityPayment = if (monthlyRate.signum() == 0) {
            amount.divide(BigDecimal(months), KOPECKS, RoundingMode.HALF_UP)
        } else {
            val growth = BigDecimal.ONE.add(monthlyRate).pow(months)
            money(
                amount.multiply(monthlyRate).multiply(growth)
                    .divide(growth.subtract(BigDecimal.ONE), 8, RoundingMode.HALF_UP),
            )
        }
        return buildSchedule(amount, monthlyRate, months) { month, balance, interest ->
            if (month == months) {
                balance
            } else {
                money(annuityPayment.subtract(interest)).coerceIn(ZERO, balance)
            }
        }
    }

    private fun differentiated(amount: BigDecimal, monthlyRate: BigDecimal, months: Int): LoanResult {
        val principalPart = amount.divide(BigDecimal(months), KOPECKS, RoundingMode.DOWN)
        return buildSchedule(amount, monthlyRate, months) { month, balance, _ ->
            if (month == months) balance else principalPart.min(balance)
        }
    }

    private fun buildSchedule(
        amount: BigDecimal,
        monthlyRate: BigDecimal,
        months: Int,
        principalOf: (month: Int, balance: BigDecimal, interest: BigDecimal) -> BigDecimal,
    ): LoanResult {
        var balance = amount
        var total = ZERO
        val rows = ArrayList<PaymentRow>(months)
        for (month in 1..months) {
            val interest = money(balance.multiply(monthlyRate))
            val principal = money(principalOf(month, balance, interest)).coerceIn(ZERO, balance)
            val payment = money(principal.add(interest))
            balance = money(balance.subtract(principal))
            if (month == months) balance = ZERO
            total = total.add(payment)
            rows += PaymentRow(month, payment, principal, interest, balance)
        }
        val totalPayment = money(total)
        return LoanResult(
            monthlyPayment = rows.first().payment,
            lastPayment = rows.last().payment,
            totalPayment = totalPayment,
            totalInterest = money(totalPayment.subtract(amount)),
            schedule = rows,
        )
    }

    private fun money(value: BigDecimal): BigDecimal = value.setScale(KOPECKS, RoundingMode.HALF_UP)
}

private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal): BigDecimal = when {
    this < min -> min
    this > max -> max
    else -> this
}
