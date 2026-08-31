package ru.maxx52.pulseplayer.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.maxx52.pulseplayer.model.Playlist

private val Context.musicDataStore by preferencesDataStore(name = "pulse_music")

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
    }
}
