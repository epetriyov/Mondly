package com.atistudios.mondly.languages.chatbot.utils

import android.view.View

fun View.scaleAnimation(scaleFactor: Float, duration: Long) {
    animate().scaleX(scaleFactor).scaleY(scaleFactor).setDuration(duration).withEndAction {
        animate().scaleX(1F).scaleY(1F).duration = duration
    }
}

fun View.slideDown(duration: Long) {
    if (translationY == 0F) {
        animate().translationY(height.toFloat()).duration = duration
    }
}

fun View.slideUp(duration: Long) {
    if (translationY > 0F) {
        animate().translationY(0F).duration = duration
    }
}