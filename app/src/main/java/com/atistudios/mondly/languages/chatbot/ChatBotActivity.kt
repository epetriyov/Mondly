package com.atistudios.mondly.languages.chatbot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.transition.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.atistudios.mondly.languages.chatbot.utils.*
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_chat.*
import net.gotev.speech.Speech
import java.util.*


internal interface ChatView {

    fun suggestionsLoaded(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion>, introAnimations: Boolean
    )

    fun botMessageLoading()

    fun chatUpdated(messages: List<ChatMessage>)

    fun progressStateChanged(isLoading: Boolean)

    fun speak(message: String)
}

class ChatBotActivity : AppCompatActivity(), ChatView {

    companion object {

        private val EXTRA_CHATBOT_LANGUAGE = "extra_chatbot_language"
        private val EXTRA_CHATBOT_TITLE = "extra_chatbot_title"
        private const val MY_PERMISSIONS_RECORD_AUDIO = 89

        private const val BOTTOM_PANEL_SLIDE_DURATION = 250L
        private const val MICROPHONE_SCALE_DURATION = 250L
        private const val MICROPHONE_SCALE_FACTOR = 1.5F
        private const val FIRST_SUGGESTION_SCALE_DURATION = 250L
        private const val FIRST_SUGGESTION_SCALE_FACTOR = 1.3F
        private const val ALPHA_CONTROLS_DISABLED = 0.5F

        // use this method to pass arguments in Activity
        fun buildIntent(context: Context, language: Locale, title: String): Intent {
            return Intent(context, ChatBotActivity::class.java).apply {
                putExtra(EXTRA_CHATBOT_LANGUAGE, language)
                putExtra(EXTRA_CHATBOT_TITLE, title)
            }
        }
    }

    private val handler = Handler()

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var chatEngine: ChatEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val chatLanguage = (intent.getSerializableExtra(EXTRA_CHATBOT_LANGUAGE) as? Locale) ?: Locale.getDefault()
        Speech.init(this, packageName).apply {
            setLocale(chatLanguage)
        }
        chatEngine = ChatEngineImpl(this, ChatListHelperImpl(), MockMessagesLoader(), handler)
        label_title.text = intent.getStringExtra(EXTRA_CHATBOT_TITLE) ?: getString(R.string.app_name)
        btn_close.setOnClickListener { finish() }
        chatAdapter = ChatAdapter {
            speak(it)
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
        btn_microphone.setOnClickListener {
            checkPermission()
        }
        btn_send.setOnClickListener {
            chatEngine.onUserAnswered(edit_answer.text.toString(), true)
            hideKeyboard(edit_answer)
            edit_answer.text = null
        }
        btn_more_options.setOnClickListener {
            TransitionManager.beginDelayedTransition(bottom_container)
            options_group.isVisible = !options_group.isVisible
            updateFooterHeight()
        }
        btn_change_input_type.setOnClickListener { controlModeClicked() }
        switch_auto_play.setOnCheckedChangeListener { _, isChecked -> chatEngine.onAutoPlayModeChanged(isChecked) }
        switch_translations.setOnCheckedChangeListener { _, isChecked -> translationsVisibilityChanged(isChecked) }
        chatEngine.onChatOpened()
        updateFooterHeight()
    }

    override fun onDestroy() {
        super.onDestroy()
        Speech.getInstance().shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    controlModeClicked()
                    btn_change_input_type.isEnabled = false
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
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
                    showSuggestions(suggestions, introAnimations)
                }
            })
        })
        if (introAnimations) {
            label_suggestions.isInvisible = false
        }
        setControlsEnabled(true)
    }

    override fun botMessageLoading() {
        TransitionManager.beginDelayedTransition(bottom_container)
        setControlsEnabled(false)
        suggestions_group.isInvisible = true
    }

    override fun progressStateChanged(isLoading: Boolean) {
        progressBar.isVisible = isLoading
    }

    override fun speak(message: String) {
        Speech.getInstance().say(message)
    }

    private fun initRecyclerView() {
        recycler_view_chat_bot.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideInLeftAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                .apply { setDrawable(getDrawable(R.drawable.divider)) })
            adapter = chatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {


                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == SCROLL_STATE_IDLE) {
                        if (recycler_view_chat_bot.canScrollVertically(-1)) {
                            toolbar.elevation = resources.getDimension(R.dimen.chatbot_control_panel_elevation)
                            toolbar.setBackgroundColor(
                                ContextCompat.getColor(
                                    this@ChatBotActivity,
                                    R.color.dusk_blue
                                )
                            )
                            bottom_container.slideUp(BOTTOM_PANEL_SLIDE_DURATION)
                        }
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0 && recycler_view_chat_bot.canScrollVertically(1) && !recycler_view_chat_bot.canScrollVertically(
                            -1
                        )
                    ) {
                        toolbar.elevation = 0F
                        toolbar.setBackgroundColor(
                            ContextCompat.getColor(this@ChatBotActivity, android.R.color.transparent)
                        )
                        bottom_container.slideDown(BOTTOM_PANEL_SLIDE_DURATION)
                    }
                }
            })
        }
    }

    private fun initBottomPanel() {
        label_suggestions.isInvisible = true
        suggestions_group.isInvisible = true
        edit_text_group.isVisible = false
        options_group.isVisible = false
        setControlsEnabled(false)
        img_pulse_microphone.alpha = ALPHA_CONTROLS_DISABLED
        pulsator.isInvisible = true
    }

    private fun setControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1F else ALPHA_CONTROLS_DISABLED
        btn_microphone.isEnabled = enabled
        btn_microphone.alpha = alpha
        btn_send.isEnabled = enabled
        btn_send.alpha = alpha
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MY_PERMISSIONS_RECORD_AUDIO
                )
            }
        } else {
            // Permission has already been granted
            speakStarted()
        }
    }

    private fun speakStarted() {
        TransitionManager.beginDelayedTransition(bottom_container)
        btn_microphone.isInvisible = true
        pulsator.isInvisible = false
        Speech.getInstance().startListening(object : EndSpeechDelegate() {
            override fun onSpeechResult(result: String?) {
                if (pulsator.isStarted) {
                    speakFinished()
                }
                chatEngine.onUserAnswered(result, false)
            }
        })
        chatEngine.onUserSpeakStarted()
        pulsator.start()
    }

    private fun speakFinished() {
        TransitionManager.beginDelayedTransition(bottom_container)
        pulsator.stop()
        pulsator.isInvisible = true
        btn_microphone.isInvisible = false
        Speech.getInstance().stopListening()
    }

    private fun controlModeClicked() {
        TransitionManager.beginDelayedTransition(bottom_container)
        btn_change_input_type.setImageResource(
            if (!btn_microphone.isInvisible) R.drawable.ic_microphone else R.drawable.ic_keyboard
        )
        btn_microphone.isInvisible = !btn_microphone.isInvisible
        edit_text_group.isVisible = !edit_text_group.isVisible
    }

    private fun translationsVisibilityChanged(areTranslationsVisible: Boolean) {
        TransitionManager.beginDelayedTransition(bottom_container as ViewGroup, AutoTransition().apply {
            addListener(object : TransitionEndListener() {
                override fun onTransitionEnd(transition: Transition?) {
                    chatEngine.onTranslationsVisibilityChanged(areTranslationsVisible)
                }

            })
        })
        first_suggestion.findViewById<View>(R.id.translation_suggestion).isVisible = areTranslationsVisible
        second_suggestion.findViewById<View>(R.id.translation_suggestion).isVisible = areTranslationsVisible
        third_suggestion.findViewById<View>(R.id.translation_suggestion).isVisible = areTranslationsVisible
    }

    private fun showSuggestions(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion,
                ResponseSuggestion>, introAnimations: Boolean
    ) {
        bindSuggestion(first_suggestion as ViewGroup, suggestions.first)
        bindSuggestion(second_suggestion as ViewGroup, suggestions.second)
        bindSuggestion(third_suggestion as ViewGroup, suggestions.third)
        handler.post {
            showSuggestion(first_suggestion,
                object : TransitionEndListener() {
                    override fun onTransitionEnd(transition: Transition) {
                        if (introAnimations) {
                            first_suggestion.findViewById<View>(R.id.text_suggestion)
                                .scaleAnimation(FIRST_SUGGESTION_SCALE_FACTOR, FIRST_SUGGESTION_SCALE_DURATION)
                        }
                        showSuggestion(second_suggestion,
                            object : TransitionEndListener() {
                                override fun onTransitionEnd(transition: Transition) {
                                    showSuggestion(third_suggestion, object : TransitionEndListener() {
                                        override fun onTransitionEnd(transition: Transition?) {
                                            if (introAnimations) {
                                                btn_microphone.scaleAnimation(
                                                    MICROPHONE_SCALE_FACTOR,
                                                    MICROPHONE_SCALE_DURATION
                                                )
                                            }
                                            suggestions_group.isInvisible = false
                                        }

                                    })
                                }
                            })
                    }
                })
        }
    }

    private fun bindSuggestion(viewGroup: ViewGroup, suggestion: ResponseSuggestion) {
        viewGroup.findViewById<ImageView>(R.id.image_message).apply {
            if (suggestion.icon != null) {
                setImageResource(suggestion.icon)
            }
        }
        viewGroup.findViewById<TextView>(R.id.text_suggestion).apply {
            text = suggestion.text
        }
        viewGroup.findViewById<TextView>(R.id.translation_suggestion).apply {
            text = suggestion.translation
        }
        viewGroup.findViewById<View>(R.id.btn_playback).apply {
            setOnClickListener {
                speak(suggestion.text)
            }
        }
    }

    private fun showSuggestion(suggestionViewGroup: View, transitionEndListener: TransitionEndListener?) {
        TransitionManager.beginDelayedTransition(bottom_container, Slide().apply {
            slideEdge = Gravity.START
            transitionEndListener?.let { addListener(it) }
        })
        suggestionViewGroup.visibility = View.VISIBLE
    }

    private fun updateFooterHeight() {
        //dirty hardcoded height of bottom panel
        val footerHeight =
            resources.getDimension(if (options_group.isVisible) R.dimen.max_footer_height else R.dimen.min_footer_height)
        chatEngine.onFooterHeightChanged(footerHeight.toInt())
    }
}