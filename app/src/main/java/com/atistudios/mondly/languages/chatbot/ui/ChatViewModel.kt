package com.atistudios.mondly.languages.chatbot.ui

interface ChatViewModel{

    fun loadMessage()

    fun loadSuggestions()

    fun sendUserMessage(message: String)

}