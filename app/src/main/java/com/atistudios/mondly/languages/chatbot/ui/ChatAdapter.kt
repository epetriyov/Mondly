package com.atistudios.mondly.languages.chatbot.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage
import com.atistudios.mondly.languages.chatbot.ui.ChatAdapter.ItemViewType.*
import com.atistudios.mondly.languages.chatbot.utilities.GlideApp
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.adt_chat_bot_message.*
import kotlinx.android.synthetic.main.adt_chat_user_message.*

internal class ChatAdapter :
    ListAdapter<ChatMessage, BaseViewHolder>(object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }) {

    enum class ItemViewType {
        BOT_MESSAGE_TYPE, USER_MESSAGE_TYPE, FOOTER_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (values()[viewType]) {
            BOT_MESSAGE_TYPE -> BotMessageViewHolder(
                layoutInflater.inflate(
                    R.layout.adt_chat_bot_message,
                    parent,
                    false
                )
            )
            USER_MESSAGE_TYPE -> UserMessageViewHolder(
                layoutInflater.inflate(
                    R.layout.adt_chat_user_message,
                    parent,
                    false
                )
            )
            FOOTER_TYPE -> FooterViewHolder(
                layoutInflater.inflate(
                    R.layout.adt_chat_footer,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatMessage.BotMessage -> BOT_MESSAGE_TYPE.ordinal
            is ChatMessage.UserMessage -> USER_MESSAGE_TYPE.ordinal
            is ChatMessage.Footer -> FOOTER_TYPE.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is BotMessageViewHolder -> holder.bindView(getItem(position) as ChatMessage.BotMessage)
            is UserMessageViewHolder -> holder.bindView(getItem(position) as ChatMessage.UserMessage)
            is FooterViewHolder -> holder.bindView(getItem(position) as ChatMessage.Footer)
        }
    }
}

sealed class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer

class BotMessageViewHolder(containerView: View) : BaseViewHolder(containerView) {
    fun bindView(item: ChatMessage.BotMessage) {
        text_message.text = item.text
        text_message_translation.text = item.translation
        img_bot_avatar.isInvisible = !item.showBotAvatar
        text_bot_messages.isVisible = !item.isLoading
        loader_bot_message.isVisible = item.isLoading
//        img_bot_avatar.setImageResource(-1)
    }
}


class UserMessageViewHolder(containerView: View) : BaseViewHolder(containerView) {
    fun bindView(item: ChatMessage.UserMessage) {
        img_message_icon.isInvisible = item.icon == null
        loader_user_message.isVisible = item.isSpeaking
        text_user_message.isVisible = !item.isSpeaking
        text_user_message.text = item.text
        if (item.icon != null) {
            GlideApp.with(containerView)
                .load(item.icon)
                .into(img_message_icon)
        } else {
            img_message_icon.setImageBitmap(null)
        }
        if (item.avatarUrl != null) {
            GlideApp.with(containerView)
                .load(item.avatarUrl)
                .into(img_user_avatar)
        } else {
            img_user_avatar.setImageBitmap(null)
        }
    }
}

class FooterViewHolder(containerView: View) : BaseViewHolder(containerView) {
    fun bindView(item: ChatMessage.Footer) {
        val lp = containerView.layoutParams
            .apply { height = item.height }
        containerView.layoutParams = lp
    }
}