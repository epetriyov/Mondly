package com.atistudios.mondly.languages.chatbot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.transition.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.atistudios.mondly.languages.chatbot.ext.hideKeyboard
import com.atistudios.mondly.languages.chatbot.ext.scaleAnimation
import com.atistudios.mondly.languages.chatbot.ext.slideDown
import com.atistudios.mondly.languages.chatbot.ext.slideUp
import com.atistudios.mondly.languages.chatbot.listeners.EndSpeechDelegate
import com.atistudios.mondly.languages.chatbot.listeners.EndTextToSpeechCallback
import com.atistudios.mondly.languages.chatbot.listeners.TransitionEndListener
import jp.wasabeef.recyclerview.animators.BaseItemAnimator
import kotlinx.android.synthetic.main.activity_chat.*
import net.gotev.speech.Speech
import java.util.*


interface ChatView {

    fun suggestionsLoaded(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion, ResponseSuggestion>, introAnimations: Boolean
    )

    fun botMessageLoading()

    fun chatUpdated(messages: List<ChatMessage>)

    fun progressStateChanged(isLoading: Boolean)

    fun speak(message: String, speakRate: Float? = null, speakEndListener: EndTextToSpeechCallback? = null)
}

class ChatBotActivity : AppCompatActivity(), ChatView {

    companion object {

        private val EXTRA_CHATBOT_LANGUAGE = "extra_chatbot_language"
        private val EXTRA_CHATBOT_TITLE = "extra_chatbot_title"
        private const val MY_PERMISSIONS_RECORD_AUDIO = 89
        private const val PLAYBACK_WAS_ALREADY_CLICKED = "pb_clicked_tag"

        private const val BOTTOM_PANEL_SLIDE_DURATION = 250L
        private const val MICROPHONE_SCALE_DURATION = 200L
        private const val MICROPHONE_SCALE_FACTOR = 1.2F
        private const val FIRST_SUGGESTION_SCALE_DURATION = 250L
        private const val FIRST_SUGGESTION_SCALE_FACTOR = 1.05F
        private const val ALPHA_CONTROLS_DISABLED = 0.5F
        private const val SEND_USER_ANSWER_DELAY = 250L
        private const val OPTION_SLIDE_DURATION = 250L
        private const val OPTION_SPEAK_DELAY = 200L
        private const val SPEAK_RATE_SLOWER = 0.5F
        private const val MICROPHONE_START_ANIMATION_DELAY = 3600L
        private const val MICROPHONE_REPEAT_DURATION = 7000L

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

    private var loopMicAnimation = true

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
        btn_microphone.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> checkPermission()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> speakFinished()
                else -> {
                    //do nothing
                }
            }
            true
        }
        btn_send.setOnClickListener {
            hideKeyboard(edit_answer)
            handler.postDelayed(
                {
                    chatEngine.onUserAnswered(edit_answer.text.toString(), true)
                    edit_answer.text = null
                },
                SEND_USER_ANSWER_DELAY
            )
        }
        btn_more_options.setOnClickListener {
            TransitionManager.beginDelayedTransition(bottom_container)
            options_group.isVisible = !options_group.isVisible
            btn_more_options.alpha = if (options_group.isVisible) 1F else ALPHA_CONTROLS_DISABLED
            updateFooterHeight()
        }
        btn_change_input_type.setOnClickListener { controlModeClicked() }
        switch_auto_play.setOnCheckedChangeListener { _, isChecked -> chatEngine.onAutoPlayModeChanged(isChecked) }
        switch_translations.setOnCheckedChangeListener { _, isChecked -> translationsVisibilityChanged(isChecked) }
        edit_answer.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_send.alpha = if (s.isNullOrEmpty()) ALPHA_CONTROLS_DISABLED else 1F
            }
        })
        chatEngine.onChatOpened()
        updateFooterHeight()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatEngine.onDestroy()
        Speech.getInstance().shutdown()
        handler.removeCallbacksAndMessages(null)
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
        loopMicAnimation = true
        handler.post {
            TransitionManager.go(Scene(bottom_container), AutoTransition().apply {
                addListener(object : TransitionEndListener() {
                    override fun onTransitionEnd(transition: Transition) {
                        showSuggestions(suggestions)
                    }
                })
            })
            if (introAnimations) {
                label_suggestions.isInvisible = false
            }
            setControlsEnabled(true)
            handler.postDelayed({
                microphoneBounceAnimation()
                loopMicroPhoneAnimation()
            }, MICROPHONE_START_ANIMATION_DELAY)
        }
    }

    override fun botMessageLoading() {
        TransitionManager.beginDelayedTransition(bottom_container)
        setControlsEnabled(false)
        first_suggestion.isInvisible = true
        second_suggestion.isInvisible = true
        third_suggestion.isInvisible = true
    }

    override fun progressStateChanged(isLoading: Boolean) {
        progressBar.isInvisible = !isLoading
    }

    override fun speak(message: String, speakRate: Float?, speakEndListener: EndTextToSpeechCallback?) {
        Speech.getInstance()
            .apply { setTextToSpeechRate(speakRate ?: 1F) }
            .say(message, speakEndListener)
    }

    private fun initRecyclerView() {
        recycler_view_chat_bot.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = object : BaseItemAnimator() {
                override fun animateRemoveImpl(holder: RecyclerView.ViewHolder?) {}
                override fun animateAddImpl(holder: RecyclerView.ViewHolder?) {}
            }
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                .apply { setDrawable(getDrawable(R.drawable.divider)) })
            adapter = chatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {


                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == SCROLL_STATE_IDLE) {
                        if (recycler_view_chat_bot.canScrollVertically(-1)) {
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
                    } else if (dy != 0) {
                        toolbar.elevation = resources.getDimension(R.dimen.chatbot_control_panel_elevation)
                        toolbar.setBackgroundColor(
                            ContextCompat.getColor(
                                this@ChatBotActivity,
                                R.color.dusk_blue
                            )
                        )
                    }
                }
            })
        }
    }

    private fun initBottomPanel() {
        first_suggestion.findViewById<View>(R.id.translation_suggestion)
            .apply {
                pivotY = 0F
            }
        second_suggestion.findViewById<View>(R.id.translation_suggestion)
            .apply {
                pivotY = 0F
            }
        third_suggestion.findViewById<View>(R.id.translation_suggestion)
            .apply {
                pivotY = 0F
            }
        label_suggestions.isInvisible = true
        first_suggestion.isInvisible = true
        second_suggestion.isInvisible = true
        third_suggestion.isInvisible = true
        edit_text_group.isVisible = false
        options_group.isVisible = false
        btn_more_options.alpha = ALPHA_CONTROLS_DISABLED
        setControlsEnabled(false)
        img_pulse_microphone.alpha = ALPHA_CONTROLS_DISABLED
        pulsator.isInvisible = true
        btn_send.alpha = ALPHA_CONTROLS_DISABLED
    }

    private fun setControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1F else ALPHA_CONTROLS_DISABLED
        btn_microphone.isEnabled = enabled
        btn_microphone.alpha = alpha
        btn_send.isEnabled = enabled
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
        loopMicAnimation = false
        handler.post {
            TransitionManager.go(Scene(bottom_container), AutoTransition()
                .apply {
                    addTarget(btn_microphone)
                    addTarget(pulsator)
                })
            btn_microphone.isInvisible = true
            pulsator.isInvisible = false
            Speech.getInstance().startListening(object : EndSpeechDelegate() {
                override fun onSpeechResult(result: String?) {
                    if (Speech.getInstance().isListening) {
                        Speech.getInstance().stopListening()
                    }
                    chatEngine.onUserAnswered(result, false)
                    speakFinished()
                }
            })
            chatEngine.onUserSpeakStarted()
            pulsator.start()
        }
    }

    private fun speakFinished() {
        if (pulsator.isStarted) {
            handler.post {
                TransitionManager.go(Scene(bottom_container), AutoTransition()
                    .apply {
                        addTarget(btn_microphone)
                        addTarget(pulsator)
                    })
                pulsator.stop()
                pulsator.isInvisible = true
                btn_microphone.isInvisible = false
            }
        }
    }

    private fun controlModeClicked() {
        handler.post {
            TransitionManager.go(Scene(bottom_container), AutoTransition()
                .apply {
                    addTarget(btn_change_input_type)
                    addTarget(btn_microphone)
                    addTarget(container_edit_text)
                    addTarget(edit_answer)
                    addTarget(btn_send)
                })
            btn_change_input_type.setImageResource(
                if (!btn_microphone.isInvisible) R.drawable.ic_microphone else R.drawable.ic_keyboard
            )
            btn_microphone.isInvisible = !btn_microphone.isInvisible
            edit_text_group.isVisible = !edit_text_group.isVisible
        }
    }

    private fun translationsVisibilityChanged(areTranslationsVisible: Boolean) {
        chatEngine.onTranslationsVisibilityChanged(areTranslationsVisible)
        if (areTranslationsVisible) {
            (first_suggestion as MotionLayout).transitionToStart()
            (second_suggestion as MotionLayout).transitionToStart()
            (third_suggestion as MotionLayout).transitionToStart()
        } else {
            (first_suggestion as MotionLayout).transitionToEnd()
            (second_suggestion as MotionLayout).transitionToEnd()
            (third_suggestion as MotionLayout).transitionToEnd()
        }
    }

    private fun showSuggestions(
        suggestions: Triple<ResponseSuggestion, ResponseSuggestion,
                ResponseSuggestion>
    ) {
        bindSuggestion(first_suggestion as ViewGroup, suggestions.first)
        bindSuggestion(second_suggestion as ViewGroup, suggestions.second)
        bindSuggestion(third_suggestion as ViewGroup, suggestions.third)
        showSuggestion(suggestions.first.text, first_suggestion,
            object : TransitionEndListener() {
                override fun onTransitionEnd(transition: Transition) {
                    showSuggestion(suggestions.second.text, second_suggestion,
                        object : TransitionEndListener() {
                            override fun onTransitionEnd(transition: Transition) {
                                showSuggestion(
                                    suggestions.third.text,
                                    third_suggestion,
                                    object : TransitionEndListener() {
                                        override fun onTransitionEnd(transition: Transition?) {
                                        }

                                    })
                            }
                        })
                }
            })
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
        viewGroup.findViewById<ImageView>(R.id.btn_playback).apply {
            setOnClickListener {
                if (it.tag == PLAYBACK_WAS_ALREADY_CLICKED) {
                    speak(suggestion.text, SPEAK_RATE_SLOWER)
                } else {
                    it.tag = PLAYBACK_WAS_ALREADY_CLICKED
                    speak(suggestion.text)
                    (drawable as AnimationDrawable).start()
                }
                viewGroup.findViewById<View>(R.id.text_suggestion)
                    .scaleAnimation(FIRST_SUGGESTION_SCALE_FACTOR, FIRST_SUGGESTION_SCALE_DURATION)
            }
        }
    }

    private fun showSuggestion(
        textToSpeak: String,
        suggestionViewGroup: View,
        transitionEndListener: TransitionEndListener?
    ) {
        handler.post {
            TransitionManager.go(Scene(bottom_container), Slide()
                .apply {
                    addTarget(suggestionViewGroup)
                    duration = OPTION_SLIDE_DURATION
                    slideEdge = Gravity.START
                    addListener(object : TransitionEndListener() {
                        override fun onTransitionEnd(transition: Transition?) {
                            handler.postDelayed({
                                speak(textToSpeak, speakEndListener = object : EndTextToSpeechCallback() {
                                    override fun onCompleted() {
                                        transitionEndListener?.let { it.onTransitionEnd(transition) }
                                    }

                                })
                            }, OPTION_SPEAK_DELAY)
                        }

                    })
                })
            suggestionViewGroup.isInvisible = false
        }
    }

    private fun loopMicroPhoneAnimation() {
        if (loopMicAnimation) {
            handler.postDelayed({
                if (loopMicAnimation) {
                    microphoneBounceAnimation()
                    loopMicroPhoneAnimation()
                }
            }, MICROPHONE_REPEAT_DURATION)
        }
    }

    private fun microphoneBounceAnimation() {
        btn_microphone.scaleAnimation(MICROPHONE_SCALE_FACTOR, MICROPHONE_SCALE_DURATION, true)
    }

    private fun updateFooterHeight() {
        //dirty hardcoded height of bottom panel
        chatEngine.onFooterHeightChanged(
            resources.getDimension(
                if (options_group.isVisible) R.dimen.max_footer_height else R.dimen.min_footer_height
            ).toInt()
        )
    }
}