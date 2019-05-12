package com.atistudios.mondly.languages.chatbot

import android.os.Handler

internal interface ChatEngine {

    fun onChatOpened()

    fun onUserSpeakStarted()

    fun onUserAnswered(message: String?, isTyped: Boolean)

    fun onTranslationsVisibilityChanged(areTranslationsVisible: Boolean)

    fun onAutoPlayModeChanged(isAutoPlayEnabled: Boolean)

    fun onFooterHeightChanged(footerHeight: Int)
}

internal class ChatEngineImpl(
    private val chatView: ChatView,
    private val chatListHelper: ChatListHelper,
    private val handler: Handler
) : ChatEngine {

    init {
        chatListHelper.setListUpdatedListener {
            chatView.chatUpdated(it)
        }
    }

    private var messageCounter = 0

    private var isAutoPlayEnabled = true

    override fun onChatOpened() {
        loadBotMessage(true)
    }

    override fun onUserAnswered(message: String?, isTyped: Boolean) {
        val userMessage = buildTestUserMessage(message)
        if (isTyped) {
            chatListHelper.addItem(userMessage)
        } else {
            chatListHelper.updateLastItem(userMessage)
        }
        chatView.botMessageLoading()
        loadBotMessage(false)
    }

    override fun onUserSpeakStarted() {
        chatListHelper.addItem(buildLoadingTestUserMessage())
    }

    override fun onTranslationsVisibilityChanged(areTranslationsVisible: Boolean) {
        chatListHelper.setTranslationsVisibility(areTranslationsVisible)
    }

    override fun onAutoPlayModeChanged(isAutoPlayEnabled: Boolean) {
        this.isAutoPlayEnabled = isAutoPlayEnabled
    }

    override fun onFooterHeightChanged(footerHeight: Int) {
        chatListHelper.setFooterHeight(footerHeight)
    }


    private fun loadBotMessage(introAnimations: Boolean) {
        chatView.progressStateChanged(true)
        val botMessage = buildTestBotMessage()
        handler.postDelayed({
            chatView.progressStateChanged(false)
            chatListHelper.addItem(botMessage)
        }, 2000L)
        handler.postDelayed({
            chatListHelper.updateLastItem(botMessage.copy(isLoading = false))
            if (isAutoPlayEnabled) {
                botMessage.text?.let {
                    chatView.speak(it)
                }
            }
        }, 3000L)
        handler.postDelayed({
            chatView.suggestionsLoaded(buildTestSuggestions(), introAnimations)
        }, 4000L)
    }

    private fun buildTestUserMessage(message: String?): ChatMessage.UserMessage {
        return ChatMessage.UserMessage(messageCounter.toString(), message)
    }

    private fun buildLoadingTestUserMessage(): ChatMessage.UserMessage {
        messageCounter++
        return ChatMessage.UserMessage(messageCounter.toString(), isSpeaking = true)
    }

    private fun buildTestBotMessage(): ChatMessage.BotMessage {
        messageCounter++
        return ChatMessage.BotMessage(messageCounter.toString(), "test", "test", true)
    }

    private fun buildTestSuggestions(): Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion> {
        return Triple(
            ResponseSuggestion("test", "test"),
            ResponseSuggestion("test2", "test2"),
            ResponseSuggestion("test3", "test3")
        )
    }

}