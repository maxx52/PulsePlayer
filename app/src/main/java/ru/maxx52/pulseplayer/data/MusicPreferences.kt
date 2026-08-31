package ru.maxx52.pulseplayer.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.maxx52.pulseplayer.model.Playlist

private val Context.musicDataStore by preferencesDataStore(name = "pulse_music")

data class PlaybackSnapshot(
    val queueIds: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
)

class MusicPreferences(
    private val context: Context,
) {
    val favorites: Flow<Set<Long>> = context.musicDataStore.data.map { preferences ->
        preferences[FAVORITES].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    val playlists: Flow<List<Playlist>> = context.musicDataStore.data.map { preferences ->
        preferences[PLAYLIST_NAMES].orEmpty()
            .sortedBy { it.lowercase() }
            .map { name ->
                Playlist(
                    name = name,
                    songIds = preferences[playlistKey(name)]
                        .orEmpty()
                        .mapNotNull(String::toLongOrNull)
                        .toSet(),
                )
            }
    }

    val playbackSnapshot: Flow<PlaybackSnapshot> = context.musicDataStore.data.map { preferences ->
        PlaybackSnapshot(
            queueIds = preferences[PLAYBACK_QUEUE]
                .orEmpty()
                .split(',')
                .mapNotNull(String::toLongOrNull),
            currentIndex = preferences[PLAYBACK_INDEX] ?: 0,
            positionMs = preferences[PLAYBACK_POSITION] ?: 0L,
            shuffleEnabled = preferences[PLAYBACK_SHUFFLE] ?: false,
            repeatMode = preferences[PLAYBACK_REPEAT] ?: 0,
        )
    }

    suspend fun toggleFavorite(songId: Long) {
        context.musicDataStore.edit { preferences ->
            val updated = preferences[FAVORITES].orEmpty().toMutableSet()
            val value = songId.toString()
            if (!updated.add(value)) updated.remove(value)
            preferences[FAVORITES] = updated
        }
    }

    suspend fun createPlaylist(rawName: String): String? {
        val name = rawName.trim().takeIf(String::isNotEmpty) ?: return null
        context.musicDataStore.edit { preferences ->
            preferences[PLAYLIST_NAMES] = preferences[PLAYLIST_NAMES].orEmpty() + name
        }
        return name
    }

    suspend fun deletePlaylist(name: String) {
        context.musicDataStore.edit { preferences ->
            preferences[PLAYLIST_NAMES] = preferences[PLAYLIST_NAMES].orEmpty() - name
            preferences.remove(playlistKey(name))
        }
    }

    suspend fun addToPlaylist(name: String, songId: Long) {
        context.musicDataStore.edit { preferences ->
            val key = playlistKey(name)
            preferences[key] = preferences[key].orEmpty() + songId.toString()
        }
    }

    suspend fun removeFromPlaylist(name: String, songId: Long) {
        context.musicDataStore.edit { preferences ->
            val key = playlistKey(name)
            preferences[key] = preferences[key].orEmpty() - songId.toString()
        }
    }

    suspend fun savePlaybackState(snapshot: PlaybackSnapshot) {
        context.musicDataStore.edit { preferences ->
            preferences[PLAYBACK_QUEUE] = snapshot.queueIds.joinToString(",")
            preferences[PLAYBACK_INDEX] = snapshot.currentIndex
            preferences[PLAYBACK_POSITION] = snapshot.positionMs
            preferences[PLAYBACK_SHUFFLE] = snapshot.shuffleEnabled
            preferences[PLAYBACK_REPEAT] = snapshot.repeatMode
        }
    }

    private fun playlistKey(name: String): Preferences.Key<Set<String>> {
        val encoded = Base64.encodeToString(
            name.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE,
        )
        return stringSetPreferencesKey("playlist_$encoded")
    }

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val PLAYLIST_NAMES = stringSetPreferencesKey("playlist_names")
        val PLAYBACK_QUEUE = stringPreferencesKey("playback_queue")
        val PLAYBACK_INDEX = intPreferencesKey("playback_index")
        val PLAYBACK_POSITION = longPreferencesKey("playback_position")
        val PLAYBACK_SHUFFLE = booleanPreferencesKey("playback_shuffle")
        val PLAYBACK_REPEAT = intPreferencesKey("playback_repeat")
    }
}
