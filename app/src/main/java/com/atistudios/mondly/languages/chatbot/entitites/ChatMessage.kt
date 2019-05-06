package com.atistudios.mondly.languages.chatbot.entitites

import androidx.annotation.DrawableRes

sealed class ChatMessage(open val id: String) {

    data class BotMessage(
        override val id: String,
        val text: String?,
        val translation: String?,
        val isLoading: Boolean,
        val showBotAvatar: Boolean = true
    ) : ChatMessage(id)

    data class UserMessage(
        override val id: String,
        val text: String? = null,
        @DrawableRes val icon: Int? = null,
        val avatarUrl: String? = null,
        val isSpeaking: Boolean = false
    ) : ChatMessage(id)

    data class Footer(val height: Int) : ChatMessage("-1")
}