package com.selimozturk.monexin.model
import java.io.Serializable

data class FilterModel(
    var bestMatchResult: String = "",
    var minAmount: String = "",
    var maxAmount: String = "",
    var minDate: String = "",
    var maxDate: String = "",
):Serializable
