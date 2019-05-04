package com.atistudios.mondly.languages.chatbot.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ResponseSuggestion

object SuggestionViewBinder {

    fun bindView(viewGroup: ViewGroup, suggestion: ResponseSuggestion, playbackClickListener: () -> Unit) {
        viewGroup.findViewById<ImageView>(R.id.image_message).apply {
            if (suggestion.icon != null) {
                setImageResource(suggestion.icon)
            }
        }
        viewGroup.findViewById<TextView>(R.id.text_message).apply {
            text = suggestion.text
        }
        viewGroup.findViewById<TextView>(R.id.text_translation).apply {
            text = suggestion.translation
        }
        viewGroup.findViewById<View>(R.id.btn_playback).apply {
            setOnClickListener {
                playbackClickListener.invoke()
            }
        }
    }
}