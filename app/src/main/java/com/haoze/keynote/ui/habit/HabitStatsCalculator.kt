package com.haoze.keynote.ui.habit

import com.haoze.keynote.data.db.entity.HabitEntity

internal const val DayMillis = 86_400_000L

data class HabitCheckInSnapshot(
    val habitId: Long,
    val dayStartMillis: Long
)

data class HabitProgressItem(
    val habit: HabitEntity,
    val checkedToday: Boolean,
    val currentStreak: Int,
    val weeklyCompletionPercent: Int,
    val recentCheckInDays: Set<Long>
)

fun calculateCurrentStreak(
    checkInDays: Set<Long>,
    todayStart: Long
): Int {
    var cursor = todayStart
    var streak = 0

    if (!checkInDays.contains(cursor)) {
        cursor -= DayMillis
    }

    while (checkInDays.contains(cursor)) {
        streak++
        cursor -= DayMillis
    }

    return streak
}

fun buildHabitProgressItems(
    habits: List<HabitEntity>,
    checkIns: List<HabitCheckInSnapshot>,
    todayStart: Long,
    windowDays: Int = 7
): List<HabitProgressItem> {
    val checkInsByHabit = checkIns.groupBy { it.habitId }

    return habits.map { habit ->
        val habitCheckInDays = checkInsByHabit[habit.id]
            .orEmpty()
            .map { it.dayStartMillis }
            .toSet()
        val recentWindow = (0 until windowDays).map { todayStart - it * DayMillis }.toSet()
        val recentCheckInDays = habitCheckInDays.intersect(recentWindow)
        val targetDays = habit.targetDaysPerWeek.coerceIn(1, windowDays.coerceAtLeast(1))
        val completionPercent = if (windowDays <= 0) {
            0
        } else {
            ((recentCheckInDays.size * 100) / targetDays).coerceAtMost(100)
        }

        HabitProgressItem(
            habit = habit,
            checkedToday = habitCheckInDays.contains(todayStart),
            currentStreak = calculateCurrentStreak(habitCheckInDays, todayStart),
            weeklyCompletionPercent = completionPercent,
            recentCheckInDays = recentCheckInDays
        )
    }.sortedWith(
        compareBy<HabitProgressItem> { it.checkedToday }
            .thenByDescending { it.habit.createdAt }
    )
}
