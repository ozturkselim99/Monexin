package com.selimozturk.monexin.model
import java.io.Serializable

data class Transactions(
    var id:String="",
    var title: String="",
    var description:String="",
    var createdAt: String="",
    var type:String="",
    var amount: Double=0.0,
    var photoPath: String="",
):Serializable