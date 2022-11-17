package com.selimozturk.monexin.model

data class HomeInfo(
    var displayName: String,
    var activeExpense: String,
    var activeIncome: String,
    var recentlyAdded: List<Transactions>,
)