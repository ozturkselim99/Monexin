package com.selimozturk.monexin.model

data class ExpensesInfo(
    var activeExpense: String,
    var expenses: List<Transactions>,
)
