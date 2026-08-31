package ru.maxx52.pulseplayer

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.maxx52.pulseplayer.data.MediaStoreMusicRepository
import ru.maxx52.pulseplayer.data.MusicPreferences
import ru.maxx52.pulseplayer.model.Playlist
import ru.maxx52.pulseplayer.model.Song
import ru.maxx52.pulseplayer.playback.PlaybackService

data class LibraryUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null,
)

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreMusicRepository(application)
    private val preferences = MusicPreferences(application)

    private val allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> = _libraryState

    val favorites: StateFlow<Set<Long>> = preferences.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )
    val playlists: StateFlow<List<Playlist>> = preferences.playlists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val filteredSongs: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        val needle = query.trim()
        if (needle.isEmpty()) songs else songs.filter { song ->
            song.title.contains(needle, ignoreCase = true) ||
                song.artist.contains(needle, ignoreCase = true) ||
                song.album.contains(needle, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query: StateFlow<String> = searchQuery

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState

    private var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        application,
        SessionToken(application, ComponentName(application, PlaybackService::class.java)),
    ).buildAsync()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updatePlayerState(player)
        }
    }

    init {
        controllerFuture.addListener(
            {
                controller = runCatching { controllerFuture.get() }.getOrNull()?.also {
                    it.addListener(playerListener)
                    updatePlayerState(it)
                }
            },
            ContextCompat.getMainExecutor(application),
        )

        viewModelScope.launch {
            while (true) {
                delay(500)
                controller?.let(::updatePlayerState)
            }
        }
    }

    fun loadLibrary() {
        if (_libraryState.value.isLoading) return
        viewModelScope.launch {
            _libraryState.value = LibraryUiState(isLoading = true)
            runCatching { repository.loadSongs() }
                .onSuccess { songs ->
                    allSongs.value = songs
                    _libraryState.value = LibraryUiState(songs = songs)
                }
                .onFailure { error ->
                    _libraryState.value = LibraryUiState(
                        error = error.localizedMessage ?: "Не удалось прочитать музыку",
                    )
                }
        }
    }

    fun updateQuery(value: String) {
        searchQuery.value = value
    }

    fun play(song: Song, queue: List<Song>) {
        val activeController = controller ?: return
        val playableQueue = queue.ifEmpty { listOf(song) }
        val index = playableQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        activeController.setMediaItems(playableQueue.map(Song::toMediaItem), index, 0L)
        activeController.prepare()
        activeController.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.let {
            if (it.currentPosition > 4_000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { preferences.toggleFavorite(songId) }
    }

    fun createPlaylist(name: String, initialSongId: Long? = null) {
        viewModelScope.launch {
            val createdName = preferences.createPlaylist(name) ?: return@launch
            initialSongId?.let { preferences.addToPlaylist(createdName, it) }
        }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch { preferences.deletePlaylist(name) }
    }

    fun addToPlaylist(name: String, songId: Long) {
        viewModelScope.launch { preferences.addToPlaylist(name, songId) }
    }

    fun removeFromPlaylist(name: String, songId: Long) {
        viewModelScope.launch { preferences.removeFromPlaylist(name, songId) }
    }

    fun songsByIds(ids: Set<Long>): List<Song> = allSongs.value.filter { it.id in ids }

    private fun updatePlayerState(player: Player) {
        val currentItem = player.currentMediaItem
        val currentSong = currentItem?.let { item ->
            allSongs.value.firstOrNull { it.id.toString() == item.mediaId }
                ?: item.toFallbackSong()
        }
        _playerState.update {
            PlayerUiState(
                currentSong = currentSong,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeUnless { it == C.TIME_UNSET || it < 0 }
                    ?: currentSong?.durationMs
                    ?: 0L,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
            )
        }
    }

    private fun MediaItem.toFallbackSong(): Song? {
        val uri = localConfiguration?.uri ?: return null
        return Song(
            id = mediaId.toLongOrNull() ?: -1L,
            title = mediaMetadata.title?.toString() ?: "Без названия",
            artist = mediaMetadata.artist?.toString() ?: "Неизвестный исполнитель",
            album = mediaMetadata.albumTitle?.toString() ?: "Без альбома",
            durationMs = 0L,
            contentUri = uri,
            artworkUri = mediaMetadata.artworkUri,
        )
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
