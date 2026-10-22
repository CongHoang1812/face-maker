package com.conght.facemakerapp.core.common

object StarRating {
    /** ≥90% = 3 sao, ≥60% = 2, ≥30% = 1, còn lại 0 (01_ANALYSIS.md mục 5). */
    fun fromPercent(percent: Int): Int = when {
        percent >= 90 -> 3
        percent >= 60 -> 2
        percent >= 30 -> 1
        else -> 0
    }
}
