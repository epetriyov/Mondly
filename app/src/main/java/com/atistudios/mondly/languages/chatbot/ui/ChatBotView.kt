package com.atistudios.mondly.languages.chatbot.ui

import com.atistudios.mondly.languages.chatbot.entitites.ResponseSuggestion

interface BottomPanelView {

    fun init()

    fun botMessageLoaded(suggestion: List<ResponseSuggestion>, isFirst: Boolean)

    fun userMessageSent()

    fun controlModeClicked()

    fun optionsClicked()

    fun translationEnableChanged(enabled: Boolean)
}