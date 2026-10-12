package ru.maxx52.pulseplayer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class EqualizerSettings(
    val enabled: Boolean = false,
    val bands: List<Int> = List(BAND_COUNT) { 0 },
) {
    companion object {
        const val BAND_COUNT = 5
        val Flat = EqualizerSettings()
    }
}

enum class EqualizerPreset(val title: String, val bands: List<Int>) {
    FLAT("Без обработки", listOf(0, 0, 0, 0, 0)),
    BASS("Больше баса", listOf(7, 5, 1, 0, -1)),
    ROCK("Рок", listOf(5, 3, -1, 3, 5)),
    POP("Поп", listOf(-1, 3, 5, 3, -1)),
    VOCAL("Вокал", listOf(-3, 0, 5, 5, 2)),
    NIGHT("Ночной", listOf(2, 1, 0, -2, -4)),
}

class EqualizerPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    val settings: Flow<EqualizerSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ENABLED || key?.startsWith(KEY_BAND_PREFIX) == true) {
                trySend(read())
            }
        }
        trySend(read())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun read(): EqualizerSettings = EqualizerSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        bands = List(EqualizerSettings.BAND_COUNT) { index ->
            preferences.getInt("$KEY_BAND_PREFIX$index", 0).coerceIn(-10, 10)
        },
    )

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setBand(index: Int, value: Int) {
        if (index !in 0 until EqualizerSettings.BAND_COUNT) return
        preferences.edit().putInt("$KEY_BAND_PREFIX$index", value.coerceIn(-10, 10)).apply()
    }

    fun applyPreset(preset: EqualizerPreset) {
        preferences.edit().apply {
            preset.bands.forEachIndexed { index, value ->
                putInt("$KEY_BAND_PREFIX$index", value.coerceIn(-10, 10))
            }
        }.apply()
    }

    companion object {
        const val FILE_NAME = "pulse_equalizer"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BAND_PREFIX = "band_"
    }
}
