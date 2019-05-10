package com.atistudios.mondly.languages.chatbot.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage
import com.atistudios.mondly.languages.chatbot.entitites.ResponseSuggestion
import com.atistudios.mondly.languages.chatbot.utilities.Speaker
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_chatbot.*
import java.util.*


class ChatBotActivity : AppCompatActivity(), BottomPanelView {
    override fun translationEnableChanged(enabled: Boolean) {
        chatAdapter.setTranslationVisibility(enabled)
        TransitionManager.beginDelayedTransition(first_suggestion as ViewGroup)
        first_suggestion.findViewById<View>(R.id.text_translation).isVisible = enabled
        TransitionManager.beginDelayedTransition(second_suggestion as ViewGroup)
        second_suggestion.findViewById<View>(R.id.text_translation).isVisible = enabled
        TransitionManager.beginDelayedTransition(third_suggestion as ViewGroup)
        third_suggestion.findViewById<View>(R.id.text_translation).isVisible = enabled
    }

    override fun init() {
        label_suggestions.visibility = View.GONE
        first_suggestion.isInvisible = true
        second_suggestion.isInvisible = true
        third_suggestion.isInvisible = true
        btn_more_options.alpha = 0.5F
        btn_change_input_type.alpha = 0.5F
        btn_microphone.alpha = 0.5F
        container_edit_text.visibility = View.GONE
        edit_answer.visibility = View.GONE
        btn_send.visibility = View.GONE
        switch_auto_play.visibility = View.GONE
        divider_switch.visibility = View.GONE
        switch_translations.visibility = View.GONE
    }

    override fun botMessageLoaded(suggestion: List<ResponseSuggestion>, isFirst: Boolean) {
        TransitionManager.go(Scene(bottom_container), AutoTransition().apply {
            addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    val animX = ObjectAnimator.ofFloat(btn_microphone, "scaleX", 1.5f)
                    val animY = ObjectAnimator.ofFloat(btn_microphone, "scaleY", 1.5f)
                    val animRX = ObjectAnimator.ofFloat(btn_microphone, "scaleX", 1f)
                    val animRY = ObjectAnimator.ofFloat(btn_microphone, "scaleY", 1f)
                    AnimatorSet().apply {
                        play(animX).with(animY).before(animRX)
                        play(animRX).with(animRY)
                        duration = 240
                        start()
                    }

                    fillAdapterWithMockData()
                }

                override fun onTransitionResume(transition: Transition) {
                }

                override fun onTransitionPause(transition: Transition) {
                }

                override fun onTransitionCancel(transition: Transition) {
                }

                override fun onTransitionStart(transition: Transition) {
                }
            })
        })
        if (isFirst) {
            label_suggestions.visibility = View.VISIBLE
        }
        btn_more_options.alpha = 1F
        btn_change_input_type.alpha = 1F
        btn_microphone.alpha = 1F
        btn_send.alpha = 1F
    }

    override fun userMessageSent() {
        TransitionManager.beginDelayedTransition(bottom_container)
        btn_more_options.alpha = 0.5F
        btn_change_input_type.alpha = 0.5F
        btn_microphone.alpha = 0.5F
        btn_send.alpha = 0.5F
        first_suggestion.isInvisible = true
        second_suggestion.isInvisible = true
        third_suggestion.isInvisible = true
    }

    override fun controlModeClicked() {
        TransitionManager.beginDelayedTransition(bottom_container)
        btn_change_input_type.setImageResource(if (btn_microphone.isVisible) R.drawable.ic_microphone else R.drawable.ic_keyboard)
        btn_microphone.isVisible = !btn_microphone.isVisible
        edit_text_group.isVisible = !edit_text_group.isVisible
    }

    override fun optionsClicked() {
        TransitionManager.beginDelayedTransition(bottom_container)
        options_group.isVisible = !options_group.isVisible
    }

    companion object {

        private val EXTRA_CHATBOT_LANGUAGE = "extra_chatbot_language"
        private val EXTRA_CHATBOT_THEME = "extra_chatbot_theme"
        private val TTS_RESULT_CODE = 89

        fun buildIntent(context: Context, language: Locale, theme: String): Intent {
            return Intent(context, ChatBotActivity::class.java).apply {
                putExtra(EXTRA_CHATBOT_LANGUAGE, language)
                putExtra(EXTRA_CHATBOT_THEME, theme)
            }
        }
    }

    private lateinit var chatAdapter: ChatAdapter

    private var speaker: Speaker? = null

    private lateinit var chatViewModel: ChatViewModel

    private lateinit var chatLanguage: Locale

    private var chatTheme: String? = null

    private var isRecyclerInTop = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this)
        recycler_view_chat_bot.layoutManager = layoutManager
        recycler_view_chat_bot.itemAnimator = SlideInLeftAnimator()
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
            .apply { setDrawable(getDrawable(R.drawable.divider)) }
        recycler_view_chat_bot.addItemDecoration(dividerItemDecoration)
        recycler_view_chat_bot.adapter = chatAdapter
        readIntent()
        checkTTS()
        init()
        val mockData =
            mutableListOf<ChatMessage>(ChatMessage.Footer(resources.getDimension(R.dimen.footer_height).toInt()))
        var counter = 0
        btn_microphone.setOnClickListener {
            mockData.add(mockData.size - 1, getElement(counter))
            chatAdapter.submitList(mockData.toMutableList())
            counter++
        }
        bottom_container.setOnClickListener {
            val list = mutableListOf(
                ChatMessage.BotMessage("1", "Hola!", "привет", false),
                ChatMessage.UserMessage("2", "Hola U+1F600", isSpeaking = false),
                ChatMessage.Footer(resources.getDimension(R.dimen.footer_height).toInt())
            )
            chatAdapter.submitList(list)
        }
        var closeCounter = 0
        btn_close.setOnClickListener {
            if (closeCounter % 2 == 0) {
                botMessageLoaded(emptyList(), true)
            } else {
                userMessageSent()
            }
            closeCounter++
        }
        btn_more_options.setOnClickListener {
            optionsClicked()
        }
        btn_change_input_type.setOnClickListener {
            controlModeClicked()
        }
        switch_translations.setOnCheckedChangeListener { buttonView, isChecked -> translationEnableChanged(isChecked) }
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recycler_view_chat_bot.layoutManager!!.smoothScrollToPosition(
                    recycler_view_chat_bot,
                    null,
                    chatAdapter.itemCount
                )
            }
        })
        recycler_view_chat_bot.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (recycler_view_chat_bot.canScrollVertically(1)) {
                    if (layoutManager.findFirstVisibleItemPosition() == 0
                        && !recycler_view_chat_bot.canScrollVertically(-1)
                    ) {
                        toolbar.elevation = 0f
                        toolbar.setBackgroundColor(
                            ContextCompat.getColor(
                                this@ChatBotActivity,
                                android.R.color.transparent
                            )
                        )
                        hideBottomPanelAnimation()
                        isRecyclerInTop = true
                    } else {
                        toolbar.elevation = resources.getDimension(R.dimen.chatbot_control_panel_elevation)
                        toolbar.setBackgroundColor(ContextCompat.getColor(this@ChatBotActivity, R.color.dusk_blue))
                        showBottomPanelAnimation()
                        isRecyclerInTop = false
                    }
                }
            }
        })
    }

    private fun hideBottomPanelAnimation() {
        if (bottom_container.translationY == 0F) {
            ObjectAnimator.ofFloat(bottom_container, "translationY", 0F, bottom_container.height.toFloat())
                .apply {
                    duration = 250
                    start()
                }
        }
    }

    private fun showBottomPanelAnimation() {
        if (bottom_container.translationY > 0F) {
            ObjectAnimator.ofFloat(bottom_container, "translationY", bottom_container.height.toFloat(), 0F)
                .apply {
                    duration = 250
                    start()
                }
        }
    }

    private fun getElement(counter: Int): ChatMessage {
        return when (counter) {
            0 -> ChatMessage.BotMessage("1", "Hola!", "привет", true)
            1 -> ChatMessage.UserMessage("2", "Hola U+1F600", isSpeaking = true)
            2 -> ChatMessage.BotMessage("3", "Commo te lammas?", "как дела", false)
            3 -> ChatMessage.UserMessage("4", "John")
            4 -> ChatMessage.BotMessage("5", "Encantanda.", "приятно познакомиться", false)
            5 -> ChatMessage.UserMessage("6", "Hola, encantando de conocerte")
            6 -> ChatMessage.BotMessage("7", "Como estas?", "как дела?", false, showBotAvatar = false)
            7 -> ChatMessage.BotMessage("8", "Es un placer.", "рад слышать", false)
            else -> ChatMessage.BotMessage("9", "Hola!", "привет", false)
        }
    }

    private fun fillAdapterWithMockData() {
        TransitionManager.beginDelayedTransition(bottom_container, Slide().apply {
            slideEdge = Gravity.LEFT
            addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    val animX = ObjectAnimator.ofFloat(first_suggestion, "scaleX", 1.5f)
                    val animY = ObjectAnimator.ofFloat(first_suggestion, "scaleY", 1.5f)
                    val animRX = ObjectAnimator.ofFloat(first_suggestion, "scaleX", 1f)
                    val animRY = ObjectAnimator.ofFloat(first_suggestion, "scaleY", 1f)
                    AnimatorSet().apply {
                        play(animX).with(animY).before(animRX)
                        play(animRX).with(animRY)
                        duration = 240
                        start()
                    }
                }

                override fun onTransitionResume(transition: Transition) {
                }

                override fun onTransitionPause(transition: Transition) {
                }

                override fun onTransitionCancel(transition: Transition) {
                }

                override fun onTransitionStart(transition: Transition) {
                }

            })
        })
        SuggestionViewBinder.bindView(
            first_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
        first_suggestion.visibility = View.VISIBLE
        Handler().postDelayed({
            TransitionManager.beginDelayedTransition(bottom_container, Slide().apply { slideEdge = Gravity.LEFT })
            SuggestionViewBinder.bindView(
                second_suggestion as ViewGroup,
                ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
            ) {

            }
            second_suggestion.visibility = View.VISIBLE
        }, 500L)
        Handler().postDelayed({
            TransitionManager.beginDelayedTransition(bottom_container, Slide().apply { slideEdge = Gravity.LEFT })
            SuggestionViewBinder.bindView(
                third_suggestion as ViewGroup,
                ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
            ) {

            }
            third_suggestion.visibility = View.VISIBLE
        }, 1000L)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TTS_RESULT_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                speaker = Speaker(this, chatLanguage)
            } else {
                startActivity(Intent().apply { action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA; })
            }
        }
    }

    private fun readIntent() {
        chatLanguage = (intent.getSerializableExtra(EXTRA_CHATBOT_LANGUAGE) as? Locale) ?: Locale.getDefault()
        chatTheme = intent.getStringExtra(EXTRA_CHATBOT_THEME)
        label_title.text = chatTheme

    }

    private fun checkTTS() {
        val check = Intent()
        check.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(check, TTS_RESULT_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        speaker?.destroy()
    }
}