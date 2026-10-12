package ru.maxx52.pulseplayer.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import androidx.media3.common.C
import ru.maxx52.pulseplayer.data.EqualizerPreferences
import ru.maxx52.pulseplayer.data.EqualizerSettings

class PulseEqualizer(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = context.getSharedPreferences(
        EqualizerPreferences.FILE_NAME,
        Context.MODE_PRIVATE,
    )
    private val settingsStore = EqualizerPreferences(context)
    private var audioEffect: Equalizer? = null
    private var audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun attachTo(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == audioSessionId) return
        audioEffect?.release()
        audioEffect = null
        audioSessionId = sessionId
        audioEffect = runCatching { Equalizer(0, sessionId) }.getOrNull()
        apply(settingsStore.read())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        apply(settingsStore.read())
    }

    private fun apply(settings: EqualizerSettings) {
        val effect = audioEffect ?: return
        runCatching {
            effect.enabled = settings.enabled
            val bandCount = effect.numberOfBands.toInt()
            if (bandCount <= 0) return@runCatching
            val range = effect.bandLevelRange
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()
            repeat(bandCount) { bandIndex ->
                val sourceIndex = if (bandCount == 1) {
                    2
                } else {
                    (bandIndex * (settings.bands.lastIndex.toFloat() / (bandCount - 1))).toInt()
                }
                val normalized = settings.bands[sourceIndex].coerceIn(-10, 10)
                val level = if (normalized >= 0) {
                    normalized * maxLevel / 10
                } else {
                    -normalized * minLevel / 10
                }
                effect.setBandLevel(bandIndex.toShort(), level.coerceIn(minLevel, maxLevel).toShort())
            }
        }
    }

    fun release() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        audioEffect?.release()
        audioEffect = null
        audioSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
