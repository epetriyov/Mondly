package com.atistudios.mondly.languages.chatbot

import android.annotation.SuppressLint
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atistudios.mondly.languages.chatbot.utils.scaleAnimation
import com.bumptech.glide.Glide
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.adt_chat_bot_message.*
import kotlinx.android.synthetic.main.adt_chat_user_message.*

enum class ItemViewType {
    BOT_MESSAGE_TYPE, USER_MESSAGE_TYPE, FOOTER_TYPE
}

private const val ITEM_SLIDE_DURATION = 250L
private const val TEXT_SCALE_DURATION = 250L
private const val TEXT_SCALE_FACTOR = 1.5F

internal class ChatAdapter(private val botMessageClickListener: ((message: String) -> Unit)?) :
    ListAdapter<ChatMessage, BaseViewHolder>(
        object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }) {

    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (ItemViewType.values()[viewType]) {
            ItemViewType.BOT_MESSAGE_TYPE -> BaseViewHolder.BotMessageViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_bot_message, parent, false)
            ) { botMessageClickListener?.invoke(it) }
            ItemViewType.USER_MESSAGE_TYPE -> BaseViewHolder.UserMessageViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_user_message, parent, false)
            )
            ItemViewType.FOOTER_TYPE -> BaseViewHolder.FooterViewHolder(
                layoutInflater.inflate(R.layout.adt_chat_footer, parent, false)
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatMessage.BotMessage -> ItemViewType.BOT_MESSAGE_TYPE.ordinal
            is ChatMessage.UserMessage -> ItemViewType.USER_MESSAGE_TYPE.ordinal
            is ChatMessage.Footer -> ItemViewType.FOOTER_TYPE.ordinal
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is BaseViewHolder.BotMessageViewHolder -> {
                val item = getItem(position) as ChatMessage.BotMessage
                val animateText = !item.isLoading && position > lastPosition
                if (animateText) {
                    lastPosition = position
                }
                holder.bindView(item, animateText)
            }
            is BaseViewHolder.UserMessageViewHolder -> {
                val item = getItem(position) as ChatMessage.UserMessage
                val animateText = !item.isSpeaking && position > lastPosition
                if (animateText) {
                    lastPosition = position
                }
                holder.bindView(item, animateText)
            }
            is BaseViewHolder.FooterViewHolder -> holder.bindView(getItem(position) as ChatMessage.Footer)
        }
    }
}

sealed class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {

    class BotMessageViewHolder(containerView: View, private val botMessageClickListener: (message: String) -> Unit?) :
        BaseViewHolder(containerView) {
        fun bindView(item: ChatMessage.BotMessage, showAnimated: Boolean) {
            if (showAnimated) {
                text_message.isVisible = false
                text_message_translation.isVisible = false
            }
            TransitionManager.beginDelayedTransition(itemView as ViewGroup)
            text_message.text = item.text
            text_message_translation.text = item.translation
            img_bot_avatar.isInvisible = !item.showBotAvatar
            loader_bot_message.isVisible = item.isLoading
            text_message.isVisible = !item.isLoading
            text_message_translation.isVisible = !item.isLoading && item.showTranslation
            text_bot_messages.setOnClickListener {
                if (!item.text.isNullOrEmpty()) {
                    text_bot_messages.scaleAnimation(TEXT_SCALE_FACTOR, TEXT_SCALE_DURATION)
                    botMessageClickListener.invoke(item.text)
                }
            }
        }
    }


    class UserMessageViewHolder(containerView: View) : BaseViewHolder(containerView), AnimateViewHolder {
        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            itemView.translationX = itemView.width.toFloat()
            itemView.alpha = 0F
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder?) {}

        override fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
            ViewCompat.animate(itemView).apply {
                translationX(0F)
                alpha(1F)
                duration = ITEM_SLIDE_DURATION
                setListener(listener)
            }.start()
        }

        override fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {}

        fun bindView(item: ChatMessage.UserMessage, animateText: Boolean) {
            img_message_icon.isInvisible = item.icon == null
            loader_user_message.isVisible = item.isSpeaking
            text_user_message.isVisible = !item.isSpeaking
            text_user_message.text = item.text
            if (item.icon != null) {
                Glide.with(containerView)
                    .load(item.icon)
                    .into(img_message_icon)
            } else {
                img_message_icon.setImageBitmap(null)
            }
            if (item.avatarUrl != null) {
                Glide.with(containerView)
                    .load(item.avatarUrl)
                    .into(img_user_avatar)
            } else {
                img_user_avatar.setImageBitmap(null)
            }
            if (animateText) {
                text_user_message.isVisible = false
            }
            TransitionManager.beginDelayedTransition(itemView as ViewGroup)
            text_user_message.isVisible = true
        }
    }

    class FooterViewHolder(containerView: View) : BaseViewHolder(containerView) {
        fun bindView(item: ChatMessage.Footer) {
            val lp = containerView.layoutParams.apply { height = item.height }
            containerView.layoutParams = lp
        }
    }
}