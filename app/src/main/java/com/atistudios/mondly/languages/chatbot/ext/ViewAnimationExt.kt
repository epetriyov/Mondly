package com.atistudios.mondly.languages.chatbot.ext

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.widget.ImageView


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

fun View.slideUp(translation: Float, duration: Long) {
    animate().translationY(translation).duration = duration
}

fun ImageView.play(callback: (rate: Float) -> Unit?) {
    (drawable as AnimationDrawable).apply {
        start()
        setImageDrawable(reverseAnimation())
    }
    if (tag != null) {
        tag = null
        callback.invoke(0.5F)
    } else {
        tag = "PLAYBACK_WAS_ALREADY_CLICKED"
        callback.invoke(1F)
    }
}

private fun AnimationDrawable.reverseAnimation(): AnimationDrawable {
    val newAnim = AnimationDrawable()
    val numberOfFrame = numberOfFrames
    for (i in 0 until numberOfFrame) {
        newAnim.addFrame(
            getFrame(numberOfFrame - i - 1),
            getDuration(numberOfFrame - i - 1)
        )
    }
    newAnim.isOneShot = true
    return newAnim
}