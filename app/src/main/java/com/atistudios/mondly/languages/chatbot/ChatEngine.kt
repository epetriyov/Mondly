package com.atistudios.mondly.languages.chatbot

import java.util.*

internal interface ChatEngine {

    fun onChatOpened()

    fun onUserAnswered(message: String)
}

internal class ChatEngineImpl(
    private val chatView: ChatView, private val localeToLearn: Locale,
    private val chatListHelper: ChatListHelper
) :
    ChatEngine {
    override fun onChatOpened() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUserAnswered(message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}