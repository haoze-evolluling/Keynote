package com.haoze.keynote.data.db.entity

import androidx.room.ColumnInfo

data class BillWithCategoryRaw(
    @ColumnInfo(name = "bill_id") val billId: Long,
    @ColumnInfo(name = "bill_item") val billItem: String,
    @ColumnInfo(name = "bill_amount") val billAmount: Double,
    @ColumnInfo(name = "bill_date") val billDate: Long,
    @ColumnInfo(name = "cat_id") val catId: Long?,
    @ColumnInfo(name = "cat_name") val catName: String?
)

data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String?,
    val total: Double
)

data class MonthlySpending(
    val month: String,
    val total: Double
)

data class DailySpending(
    val day: String,
    val total: Double
)

data class WeeklySpending(
    val week: String,
    val total: Double
)
