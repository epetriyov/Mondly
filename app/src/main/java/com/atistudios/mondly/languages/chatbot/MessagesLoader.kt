package com.atistudios.mondly.languages.chatbot

interface MessagesLoader {
    fun buildTestUserMessage(message: String?, isTyped: Boolean): ChatMessage.UserMessage
    fun buildLoadingTestUserMessage(): ChatMessage.UserMessage
    fun buildTestBotMessage(): ChatMessage.BotMessage
    fun buildTestSuggestions(): Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion>
}

class MockMessagesLoader : MessagesLoader {

    private var messageCounter = 0

    override fun buildTestUserMessage(message: String?, isTyped: Boolean): ChatMessage.UserMessage {
        if (isTyped) {
            messageCounter++
        }
        return ChatMessage.UserMessage(messageCounter.toString(), message, icon = R.drawable.ic_emoji)
    }

    override fun buildLoadingTestUserMessage(): ChatMessage.UserMessage {
        messageCounter++
        return ChatMessage.UserMessage(
            messageCounter.toString(),
            isSpeaking = true
        )
    }

    override fun buildTestBotMessage(): ChatMessage.BotMessage {
        messageCounter++
        return ChatMessage.BotMessage(messageCounter.toString(), "test", "test", true)
    }

    override fun buildTestSuggestions(): Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion> {
        return Triple(
            ResponseSuggestion("test", "test", R.drawable.ic_emoji),
            ResponseSuggestion("test2", "test2", R.drawable.ic_emoji),
            ResponseSuggestion("test3", "test3", R.drawable.ic_emoji)
        )
    }
}