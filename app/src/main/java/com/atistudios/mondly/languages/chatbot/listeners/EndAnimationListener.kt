package com.atistudios.mondly.languages.chatbot.listeners

import android.animation.Animator

abstract class EndAnimationListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator?) {}

    override fun onAnimationCancel(animation: Animator?) {}

    override fun onAnimationStart(animation: Animator?) {}
}