package com.atistudios.mondly.languages.chatbot.entitites

import androidx.annotation.DrawableRes

sealed class ChatMessage {

    data class BotMessage(
        val text: String?,
        val translation: String?,
        val isLoading: Boolean,
        val showBotAvatar: Boolean = true
    ) : ChatMessage()

    data class UserMessage(
        val text: String? = null,
        @DrawableRes val icon: Int? = null,
        val avatarUrl: String? = null,
        val isSpeaking: Boolean = false
    ) : ChatMessage()

    data class Footer(val height: Int) : ChatMessage()
}