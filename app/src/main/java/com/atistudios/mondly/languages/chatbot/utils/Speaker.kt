package com.atistudios.mondly.languages.chatbot.utils

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import java.util.*

internal class Speaker(context: Context, private val language: Locale) : OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)

    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = language
            ready = true
        } else {
            ready = false
        }
    }

    fun speak(text: String) {
        // Speak only if the TTS is ready
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
            }, null)
        }
    }

    fun pause(duration: Int) {
        tts.playSilentUtterance(duration.toLong(), TextToSpeech.QUEUE_ADD, null)
    }

    // Free up resources
    fun destroy() {
        tts.shutdown()
    }

}