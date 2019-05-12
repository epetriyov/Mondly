package com.atistudios.mondly.languages.chatbot

internal interface ChatListHelper {

    fun addItem(message: ChatMessage)

    fun updateLastItem(message: ChatMessage)

    fun setFooterHeight(footerHeight: Int)

    fun setTranslationsVisibility(areTranslationsVisible: Boolean)

    fun setListUpdatedListener(listUpdatedCallback: (List<ChatMessage>) -> Unit)
}

internal class ChatListHelperImpl : ChatListHelper {

    private var chatList = mutableListOf<ChatMessage>()

    private var listUpdatedCallback: ((List<ChatMessage>) -> Unit)? = null

    override fun setListUpdatedListener(listUpdatedCallback: (List<ChatMessage>) -> Unit) {
        this.listUpdatedCallback = listUpdatedCallback
    }

    override fun addItem(message: ChatMessage) {
        if (chatList.size > 0 && chatList[chatList.size - 1] is ChatMessage.Footer) {
            chatList.add(chatList.size - 1, message)
        } else {
            chatList.add(message)
        }
        notifyListener()
    }

    override fun updateLastItem(message: ChatMessage) {
        if (chatList.size == 0) {
            throw IllegalStateException("List is empty")
        }
        if (chatList.size == 1 && chatList[chatList.size - 1] is ChatMessage.Footer) {
            throw IllegalStateException("Last message is not updatable ${chatList[chatList.size - 1]}")
        }
        if (chatList.size > 1 && chatList[chatList.size - 1] is ChatMessage.Footer) {
            chatList[chatList.size - 2] = message
        } else {
            chatList[chatList.size - 1] = message
        }
        notifyListener()
    }

    override fun setFooterHeight(footerHeight: Int) {
        val footerItem = ChatMessage.Footer(footerHeight)
        when {
            chatList.isEmpty() -> chatList.add(footerItem)
            chatList[chatList.size - 1] is ChatMessage.Footer -> chatList[chatList.size - 1] = footerItem
            else -> chatList.add(footerItem)
        }
        notifyListener()
    }

    override fun setTranslationsVisibility(areTranslationsVisible: Boolean) {
        chatList = chatList.map {
            if (it is ChatMessage.BotMessage) it.copy(showTranslation = areTranslationsVisible) else it
        }
            .toMutableList()
        notifyListener()
    }

    private fun notifyListener() {
        listUpdatedCallback?.invoke(chatList.toMutableList())
    }

}