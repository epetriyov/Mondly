package com.atistudios.mondly.languages.chatbot

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

internal object SuggestionViewBinder {

    fun bindView(viewGroup: ViewGroup, suggestion: ResponseSuggestion, playbackClickListener: () -> Unit) {
        viewGroup.findViewById<ImageView>(R.id.image_message).apply {
            if (suggestion.icon != null) {
                setImageResource(suggestion.icon)
            }
        }
        viewGroup.findViewById<TextView>(R.id.text_suggestion).apply {
            text = suggestion.text
        }
        viewGroup.findViewById<TextView>(R.id.translation_suggestion).apply {
            text = suggestion.translation
        }
        viewGroup.findViewById<View>(R.id.btn_playback).apply {
            setOnClickListener {
                playbackClickListener.invoke()
            }
        }
    }
}