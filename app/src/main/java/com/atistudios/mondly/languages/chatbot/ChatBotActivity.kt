package com.atistudios.mondly.languages.chatbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.transition.*
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
import com.atistudios.mondly.languages.chatbot.utils.Speaker
import com.atistudios.mondly.languages.chatbot.utils.scaleAnimation
import com.atistudios.mondly.languages.chatbot.utils.slideDown
import com.atistudios.mondly.languages.chatbot.utils.slideUp
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_chat.*
import java.util.*

internal interface ChatView {

    fun suggestionsLoaded(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion>,
        introAnimations: Boolean
    )

    fun botMessageLoading()

    fun chatUpdated(messages: List<ChatMessage>)
}

class ChatBotActivity : AppCompatActivity(), ChatView {

    companion object {

        private val EXTRA_CHATBOT_LANGUAGE = "extra_chatbot_language"
        private val EXTRA_CHATBOT_TITLE = "extra_chatbot_title"
        private val TTS_RESULT_CODE = 89

        private const val BOTTOM_PANEL_SLIDE_DURATION = 250L
        private const val MICROPHONE_SCALE_DURATION = 250L
        private const val MICROPHONE_SCALE_FACTOR = 1.5F
        private const val FIRST_SUGGESTION_SCALE_DURATION = 250L
        private const val FIRST_SUGGESTION_SCALE_FACTOR = 1.5F
        private const val ALPHA_CONTROLS_DISABLED = 0.5F

        fun buildIntent(context: Context, language: Locale, title: String): Intent {
            return Intent(context, ChatBotActivity::class.java).apply {
                putExtra(EXTRA_CHATBOT_LANGUAGE, language)
                putExtra(EXTRA_CHATBOT_TITLE, title)
            }
        }
    }

    private lateinit var chatAdapter: ChatAdapter

    private var speaker: Speaker? = null

    private lateinit var chatEngine: ChatEngine

    private lateinit var chatLanguage: Locale

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        chatLanguage = (intent.getSerializableExtra(EXTRA_CHATBOT_LANGUAGE) as? Locale) ?: Locale.getDefault()
        chatEngine = ChatEngineImpl(this, chatLanguage)
        label_title.text = intent.getStringExtra(EXTRA_CHATBOT_TITLE)
        btn_close.setOnClickListener { finish() }
        chatAdapter = ChatAdapter {
            speaker?.speak(it)
        }
        initRecyclerView()
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recycler_view_chat_bot.layoutManager!!.smoothScrollToPosition(
                    recycler_view_chat_bot,
                    null,
                    chatAdapter.itemCount
                )
            }
        })
        initBottomPanel()
        btn_microphone.setOnClickListener { microphoneClicked() }
        btn_more_options.setOnClickListener {
            TransitionManager.beginDelayedTransition(bottom_container)
            options_group.isVisible = !options_group.isVisible
        }
        btn_change_input_type.setOnClickListener { controlModeClicked() }
        switch_translations.setOnCheckedChangeListener { _, isChecked -> translationsVisibilityChanged(isChecked) }
        checkTTS()
        chatEngine.onChatOpened()
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

    override fun onDestroy() {
        super.onDestroy()
        speaker?.destroy()
    }

    override fun chatUpdated(messages: List<ChatMessage>) {
        chatAdapter.submitList(messages)
    }

    override fun suggestionsLoaded(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion>,
        introAnimations: Boolean
    ) {
        TransitionManager.go(Scene(bottom_container), AutoTransition().apply {
            addListener(object : TransitionEndListener() {
                override fun onTransitionEnd(transition: Transition) {
                    btn_microphone.scaleAnimation(MICROPHONE_SCALE_FACTOR, MICROPHONE_SCALE_DURATION)
                    showSuggestions(suggestions, introAnimations)
                }
            })
        })
        if (introAnimations) {
            label_suggestions.isVisible = true
        }
        setControlsEnabled(true)
    }

    override fun botMessageLoading() {
        TransitionManager.beginDelayedTransition(bottom_container)
        setControlsEnabled(false)
        suggestions_group.isInvisible = true
    }

    private fun initRecyclerView() {
        recycler_view_chat_bot.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideInLeftAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                .apply { setDrawable(getDrawable(R.drawable.divider)) })
            adapter = chatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (recycler_view_chat_bot.canScrollVertically(1)) {
                        if (!recycler_view_chat_bot.canScrollVertically(-1)) {
                            toolbar.elevation = 0F
                            toolbar.setBackgroundColor(
                                ContextCompat.getColor(this@ChatBotActivity, android.R.color.transparent)
                            )
                            bottom_container.slideDown(BOTTOM_PANEL_SLIDE_DURATION)
                        } else {
                            toolbar.elevation = resources.getDimension(R.dimen.chatbot_control_panel_elevation)
                            toolbar.setBackgroundColor(ContextCompat.getColor(this@ChatBotActivity, R.color.dusk_blue))
                            bottom_container.slideUp(BOTTOM_PANEL_SLIDE_DURATION)
                        }
                    }
                }
            })
        }
    }

    private fun initBottomPanel() {
        label_suggestions.isVisible = false
        suggestions_group.isInvisible = false
        edit_text_group.isVisible = false
        options_group.isVisible = false
        setControlsEnabled(false)
    }

    private fun setControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1F else ALPHA_CONTROLS_DISABLED
        btn_microphone.isEnabled = enabled
        btn_microphone.alpha = alpha
        btn_send.isEnabled = enabled
        btn_send.alpha = alpha
    }

    private fun checkTTS() {
        val check = Intent()
        check.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(check, TTS_RESULT_CODE)
    }

    private fun microphoneClicked() {
        //todo show micro animation
    }

    private fun controlModeClicked() {
        TransitionManager.beginDelayedTransition(bottom_container)
        btn_change_input_type.setImageResource(
            if (btn_microphone.isVisible) R.drawable.ic_microphone else R.drawable.ic_keyboard
        )
        btn_microphone.isVisible = !btn_microphone.isVisible
        edit_text_group.isVisible = !edit_text_group.isVisible
    }

    private fun translationsVisibilityChanged(areTranslationsVisible: Boolean) {
        chatAdapter.setTranslationVisibility(areTranslationsVisible)
        TransitionManager.beginDelayedTransition(bottom_container as ViewGroup)
        first_suggestion.findViewById<View>(R.id.text_translation).isVisible = areTranslationsVisible
        second_suggestion.findViewById<View>(R.id.text_translation).isVisible = areTranslationsVisible
        third_suggestion.findViewById<View>(R.id.text_translation).isVisible = areTranslationsVisible
    }

    private fun showSuggestions(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion,
                ResponseSuggestion>, introAnimations: Boolean
    ) {
        showSuggestion(suggestions.first,
            object : TransitionEndListener() {
                override fun onTransitionEnd(transition: Transition) {
                    if (introAnimations) {
                        first_suggestion.scaleAnimation(FIRST_SUGGESTION_SCALE_FACTOR, FIRST_SUGGESTION_SCALE_DURATION)
                    }
                    showSuggestion(suggestions.second,
                        object : TransitionEndListener() {
                            override fun onTransitionEnd(transition: Transition) {
                                showSuggestion(suggestions.third, null)
                            }
                        })
                }
            })
    }

    private fun showSuggestion(suggestion: ResponseSuggestion, transitionEndListener: TransitionEndListener?) {
        TransitionManager.beginDelayedTransition(bottom_container, Slide().apply {
            slideEdge = Gravity.START
            transitionEndListener?.let { addListener(it) }
        })
        SuggestionViewBinder.bindView(first_suggestion as ViewGroup, suggestion) {
            speaker?.speak(suggestion.text)
        }
        first_suggestion.visibility = View.VISIBLE
    }
}