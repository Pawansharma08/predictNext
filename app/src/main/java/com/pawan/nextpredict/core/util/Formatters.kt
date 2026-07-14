package com.pawan.nextpredict.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * Centralized number and string formatting utilities for financial data.
 */
object Formatters {

    private val indianLocale = Locale("en", "IN")
    private val decimalFormat = DecimalFormat("#,##,##0.00")
    private val compactFormat = DecimalFormat("0.##")

    /**
     * Formats a price with 2 decimal places in Indian number system.
     * e.g. 25400.55 -> "₹25,400.55"
     */
    fun formatPrice(price: Double): String {
        return "₹${decimalFormat.format(price)}"
    }

    /**
     * Formats price without rupee symbol.
     */
    fun formatPriceRaw(price: Double): String {
        return decimalFormat.format(price)
    }

    /**
     * Formats a change value with sign: +100.50 or -50.25
     */
    fun formatChange(change: Double): String {
        val formatted = decimalFormat.format(abs(change))
        return if (change >= 0) "+$formatted" else "-$formatted"
    }

    /**
     * Formats a percentage change: +1.25% or -0.88%
     */
    fun formatChangePercent(changePercent: Double): String {
        val formatted = String.format(Locale.US, "%.2f", abs(changePercent))
        return if (changePercent >= 0) "+$formatted%" else "-$formatted%"
    }

    /**
     * Formats volume in compact form: 1.2M, 450K, etc.
     */
    fun formatVolume(volume: Long): String {
        return when {
            volume >= 1_000_000_000 -> "${compactFormat.format(volume / 1_000_000_000.0)}B"
            volume >= 1_000_000 -> "${compactFormat.format(volume / 1_000_000.0)}M"
            volume >= 1_000 -> "${compactFormat.format(volume / 1_000.0)}K"
            else -> volume.toString()
        }
    }

    /**
     * Formats market cap in crores: ₹1,25,430 Cr
     */
    fun formatMarketCap(marketCapCr: Double): String {
        return when {
            marketCapCr >= 1_00_000 -> "${compactFormat.format(marketCapCr / 1_00_000.0)} L Cr"
            else -> "₹${decimalFormat.format(marketCapCr)} Cr"
        }
    }

    /**
     * Returns true if the value represents a gain (positive).
     */
    fun isGain(value: Double): Boolean = value >= 0.0
}

/**
 * Extension functions for common formatting needs.
 */
fun Double.toPriceString(): String = Formatters.formatPrice(this)
fun Double.toChangeString(): String = Formatters.formatChange(this)
fun Double.toChangePercentString(): String = Formatters.formatChangePercent(this)
fun Double.isGain(): Boolean = this >= 0.0
fun Long.toVolumeString(): String = Formatters.formatVolume(this)
