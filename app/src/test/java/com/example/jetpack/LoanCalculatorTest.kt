package com.example.jetpack

import com.example.jetpack.domain.LoanCalculator
import com.example.jetpack.domain.LoanType
import com.example.jetpack.domain.plural
import com.example.jetpack.domain.termInWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class LoanCalculatorTest {
    @Test
    fun annuityPaymentMatchesStandardFormula() {
        val result = LoanCalculator.calculate(
            amount = BigDecimal("100000"),
            annualRatePercent = BigDecimal("12"),
            months = 12,
            type = LoanType.ANNUITY,
        )
        assertTrue(result.monthlyPayment.toDouble() in 8884.87..8884.89)
        assertEquals(12, result.schedule.size)
        assertInvariants(result, BigDecimal("100000.00"))
    }

    @Test
    fun zeroRateSplitsAmountWithoutInterest() {
        val result = LoanCalculator.calculate(
            amount = BigDecimal("1000"),
            annualRatePercent = BigDecimal.ZERO,
            months = 3,
            type = LoanType.ANNUITY,
        )
        assertEquals(0, result.totalInterest.compareTo(BigDecimal("0.00")))
        assertEquals(0, result.totalPayment.compareTo(BigDecimal("1000.00")))
        assertInvariants(result, BigDecimal("1000.00"))
    }

    @Test
    fun differentiatedPaymentShrinksAndPrincipalSumsToAmount() {
        val result = LoanCalculator.calculate(
            amount = BigDecimal("120000"),
            annualRatePercent = BigDecimal("15"),
            months = 12,
            type = LoanType.DIFFERENTIATED,
        )
        assertTrue(result.monthlyPayment > result.lastPayment)
        assertEquals(0, result.schedule.first().principal.compareTo(BigDecimal("10000.00")))
        assertInvariants(result, BigDecimal("120000.00"))
    }

    @Test
    fun termWordsUseRussianPlurals() {
        assertEquals("2\u00A0года", termInWords(24))
        assertEquals("1\u00A0год 1\u00A0месяц", termInWords(13))
        assertEquals("5\u00A0лет", termInWords(60))
        assertEquals("месяцев", plural(11, "месяц", "месяца", "месяцев"))
    }

    private fun assertInvariants(result: com.example.jetpack.domain.LoanResult, amount: BigDecimal) {
        val principal = result.schedule.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.principal) }
        val payments = result.schedule.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.payment) }
        assertEquals(0, principal.setScale(2).compareTo(amount))
        assertEquals(0, payments.setScale(2).compareTo(result.totalPayment))
        assertEquals(0, result.schedule.last().balance.compareTo(BigDecimal("0.00")))
        result.schedule.forEach { row ->
            assertEquals(0, row.payment.compareTo(row.principal.add(row.interest)))
        }
    }
}
