package com.atistudios.mondly.languages.chatbot.utils

import net.gotev.speech.TextToSpeechCallback

abstract class EndTextToSpeechCallback : TextToSpeechCallback {
    override fun onError() {}
    override fun onStart() {}
}