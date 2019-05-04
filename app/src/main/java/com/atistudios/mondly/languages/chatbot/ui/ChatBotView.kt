package com.atistudios.mondly.languages.chatbot.ui

import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage

interface ChatBotView {

    fun showMessage(chatMessage: ChatMessage)
}