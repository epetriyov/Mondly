package com.atistudios.mondly.languages.chatbot

import android.annotation.SuppressLint
import android.app.Activity
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
import com.atistudios.mondly.languages.chatbot.utils.getScreenWidth
import com.atistudios.mondly.languages.chatbot.utils.scaleAnimation
import com.bumptech.glide.Glide
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.adt_chat_bot_message.*
import kotlinx.android.synthetic.main.adt_chat_user_message.*

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

    enum class ItemViewType {
        BOT_MESSAGE_TYPE, USER_MESSAGE_TYPE, FOOTER_TYPE
    }

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
                holder.bindView(getItem(position) as ChatMessage.BotMessage)
            }
            is BaseViewHolder.UserMessageViewHolder -> {
                holder.bindView(getItem(position) as ChatMessage.UserMessage)
            }
            is BaseViewHolder.FooterViewHolder -> holder.bindView(getItem(position) as ChatMessage.Footer)
        }
    }
}

internal sealed class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {

    class BotMessageViewHolder(containerView: View, private val botMessageClickListener: (message: String) -> Unit?) :
        BaseViewHolder(containerView) {
        fun bindView(item: ChatMessage.BotMessage) {
            TransitionManager.beginDelayedTransition(container_message)
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

        init {
            // ugly hack to limit max width of user message TextView
            text_user_message.maxWidth =
                (containerView.context as Activity).getScreenWidth() -
                        containerView.context.resources.getDimension(R.dimen.adt_user_views_width).toInt()
        }

        override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
            holder.itemView.translationX = itemView.width.toFloat()
            holder.itemView.alpha = 0F
        }

        override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder?) {}

        override fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
            ViewCompat.animate(holder.itemView).apply {
                translationX(0F)
                alpha(1F)
                duration = ITEM_SLIDE_DURATION
                setListener(listener)
            }.start()
        }

        override fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {}

        fun bindView(item: ChatMessage.UserMessage) {
            img_message_icon.isVisible = item.icon != null
            if (item.icon != null) {
                Glide.with(containerView)
                    .load(item.icon)
                    .into(img_message_icon)
            } else {
                img_message_icon.setImageBitmap(null)
            }
            TransitionManager.beginDelayedTransition(user_message_container)
            loader_user_message.isVisible = item.isSpeaking
            text_user_message.isVisible = !item.isSpeaking
            text_user_message.text = item.text
            if (item.avatarUrl != null) {
                Glide.with(containerView)
                    .load(item.avatarUrl)
                    .into(img_user_avatar)
            } else {
                img_user_avatar.setImageBitmap(null)
            }
        }
    }

    class FooterViewHolder(containerView: View) : BaseViewHolder(containerView) {
        fun bindView(item: ChatMessage.Footer) {
            val lp = containerView.layoutParams.apply { height = item.height }
            containerView.layoutParams = lp
        }
    }
}