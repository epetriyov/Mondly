package com.atistudios.mondly.languages.chatbot.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atistudios.mondly.languages.chatbot.R
import com.atistudios.mondly.languages.chatbot.entitites.ChatMessage
import com.atistudios.mondly.languages.chatbot.entitites.ResponseSuggestion
import com.atistudios.mondly.languages.chatbot.utilities.Speaker
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_chatbot.*
import java.util.*


class ChatBotActivity : AppCompatActivity() {

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
        fillAdapterWithMockData()
        val mockData =
            mutableListOf<ChatMessage>(ChatMessage.Footer(resources.getDimension(R.dimen.footer_height).toInt()))
        var counter = 0
        btn_microphone.setOnClickListener {
            mockData.add(mockData.size - 1, getElement(counter))
            chatAdapter.submitList(mockData.toMutableList())
            counter++
        }
        btn_more_options.setOnClickListener {
            val list = mutableListOf(
                ChatMessage.BotMessage("1", "Hola!", "привет", false),
                ChatMessage.UserMessage("2", "Hola U+1F600", isSpeaking = false),
                ChatMessage.Footer(resources.getDimension(R.dimen.footer_height).toInt())
            )
            chatAdapter.submitList(list)
        }
        var showTranslation = true
        btn_close.setOnClickListener {
            showTranslation = !showTranslation
            chatAdapter.setTranslationVisibility(showTranslation)
        }
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
        SuggestionViewBinder.bindView(
            first_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
        SuggestionViewBinder.bindView(
            second_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
        SuggestionViewBinder.bindView(
            third_suggestion as ViewGroup,
            ResponseSuggestion(null, "Encantanda", "приятно познакомиться")
        ) {

        }
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