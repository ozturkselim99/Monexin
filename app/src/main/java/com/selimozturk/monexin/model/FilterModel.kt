package com.selimozturk.monexin.model
import com.selimozturk.monexin.utils.BestMatchResult
import java.io.Serializable

data class FilterModel(
    var bestMatchResult: BestMatchResult,
    var minAmount: String = "",
    var maxAmount: String = "",
    var minDate: String = "",
    var maxDate: String = "",
):Serializable
