package com.atistudios.mondly.languages.chatbot.listeners

import android.transition.Transition

abstract class TransitionEndListener : Transition.TransitionListener {
    override fun onTransitionResume(transition: Transition?) {
    }

    override fun onTransitionPause(transition: Transition?) {
    }

    override fun onTransitionCancel(transition: Transition) {
        onTransitionFinished(transition)
    }

    override fun onTransitionStart(transition: Transition?) {
    }

    override fun onTransitionEnd(transition: Transition) {
        onTransitionFinished(transition)
    }

    abstract fun onTransitionFinished(transition: Transition)
}