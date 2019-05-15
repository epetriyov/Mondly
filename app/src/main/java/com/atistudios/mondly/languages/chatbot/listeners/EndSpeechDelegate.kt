package com.atistudios.mondly.languages.chatbot.listeners

import net.gotev.speech.SpeechDelegate

abstract class EndSpeechDelegate: SpeechDelegate {
    override fun onStartOfSpeech() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSpeechPartialResults(results: MutableList<String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSpeechRmsChanged(value: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}