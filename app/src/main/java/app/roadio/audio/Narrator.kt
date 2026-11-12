package app.roadio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import app.roadio.data.PassPoint
import java.util.Locale

/**
 * 안내 음성. assets 에 녹음 파일이 있으면 그걸 틀고, 없으면 안드로이드 기본 TTS 로 읽는다.
 * (에뮬레이터에 한국어 TTS 가 있는지는 확인 필요 → 없으면 녹음 파일을 넣는다)
 */
class Narrator(private val context: Context) {
    private var player: MediaPlayer? = null
    private var ttsReady = false
    private val tts = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }

    fun play(point: PassPoint) {
        stop()
        val file = point.audioFile
        if (file != null && exists(file)) {
            runCatching {
                val afd = context.assets.openFd(file)
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                }
                afd.close()
            }.onFailure { Log.w("Narrator", "audio failed", it) }
        } else if (ttsReady) {
            tts.language = Locale.KOREAN
            val side = if (point.side.name == "R") "오른쪽" else "왼쪽"
            // 녹음 파일이 없을 때는 긴 설명(narration)을 읽는다. 긴 설명이 없으면 화면 문구와 같다.
            tts.speak("$side 창밖, ${point.name}. ${point.narration}", TextToSpeech.QUEUE_FLUSH, null, point.id)
        }
    }

    fun stop() {
        player?.release()
        player = null
        if (ttsReady) tts.stop()
    }

    fun release() {
        stop()
        tts.shutdown()
    }

    private fun exists(path: String): Boolean {
        val dir = path.substringBeforeLast('/', "")
        val name = path.substringAfterLast('/')
        return context.assets.list(dir)?.contains(name) == true
    }
}
