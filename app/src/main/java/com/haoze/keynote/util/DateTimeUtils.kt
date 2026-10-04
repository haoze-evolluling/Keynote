package com.haoze.keynote.util

import java.util.Calendar

fun Calendar.normalizeToDayStart(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

fun Long.toDayStartMillis(): Long {
    return Calendar.getInstance().apply {
        timeInMillis = this@toDayStartMillis
        normalizeToDayStart()
    }.timeInMillis
}