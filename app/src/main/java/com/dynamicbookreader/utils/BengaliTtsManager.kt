package com.dynamicbookreader.utils

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

sealed class TtsPlaybackState {
    object Idle : TtsPlaybackState()
    object Initializing : TtsPlaybackState()
    data class Playing(
        val paragraphIndex: Int,
        val text: String,
        val totalParagraphs: Int,
        val speed: Float
    ) : TtsPlaybackState()
    data class Paused(
        val paragraphIndex: Int,
        val text: String,
        val totalParagraphs: Int,
        val speed: Float
    ) : TtsPlaybackState()
    data class Error(val message: String) : TtsPlaybackState()
}

val TtsPlaybackState.isPlaying: Boolean get() = this is TtsPlaybackState.Playing
val TtsPlaybackState.isPaused: Boolean get() = this is TtsPlaybackState.Paused
val TtsPlaybackState.isInitializing: Boolean get() = this is TtsPlaybackState.Initializing
val TtsPlaybackState.currentParagraphIndex: Int get() = when (this) {
    is TtsPlaybackState.Playing -> paragraphIndex
    is TtsPlaybackState.Paused -> paragraphIndex
    else -> 0
}
val TtsPlaybackState.speechRate: Float get() = when (this) {
    is TtsPlaybackState.Playing -> speed
    is TtsPlaybackState.Paused -> speed
    else -> 1.0f
}

class BengaliTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var playlist: List<String> = emptyList()
    private var currentIndex = 0
    private var currentSpeed: Float = 1.0f
    private var currentPitch: Float = 1.0f

    private val scope = CoroutineScope(Dispatchers.Main)
    private var sleepTimerJob: Job? = null

    private val _playbackState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Idle)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val _isLanguageAvailable = MutableStateFlow(true)
    val isLanguageAvailable: StateFlow<Boolean> = _isLanguageAvailable.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int>(0)
    val sleepTimerSecondsLeft: StateFlow<Int> = _sleepTimerSecondsLeft.asStateFlow()

    init {
        _playbackState.value = TtsPlaybackState.Initializing
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return
            
            // Try Bengali locales
            val bnBd = Locale("bn", "BD")
            val bnIn = Locale("bn", "IN")
            val bn = Locale("bn")

            val langResult = when {
                ttsEngine.isLanguageAvailable(bnBd) >= TextToSpeech.LANG_AVAILABLE -> ttsEngine.setLanguage(bnBd)
                ttsEngine.isLanguageAvailable(bnIn) >= TextToSpeech.LANG_AVAILABLE -> ttsEngine.setLanguage(bnIn)
                ttsEngine.isLanguageAvailable(bn) >= TextToSpeech.LANG_AVAILABLE -> ttsEngine.setLanguage(bn)
                else -> {
                    _isLanguageAvailable.value = false
                    ttsEngine.setLanguage(Locale.getDefault())
                }
            }

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                _isLanguageAvailable.value = false
            }

            ttsEngine.setSpeechRate(currentSpeed)
            ttsEngine.setPitch(currentPitch)

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val idx = utteranceId?.toIntOrNull() ?: currentIndex
                    if (playlist.isNotEmpty() && idx in playlist.indices) {
                        _playbackState.value = TtsPlaybackState.Playing(
                            paragraphIndex = idx,
                            text = playlist[idx],
                            totalParagraphs = playlist.size,
                            speed = currentSpeed
                        )
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val nextIdx = (utteranceId?.toIntOrNull() ?: currentIndex) + 1
                    if (nextIdx < playlist.size) {
                        currentIndex = nextIdx
                        speakCurrentParagraph()
                    } else {
                        _playbackState.value = TtsPlaybackState.Idle
                    }
                }

                override fun onError(utteranceId: String?) {
                    _playbackState.value = TtsPlaybackState.Error("অডিও প্লে করতে সমস্যা হয়েছে।")
                }
            })

            isInitialized = true
            _playbackState.value = TtsPlaybackState.Idle
        } else {
            _playbackState.value = TtsPlaybackState.Error("Text-to-Speech ইঞ্জিন চালু করা যায়নি।")
        }
    }

    fun startReading(paragraphs: List<String>, startIndex: Int = 0) {
        if (paragraphs.isEmpty()) return
        playlist = paragraphs.filter { it.isNotBlank() }
        currentIndex = startIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
        speakCurrentParagraph()
    }

    fun pause() {
        tts?.stop()
        if (currentIndex in playlist.indices) {
            _playbackState.value = TtsPlaybackState.Paused(
                paragraphIndex = currentIndex,
                text = playlist[currentIndex],
                totalParagraphs = playlist.size,
                speed = currentSpeed
            )
        } else {
            _playbackState.value = TtsPlaybackState.Idle
        }
    }

    fun resume() {
        if (currentIndex in playlist.indices) {
            speakCurrentParagraph()
        }
    }

    fun stop() {
        tts?.stop()
        cancelSleepTimer()
        _playbackState.value = TtsPlaybackState.Idle
    }

    fun skipToNext() {
        if (currentIndex + 1 < playlist.size) {
            tts?.stop()
            currentIndex++
            speakCurrentParagraph()
        }
    }

    fun skipToPrevious() {
        if (currentIndex - 1 >= 0) {
            tts?.stop()
            currentIndex--
            speakCurrentParagraph()
        }
    }

    fun setSpeechRate(rate: Float) {
        currentSpeed = rate
        tts?.setSpeechRate(rate)
        val currentState = _playbackState.value
        if (currentState is TtsPlaybackState.Playing) {
            _playbackState.value = currentState.copy(speed = rate)
        } else if (currentState is TtsPlaybackState.Paused) {
            _playbackState.value = currentState.copy(speed = rate)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes == null || minutes <= 0) {
            _sleepTimerSecondsLeft.value = 0
            return
        }

        var remainingSeconds = minutes * 60
        _sleepTimerSecondsLeft.value = remainingSeconds

        sleepTimerJob = scope.launch {
            while (isActive && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                _sleepTimerSecondsLeft.value = remainingSeconds
            }
            if (isActive && remainingSeconds <= 0) {
                pause()
                _sleepTimerMinutes.value = null
                _sleepTimerSecondsLeft.value = 0
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerMinutes.value = null
        _sleepTimerSecondsLeft.value = 0
    }

    private fun speakCurrentParagraph() {
        if (!isInitialized || playlist.isEmpty() || currentIndex !in playlist.indices) return
        val textToSpeak = playlist[currentIndex]
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, currentIndex.toString())
        }
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, currentIndex.toString())
        _playbackState.value = TtsPlaybackState.Playing(
            paragraphIndex = currentIndex,
            text = textToSpeak,
            totalParagraphs = playlist.size,
            speed = currentSpeed
        )
    }

    fun shutdown() {
        tts?.stop()
        cancelSleepTimer()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
