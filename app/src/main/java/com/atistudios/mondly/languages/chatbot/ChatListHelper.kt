package com.atistudios.mondly.languages.chatbot

interface ChatListHelper {

    fun addItem(message: ChatMessage)

    fun updateLastItem(message: ChatMessage)

    fun setFooterHeight(footerHeight: Int)

    fun setTranslationsVisibility(areTranslationsVisible: Boolean)
}