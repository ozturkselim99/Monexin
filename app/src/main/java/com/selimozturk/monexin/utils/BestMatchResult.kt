package com.selimozturk.monexin.utils

enum class BestMatchResult(private val label: String) {
    DESCENDING_BY_DATE("Descending by date"), ASCENDING_BY_DATE("Ascending by date"), DECREASING_BY_AMOUNT(
        "Decreasing by amount"
    ),
    INCREASING_BY_AMOUNT("Increasing by amount"), ;

    override fun toString(): String {
        return label
    }
}