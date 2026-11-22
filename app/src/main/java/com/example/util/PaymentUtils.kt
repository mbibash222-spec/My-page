package com.example.util

import java.util.Locale

enum class CardBrand(val label: String) {
    VISA("VISA"),
    MASTERCARD("MASTERCARD"),
    AMEX("AMEX"),
    CARD("CARD")
}

object PaymentUtils {

    fun detectCardBrand(digitsOnly: String): CardBrand {
        val clean = digitsOnly.filter { it.isDigit() }
        return when {
            clean.startsWith("4") -> CardBrand.VISA
            clean.startsWith("5") || clean.startsWith("2") -> CardBrand.MASTERCARD
            clean.startsWith("34") || clean.startsWith("37") -> CardBrand.AMEX
            else -> CardBrand.CARD
        }
    }

    fun isValidLuhn(cardNumber: String): Boolean {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length !in 13..19) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun isValidExpiry(expiry: String): Boolean {
        val digits = expiry.filter { it.isDigit() }
        if (digits.length != 4) return false
        val month = digits.substring(0, 2).toIntOrNull() ?: return false
        val year = digits.substring(2, 4).toIntOrNull() ?: return false
        return month in 1..12 && year in 25..40
    }

    fun isValidCvv(cvv: String): Boolean {
        val digits = cvv.filter { it.isDigit() }
        return digits.length in 3..4
    }

    fun isValidUpi(upiId: String): Boolean {
        val trimmed = upiId.trim()
        return trimmed.length >= 5 && trimmed.contains("@") && !trimmed.contains(" ")
    }

    fun formatCardDisplay(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(16)
        return digits.chunked(4).joinToString(" ")
    }

    fun buildPaymentReference(methodWire: String, cardNumberOrWallet: String): String {
        val suffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
        return when (methodWire) {
            "CARD" -> {
                val digits = cardNumberOrWallet.filter { it.isDigit() }
                val last4 = digits.takeLast(4).ifEmpty { "4242" }
                val brand = detectCardBrand(digits).label
                "PAY-$brand-$last4-$suffix"
            }
            "UPI_WALLET" -> "PAY-UPI-$suffix"
            else -> "PAY-COD-$suffix"
        }
    }

    fun formatCurrency(amount: Double): String =
        String.format(Locale.US, "$%.2f", amount)
}
