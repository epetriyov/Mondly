package com.atistudios.mondly.languages.chatbot.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

fun View.scaleAnimation(scaleFactor: Float, duration: Long) {
    val animX = ObjectAnimator.ofFloat(this, "scaleX", scaleFactor)
    val animY = ObjectAnimator.ofFloat(this, "scaleY", scaleFactor)
    val animRX = ObjectAnimator.ofFloat(this, "scaleX", 1F)
    val animRY = ObjectAnimator.ofFloat(this, "scaleY", 1F)
    AnimatorSet().apply {
        play(animX).with(animY).before(animRX)
        play(animRX).with(animRY)
        this.duration = duration
        start()
    }
}

fun View.slideDown(duration: Long) {
    if (translationY == 0F) {
        ObjectAnimator.ofFloat(this, "translationY", 0F, height.toFloat())
            .apply {
                this.duration = duration
                start()
            }
    }
}

fun View.slideUp(duration: Long) {
    if (translationY > 0F) {
        ObjectAnimator.ofFloat(this, "translationY", height.toFloat(), 0F)
            .apply {
                this.duration = duration
                start()
            }
    }
}