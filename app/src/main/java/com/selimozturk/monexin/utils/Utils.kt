package com.selimozturk.monexin.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun TextInputEditText.customIsEmpty():Boolean{
    val textInputLayout=this.parent.parent as TextInputLayout
    val input=this.text.toString()
    if(input.isBlank()){
        textInputLayout.helperText="Required field."
        return false
    }
    return true
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}

fun String.convertToLongTime():String{
        val date = Date(this.toLong())
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
        return format.format(date).toString()
}

fun String.convertToTimestamp():Long{
    val formatter: DateFormat = SimpleDateFormat("dd-MM-yyyy")
    val date = formatter.parse(this) as Date
    return (date.time)
}


