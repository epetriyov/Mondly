package com.atistudios.mondly.languages.chatbot.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import androidx.transition.TransitionManager
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage
import com.atistudios.mondly.languages.chatbot.ui.ChatAdapter.ItemViewType.*
import com.atistudios.mondly.languages.chatbot.utilities.GlideApp
import jp.wasabeef.recyclerview.animators.holder.AnimateViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.adt_chat_bot_message.*
import kotlinx.android.synthetic.main.adt_chat_user_message.*


internal class ChatAdapter :
    ListAdapter<ChatMessage, BaseViewHolder>(object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }) {

    private var lastPosition = -1

    private var areTranslationsVisible: Boolean = true

    enum class ItemViewType {
        BOT_MESSAGE_TYPE, USER_MESSAGE_TYPE, FOOTER_TYPE
    }

    fun setTranslationVisibility(isTranslationsVisible: Boolean) {
        this.areTranslationsVisible = isTranslationsVisible
        notifyDataSetChanged()
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
            is BotMessageViewHolder -> {
                val item = getItem(position) as ChatMessage.BotMessage
                val animateText = if (!item.isLoading) {
                    if (position > lastPosition) {
                        lastPosition = position
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
                holder.bindView(
                    item,
                    areTranslationsVisible,
                    animateText
                )
            }
            is UserMessageViewHolder -> {
                val item = getItem(position) as ChatMessage.UserMessage
                val animateText = if (!item.isSpeaking) {
                    if (position > lastPosition) {
                        lastPosition = position
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
                holder.bindView(item, animateText)
            }
            is FooterViewHolder -> holder.bindView(getItem(position) as ChatMessage.Footer)
        }
    }
}

sealed class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer

class BotMessageViewHolder(containerView: View) : BaseViewHolder(containerView) {
    fun bindView(item: ChatMessage.BotMessage, isTranslationsVisible: Boolean, showAnimated: Boolean) {
        TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        text_message.text = item.text
        text_message_translation.text = item.translation
        img_bot_avatar.isInvisible = !item.showBotAvatar
        text_bot_messages.isVisible = !item.isLoading
        text_message_translation.isVisible = !item.isLoading && isTranslationsVisible
        loader_bot_message.isVisible = item.isLoading
        text_bot_messages.setOnClickListener {
            val animX = ObjectAnimator.ofFloat(text_message, "scaleX", 2f)
            val animY = ObjectAnimator.ofFloat(text_message, "scaleY", 2f)
            val animRX = ObjectAnimator.ofFloat(text_message, "scaleX", 1f)
            val animRY = ObjectAnimator.ofFloat(text_message, "scaleY", 1f)
            AnimatorSet().apply {
                play(animX).with(animY).before(animRX)
                play(animRX).with(animRY)
                duration = 240
                start()
            }
        }
        if (showAnimated) {
            val animX = ObjectAnimator.ofFloat(text_message, "alpha", 0F, 1F)
            val animY = ObjectAnimator.ofFloat(text_message_translation, "alpha", 0F, 1F)
            AnimatorSet().apply {
                play(animX).with(animY)
                duration = 240
                start()
            }
        }
//        img_bot_avatar.setImageResource(-1)
    }
}


class UserMessageViewHolder(containerView: View) : BaseViewHolder(containerView), AnimateViewHolder {
    override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
        ViewCompat.setTranslationX(itemView, itemView.width * 1f)
        ViewCompat.setAlpha(itemView, 0f)
    }

    override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder?) {
    }

    override fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
        ViewCompat.animate(itemView).apply {
            translationX(0f)
            alpha(1f)
            duration = 120
            setListener(listener)
        }.start()
    }


    override fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: ViewPropertyAnimatorListener?) {
    }

    fun bindView(item: ChatMessage.UserMessage, animateText: Boolean) {
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
        if (animateText) {
            ObjectAnimator.ofFloat(text_user_message, "alpha", 0F, 1F)
                .apply {
                    duration = 240
                    start()
                }
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