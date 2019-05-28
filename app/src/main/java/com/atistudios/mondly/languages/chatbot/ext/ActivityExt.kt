package com.atistudios.mondly.languages.chatbot.ext

import android.app.Activity
import android.graphics.Point
import android.view.View
import android.view.inputmethod.InputMethodManager


fun Activity.hideKeyboard(view: View) {
    (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        view.windowToken,
        0
    )
}

fun Activity.getScreenWidth(): Int {
    val point = Point()
    windowManager.defaultDisplay.getSize(point)
    return point.x
}

fun View.getLocationOnScreen(): Point {
    val location = IntArray(2)
    this.getLocationOnScreen(location)
    return Point(location[0], location[1])
}