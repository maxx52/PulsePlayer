package ru.maxx52.pulseplayer

import android.app.Application
import android.content.ComponentName
import android.os.SystemClock
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.maxx52.pulseplayer.data.MediaStoreMusicRepository
import ru.maxx52.pulseplayer.data.MusicPreferences
import ru.maxx52.pulseplayer.data.PlaybackSnapshot
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
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val sleepTimerRemainingMs: Long = 0L,
)

enum class SongSort {
    TITLE,
    ARTIST,
    DATE_ADDED,
    DURATION,
}

data class SortSettings(
    val sort: SongSort = SongSort.TITLE,
    val ascending: Boolean = true,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreMusicRepository(application)
    private val preferences = MusicPreferences(application)

    private val allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val _sortSettings = MutableStateFlow(SortSettings())
    val sortSettings: StateFlow<SortSettings> = _sortSettings
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

    val filteredSongs: StateFlow<List<Song>> = combine(
        allSongs,
        searchQuery,
        _sortSettings,
    ) { songs, query, settings ->
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) songs else songs.filter { song ->
            song.title.contains(needle, ignoreCase = true) ||
                song.artist.contains(needle, ignoreCase = true) ||
                song.album.contains(needle, ignoreCase = true) ||
                song.folder.contains(needle, ignoreCase = true)
        }
        val comparator = when (settings.sort) {
            SongSort.TITLE -> compareBy<Song> { it.title.lowercase() }
                .thenBy { it.artist.lowercase() }
            SongSort.ARTIST -> compareBy<Song> { it.artist.lowercase() }
                .thenBy { it.title.lowercase() }
            SongSort.DATE_ADDED -> compareBy<Song> { it.dateAddedSeconds }
                .thenBy { it.title.lowercase() }
            SongSort.DURATION -> compareBy<Song> { it.durationMs }
                .thenBy { it.title.lowercase() }
        }
        filtered.sortedWith(if (settings.ascending) comparator else comparator.reversed())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query: StateFlow<String> = searchQuery

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState

    private var controller: MediaController? = null
    private var savedPlaybackSnapshot: PlaybackSnapshot? = null
    private var playbackRestored = false
    private var sleepDeadlineElapsedMs: Long? = null
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
                    restorePlaybackIfReady(it)
                    updatePlayerState(it)
                }
            },
            ContextCompat.getMainExecutor(application),
        )

        viewModelScope.launch {
            savedPlaybackSnapshot = preferences.playbackSnapshot.first()
            controller?.let(::restorePlaybackIfReady)
        }

        viewModelScope.launch {
            while (true) {
                delay(500)
                controller?.let { player ->
                    updateSleepTimer(player)
                    updatePlayerState(player)
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5_000)
                controller?.takeIf { playbackRestored && it.mediaItemCount > 0 }?.let {
                    preferences.savePlaybackState(it.toPlaybackSnapshot())
                }
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
                    controller?.let(::restorePlaybackIfReady)
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

    fun selectSort(sort: SongSort) {
        _sortSettings.update { current ->
            if (current.sort == sort) {
                current.copy(ascending = !current.ascending)
            } else {
                SortSettings(
                    sort = sort,
                    ascending = sort != SongSort.DATE_ADDED,
                )
            }
        }
    }

    fun play(song: Song, queue: List<Song>) {
        val activeController = controller ?: return
        val playableQueue = queue.ifEmpty { listOf(song) }
        val index = playableQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        activeController.setMediaItems(playableQueue.map(Song::toMediaItem), index, 0L)
        activeController.prepare()
        activeController.play()
    }

    fun playNext(song: Song) {
        controller?.let { player ->
            if (player.mediaItemCount == 0) {
                player.setMediaItem(song.toMediaItem())
                player.prepare()
                player.play()
            } else {
                player.addMediaItem((player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount), song.toMediaItem())
            }
            updatePlayerState(player)
        }
    }

    fun addToQueue(song: Song) {
        controller?.let { player ->
            player.addMediaItem(song.toMediaItem())
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            updatePlayerState(player)
        }
    }

    fun playQueueItem(index: Int) {
        controller?.takeIf { index in 0 until it.mediaItemCount }?.let {
            it.seekToDefaultPosition(index)
            it.play()
        }
    }

    fun removeQueueItem(index: Int) {
        controller?.takeIf { index in 0 until it.mediaItemCount }?.let {
            it.removeMediaItem(index)
            updatePlayerState(it)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        controller?.let {
            if (fromIndex in 0 until it.mediaItemCount && toIndex in 0 until it.mediaItemCount) {
                it.moveMediaItem(fromIndex, toIndex)
                updatePlayerState(it)
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepDeadlineElapsedMs = minutes?.let {
            SystemClock.elapsedRealtime() + it * 60_000L
        }
        controller?.let(::updatePlayerState)
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

    private fun restorePlaybackIfReady(player: MediaController) {
        if (playbackRestored) return
        if (player.mediaItemCount > 0) {
            playbackRestored = true
            return
        }
        val snapshot = savedPlaybackSnapshot ?: return
        val songs = allSongs.value
        if (songs.isEmpty()) return
        val songsById = songs.associateBy(Song::id)
        val restoredQueue = snapshot.queueIds.mapNotNull(songsById::get)
        playbackRestored = true
        if (restoredQueue.isEmpty()) return
        val index = snapshot.currentIndex.coerceIn(restoredQueue.indices)
        player.setMediaItems(restoredQueue.map(Song::toMediaItem), index, snapshot.positionMs.coerceAtLeast(0L))
        player.shuffleModeEnabled = snapshot.shuffleEnabled
        player.repeatMode = snapshot.repeatMode.takeIf {
            it in Player.REPEAT_MODE_OFF..Player.REPEAT_MODE_ALL
        } ?: Player.REPEAT_MODE_OFF
        player.prepare()
    }

    private fun updateSleepTimer(player: Player) {
        val deadline = sleepDeadlineElapsedMs ?: return
        if (SystemClock.elapsedRealtime() >= deadline) {
            player.pause()
            sleepDeadlineElapsedMs = null
        }
    }

    private fun updatePlayerState(player: Player) {
        val currentItem = player.currentMediaItem
        val currentSong = currentItem?.let { item ->
            allSongs.value.firstOrNull { it.id.toString() == item.mediaId }
                ?: item.toFallbackSong()
        }
        val queue = List(player.mediaItemCount) { index ->
            val item = player.getMediaItemAt(index)
            allSongs.value.firstOrNull { it.id.toString() == item.mediaId }
                ?: item.toFallbackSong()
        }.filterNotNull()
        val remainingSleepMs = sleepDeadlineElapsedMs
            ?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }
            ?: 0L
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
                queue = queue,
                currentQueueIndex = player.currentMediaItemIndex,
                sleepTimerRemainingMs = remainingSleepMs,
            )
        }
    }

    private fun Player.toPlaybackSnapshot(): PlaybackSnapshot = PlaybackSnapshot(
        queueIds = List(mediaItemCount) { getMediaItemAt(it).mediaId.toLongOrNull() }.filterNotNull(),
        currentIndex = currentMediaItemIndex.coerceAtLeast(0),
        positionMs = currentPosition.coerceAtLeast(0L),
        shuffleEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
    )

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
            folder = "Неизвестная папка",
            dateAddedSeconds = 0L,
        )
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
