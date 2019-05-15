package com.atistudios.mondly.languages.chatbot.listeners

import net.gotev.speech.TextToSpeechCallback

abstract class EndTextToSpeechCallback : TextToSpeechCallback {
    override fun onError() {}
    override fun onStart() {}
}