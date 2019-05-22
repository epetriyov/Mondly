package com.atistudios.mondly.languages.chatbot.ext

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View


fun View.scaleAnimation(scaleFactor: Float, duration: Long, twice: Boolean = false) {
    val scaleUpX = ObjectAnimator.ofFloat(this, "scaleX", scaleFactor)
    val scaleUpY = ObjectAnimator.ofFloat(this, "scaleY", scaleFactor)
    val scaleDownX = ObjectAnimator.ofFloat(this, "scaleX", 1F)
    val scaleDownY = ObjectAnimator.ofFloat(this, "scaleY", 1F)
    AnimatorSet()
        .apply {
            play(scaleUpX).with(scaleUpY).before(scaleDownX)
            play(scaleDownX).with(scaleDownY)
            this.duration = duration
            start()
            if (twice) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        removeAllListeners()
                        start()
                    }
                })
            }
        }
}

fun View.slideDown(duration: Long, translation: Float = height.toFloat()) {
        animate().translationY(translation).duration = duration
}

fun View.slideUp(translation: Float,duration: Long) {
        animate().translationY(translation).duration = duration
}