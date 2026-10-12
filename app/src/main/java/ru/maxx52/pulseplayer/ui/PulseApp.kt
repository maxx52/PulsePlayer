package ru.maxx52.pulseplayer.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maxx52.pulseplayer.MainViewModel
import ru.maxx52.pulseplayer.BuildConfig
import ru.maxx52.pulseplayer.PlayerUiState
import ru.maxx52.pulseplayer.SongSort
import ru.maxx52.pulseplayer.SortSettings
import ru.maxx52.pulseplayer.data.EqualizerPreset
import ru.maxx52.pulseplayer.data.EqualizerSettings
import ru.maxx52.pulseplayer.model.Playlist
import ru.maxx52.pulseplayer.model.Song
import ru.maxx52.pulseplayer.ui.theme.PulseMint
import ru.maxx52.pulseplayer.ui.theme.PulseMuted
import ru.maxx52.pulseplayer.ui.theme.PulsePink
import ru.maxx52.pulseplayer.ui.theme.PulsePurple
import ru.maxx52.pulseplayer.ui.theme.PulseSurfaceHigh

private enum class HomeTab(val title: String, val icon: ImageVector) {
    MUSIC("Музыка", Icons.Rounded.LibraryMusic),
    FAVORITES("Любимое", Icons.Rounded.Favorite),
    PLAYLISTS("Плейлисты", Icons.Rounded.QueueMusic),
}

private enum class LibraryView(val title: String, val icon: ImageVector) {
    TRACKS("Треки", Icons.Rounded.LibraryMusic),
    ARTISTS("Исполнители", Icons.Rounded.Person),
    ALBUMS("Альбомы", Icons.Rounded.Album),
    FOLDERS("Папки", Icons.Rounded.Folder),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseApp(
    viewModel: MainViewModel,
    permissionGranted: Boolean,
    requestPermission: () -> Unit,
    showAdConsent: Boolean,
    requestAdSettings: () -> Unit,
    saveAdConsent: (Boolean) -> Unit,
) {
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sortSettings by viewModel.sortSettings.collectAsStateWithLifecycle()
    val equalizerSettings by viewModel.equalizerSettings.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(HomeTab.MUSIC) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var createPlaylistForSong by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var choosePlaylistForSong by remember { mutableStateOf<Song?>(null) }
    var openedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryView by rememberSaveable { mutableStateOf(LibraryView.TRACKS) }
    var openedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var openedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
    var openedAlbumArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var queueSongAction by remember { mutableStateOf<Song?>(null) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    var openedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    var playlistPendingDeletion by remember { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    if (!permissionGranted) {
        PermissionScreen(requestPermission)
        return
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val libraryHeader: @Composable () -> Unit = {
        LibraryHeader(libraryState.songs.size, query, viewModel::updateQuery)
        LibraryViewSelector(libraryView) {
            libraryView = it
            openedArtist = null
            openedAlbum = null
            openedAlbumArtist = null
            openedFolder = null
        }
        SortBar(sortSettings) { showSortDialog = true }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = {
                    Text(
                        text = "PULSE",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                    )
                },
                actions = {
                    IconButton(onClick = { showEqualizer = true }) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = "Эквалайзер")
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "О приложении")
                    }
                    IconButton(onClick = viewModel::loadLibrary) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить медиатеку")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding(),
            ) {
                playerState.currentSong?.let { song ->
                    MiniPlayer(
                        song = song,
                        isPlaying = playerState.isPlaying,
                        onOpen = { showPlayer = true },
                        onPlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::skipPrevious,
                        onNext = viewModel::skipNext,
                    )
                }
                PulseAdBanner()
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    HomeTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = {
                                activeTab = tab
                                if (tab != HomeTab.PLAYLISTS) openedPlaylist = null
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PulseMint,
                                selectedTextColor = PulseMint,
                                indicatorColor = PulseMint.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (activeTab) {
                HomeTab.MUSIC -> {
                    if (libraryView != LibraryView.TRACKS) libraryHeader()
                    when (libraryView) {
                        LibraryView.TRACKS -> SongList(
                            header = libraryHeader,
                            songs = songs,
                            queue = songs,
                            favorites = favorites,
                            loading = libraryState.isLoading,
                            emptyTitle = libraryState.error ?: "На телефоне пока нет музыки",
                            onPlay = viewModel::play,
                            onFavorite = viewModel::toggleFavorite,
                            onAddToPlaylist = { choosePlaylistForSong = it },
                            onQueueAction = { queueSongAction = it },
                        )

                        LibraryView.ARTISTS -> {
                            val artist = openedArtist
                            if (artist == null) {
                                ArtistOverview(
                                    songs = songs,
                                    onOpen = { openedArtist = it },
                                )
                            } else {
                                val artistSongs = songs.filter { it.artist == artist }
                                CollectionDetail(
                                    title = artist,
                                    subtitle = "${artistSongs.size} треков",
                                    icon = Icons.Rounded.Person,
                                    songs = artistSongs,
                                    favorites = favorites,
                                    onBack = { openedArtist = null },
                                    onPlay = viewModel::play,
                                    onFavorite = viewModel::toggleFavorite,
                                    onAddToPlaylist = { choosePlaylistForSong = it },
                                    onQueueAction = { queueSongAction = it },
                                )
                            }
                        }

                        LibraryView.ALBUMS -> {
                            val album = openedAlbum
                            val albumArtist = openedAlbumArtist
                            if (album == null || albumArtist == null) {
                                AlbumOverview(
                                    songs = songs,
                                    onOpen = { title, artist ->
                                        openedAlbum = title
                                        openedAlbumArtist = artist
                                    },
                                )
                            } else {
                                val albumSongs = songs.filter {
                                    it.album == album && it.artist == albumArtist
                                }
                                CollectionDetail(
                                    title = album,
                                    subtitle = "$albumArtist • ${albumSongs.size} треков",
                                    icon = Icons.Rounded.Album,
                                    songs = albumSongs,
                                    favorites = favorites,
                                    onBack = {
                                        openedAlbum = null
                                        openedAlbumArtist = null
                                    },
                                    onPlay = viewModel::play,
                                    onFavorite = viewModel::toggleFavorite,
                                    onAddToPlaylist = { choosePlaylistForSong = it },
                                    onQueueAction = { queueSongAction = it },
                                )
                            }
                        }

                        LibraryView.FOLDERS -> {
                            val folder = openedFolder
                            if (folder == null) {
                                FolderOverview(
                                    songs = songs,
                                    onOpen = { openedFolder = it },
                                )
                            } else {
                                val folderSongs = songs.filter { it.folder == folder }
                                CollectionDetail(
                                    title = folderDisplayName(folder),
                                    subtitle = "$folder • ${folderSongs.size} треков",
                                    icon = Icons.Rounded.Folder,
                                    songs = folderSongs,
                                    favorites = favorites,
                                    onBack = { openedFolder = null },
                                    onPlay = viewModel::play,
                                    onFavorite = viewModel::toggleFavorite,
                                    onAddToPlaylist = { choosePlaylistForSong = it },
                                    onQueueAction = { queueSongAction = it },
                                )
                            }
                        }
                    }
                }

                HomeTab.FAVORITES -> {
                    SectionHeader("ЛЮБИМЫЕ", "Треки, к которым хочется возвращаться")
                    val favoriteSongs = songs.filter { it.id in favorites }
                    SongList(
                        songs = favoriteSongs,
                        queue = favoriteSongs,
                        favorites = favorites,
                        loading = false,
                        emptyTitle = "Добавьте первый любимый трек",
                        onPlay = viewModel::play,
                        onFavorite = viewModel::toggleFavorite,
                        onAddToPlaylist = { choosePlaylistForSong = it },
                        onQueueAction = { queueSongAction = it },
                    )
                }

                HomeTab.PLAYLISTS -> {
                    val selectedPlaylist = playlists.firstOrNull { it.name == openedPlaylist }
                    if (selectedPlaylist == null) {
                        PlaylistOverview(
                            playlists = playlists,
                            onOpen = { openedPlaylist = it.name },
                            onCreate = { showCreatePlaylist = true },
                            onDelete = { playlistPendingDeletion = it },
                        )
                    } else {
                        val playlistSongs = viewModel.songsByIds(selectedPlaylist.songIds)
                        PlaylistDetail(
                            playlist = selectedPlaylist,
                            songs = playlistSongs,
                            favorites = favorites,
                            onBack = { openedPlaylist = null },
                            onPlay = viewModel::play,
                            onFavorite = viewModel::toggleFavorite,
                            onRemove = { song ->
                                viewModel.removeFromPlaylist(selectedPlaylist.name, song.id)
                            },
                            onQueueAction = { queueSongAction = it },
                        )
                    }
                }
            }
        }
    }

    if (showPlayer && playerState.currentSong != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FullPlayer(
                state = playerState,
                isFavorite = playerState.currentSong!!.id in favorites,
                onPlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::skipPrevious,
                onNext = viewModel::skipNext,
                onSeek = viewModel::seekTo,
                onFavorite = { viewModel.toggleFavorite(playerState.currentSong!!.id) },
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::cycleRepeatMode,
                onOpenQueue = {
                    showPlayer = false
                    showQueue = true
                },
                onOpenSleepTimer = { showSleepTimer = true },
                onOpenEqualizer = { showEqualizer = true },
            )
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            QueueSheet(
                state = playerState,
                onPlay = viewModel::playQueueItem,
                onRemove = viewModel::removeQueueItem,
                onMove = viewModel::moveQueueItem,
            )
        }
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMs = playerState.sleepTimerRemainingMs,
            onDismiss = { showSleepTimer = false },
            onSelect = {
                viewModel.setSleepTimer(it)
                showSleepTimer = false
            },
        )
    }

    if (showEqualizer) {
        ModalBottomSheet(
            onDismissRequest = { showEqualizer = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            EqualizerSheet(
                settings = equalizerSettings,
                onEnabledChange = viewModel::setEqualizerEnabled,
                onBandChange = viewModel::setEqualizerBand,
                onPreset = viewModel::applyEqualizerPreset,
            )
        }
    }

    playlistPendingDeletion?.let { name ->
        DeletePlaylistDialog(
            playlistName = name,
            onDismiss = { playlistPendingDeletion = null },
            onConfirm = {
                viewModel.deletePlaylist(name)
                playlistPendingDeletion = null
            },
        )
    }

    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            onAdSettings = {
                showAbout = false
                requestAdSettings()
            },
        )
    }

    if (showAdConsent) {
        AdConsentDialog(onSelect = saveAdConsent)
    }

    if (showSortDialog) {
        SortDialog(
            settings = sortSettings,
            onDismiss = { showSortDialog = false },
            onSelect = {
                viewModel.selectSort(it)
                showSortDialog = false
            },
        )
    }

    queueSongAction?.let { song ->
        QueueActionDialog(
            song = song,
            onDismiss = { queueSongAction = null },
            onPlayNext = {
                viewModel.playNext(song)
                queueSongAction = null
            },
            onAddToEnd = {
                viewModel.addToQueue(song)
                queueSongAction = null
            },
        )
    }

    choosePlaylistForSong?.let { song ->
        PlaylistPickerDialog(
            song = song,
            playlists = playlists,
            onDismiss = { choosePlaylistForSong = null },
            onChoose = { playlist ->
                viewModel.addToPlaylist(playlist.name, song.id)
                choosePlaylistForSong = null
            },
            onCreate = {
                createPlaylistForSong = song
                choosePlaylistForSong = null
            },
        )
    }

    if (showCreatePlaylist || createPlaylistForSong != null) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylist = false
                createPlaylistForSong = null
            },
            onCreate = { name ->
                viewModel.createPlaylist(name, createPlaylistForSong?.id)
                showCreatePlaylist = false
                createPlaylistForSong = null
            },
        )
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(PulsePurple.copy(alpha = 0.34f), Color.Transparent),
                ),
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.linearGradient(listOf(PulsePurple, PulsePink, PulseMint)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(58.dp),
                )
            }
            Spacer(Modifier.height(30.dp))
            Text("ТВОЯ МУЗЫКА.\nТВОЙ РИТМ.", fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                "Pulse найдёт аудиофайлы на телефоне. Мы ничего не загружаем в интернет.",
                color = PulseMuted,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = PulsePurple),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("РАЗРЕШИТЬ ДОСТУП", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibraryHeader(songCount: Int, query: String, onQueryChange: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PulseSurfaceHigh),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(PulsePurple.copy(alpha = 0.75f), PulsePink.copy(alpha = 0.42f)),
                        ),
                    )
                    .padding(22.dp),
            ) {
                Column {
                    Text("ТВОЯ МУЗЫКА", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("$songCount треков готовы звучать", color = Color.White.copy(alpha = 0.72f))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Название, исполнитель, альбом или папка") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить поиск")
                    }
                }
            },
        )
    }
}

@Composable
private fun LibraryViewSelector(
    selected: LibraryView,
    onSelect: (LibraryView) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryView.entries.forEach { view ->
            FilterChip(
                selected = selected == view,
                onClick = { onSelect(view) },
                label = { Text(view.title) },
                leadingIcon = { Icon(view.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun SortBar(
    settings: SortSettings,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Sort, contentDescription = null, tint = PulseMint)
            Spacer(Modifier.width(9.dp))
            Text("Сначала: ${sortTitle(settings.sort)}", modifier = Modifier.weight(1f))
            Icon(
                if (settings.ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = if (settings.ascending) "По возрастанию" else "По убыванию",
                tint = PulseMuted,
            )
        }
    }
}

@Composable
private fun ArtistOverview(
    songs: List<Song>,
    onOpen: (String) -> Unit,
) {
    val artists = songs.groupBy(Song::artist).toList().sortedBy { it.first.lowercase() }
    if (artists.isEmpty()) {
        EmptyState(Icons.Rounded.Person, "Исполнители не найдены")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(artists, key = { it.first }) { (artist, artistSongs) ->
            CollectionCard(
                title = artist,
                subtitle = "${artistSongs.size} треков • ${artistSongs.map(Song::album).distinct().size} альбомов",
                artworkUri = artistSongs.firstNotNullOfOrNull(Song::artworkUri),
                icon = Icons.Rounded.Person,
                onClick = { onOpen(artist) },
            )
        }
    }
}

@Composable
private fun AlbumOverview(
    songs: List<Song>,
    onOpen: (String, String) -> Unit,
) {
    val albums = songs.groupBy { it.album to it.artist }
        .toList()
        .sortedWith(compareBy({ it.first.first.lowercase() }, { it.first.second.lowercase() }))
    if (albums.isEmpty()) {
        EmptyState(Icons.Rounded.Album, "Альбомы не найдены")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(albums, key = { "${it.first.first}\u0000${it.first.second}" }) { (albumInfo, albumSongs) ->
            CollectionCard(
                title = albumInfo.first,
                subtitle = "${albumInfo.second} • ${albumSongs.size} треков",
                artworkUri = albumSongs.firstNotNullOfOrNull(Song::artworkUri),
                icon = Icons.Rounded.Album,
                onClick = { onOpen(albumInfo.first, albumInfo.second) },
            )
        }
    }
}

@Composable
private fun FolderOverview(
    songs: List<Song>,
    onOpen: (String) -> Unit,
) {
    val folders = songs.groupBy(Song::folder)
        .toList()
        .sortedBy { folderDisplayName(it.first).lowercase() }
    if (folders.isEmpty()) {
        EmptyState(Icons.Rounded.Folder, "Папки с музыкой не найдены")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(folders, key = { it.first }) { (folder, folderSongs) ->
            CollectionCard(
                title = folderDisplayName(folder),
                subtitle = "$folder • ${folderSongs.size} треков",
                artworkUri = folderSongs.firstNotNullOfOrNull(Song::artworkUri),
                icon = Icons.Rounded.Folder,
                onClick = { onOpen(folder) },
            )
        }
    }
}

@Composable
private fun CollectionCard(
    title: String,
    subtitle: String,
    artworkUri: Uri?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PulseSurfaceHigh),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            if (artworkUri != null) {
                AlbumArtwork(artworkUri, Modifier.size(62.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(PulsePurple, PulsePink))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = PulseMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CollectionDetail(
    title: String,
    subtitle: String,
    icon: ImageVector,
    songs: List<Song>,
    favorites: Set<Long>,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onQueueAction: (Song) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад") }
            Icon(icon, contentDescription = null, tint = PulseMint)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = PulseMuted, fontSize = 13.sp, maxLines = 1)
            }
        }
        SongList(
            songs = songs,
            queue = songs,
            favorites = favorites,
            loading = false,
            emptyTitle = "Треки не найдены",
            onPlay = onPlay,
            onFavorite = onFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onQueueAction = onQueueAction,
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = PulseMuted)
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    queue: List<Song>,
    favorites: Set<Long>,
    loading: Boolean,
    emptyTitle: String,
    onPlay: (Song, List<Song>) -> Unit,
    onFavorite: (Long) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onQueueAction: (Song) -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 4.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        header?.let { content -> item(key = "library_header") { Column { content() } } }
        if (loading || songs.isEmpty()) {
            item(key = "empty_state") {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(if (loading) "Сканируем медиатеку…" else emptyTitle, color = PulseMuted)
                }
            }
        }
        items(songs, key = Song::id) { song ->
            SongRow(
                song = song,
                isFavorite = song.id in favorites,
                onPlay = { onPlay(song, queue) },
                onFavorite = { onFavorite(song.id) },
                onPlaylistAction = { onAddToPlaylist(song) },
                onQueueAction = { onQueueAction(song) },
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onPlaylistAction: () -> Unit,
    onQueueAction: (() -> Unit)? = null,
    playlistActionIsRemove: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(song.artworkUri, Modifier.size(58.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${song.artist}  •  ${formatDuration(song.durationMs)}",
                    color = PulseMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Избранное",
                    tint = if (isFavorite) PulsePink else PulseMuted,
                )
            }
            onQueueAction?.let { queueAction ->
                IconButton(onClick = queueAction, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Действия с очередью",
                        tint = PulseMuted,
                    )
                }
            }
            IconButton(onClick = onPlaylistAction, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (playlistActionIsRemove) Icons.Rounded.DeleteOutline else Icons.Rounded.PlaylistAdd,
                    contentDescription = if (playlistActionIsRemove) "Убрать" else "В плейлист",
                    tint = PulseMint,
                )
            }
        }
    }
}

@Composable
private fun PlaylistOverview(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit,
    onCreate: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("ПЛЕЙЛИСТЫ", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Соберите настроение в один список", color = PulseMuted)
            }
            FilledIconButton(onClick = onCreate) {
                Icon(Icons.Rounded.Add, contentDescription = "Новый плейлист")
            }
        }
        if (playlists.isEmpty()) {
            EmptyState(Icons.Rounded.QueueMusic, "Создайте свой первый плейлист")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(playlists, key = Playlist::name) { playlist ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = PulseSurfaceHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(playlist) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(listOf(PulsePurple, PulsePink)),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("${playlist.songIds.size} треков", color = PulseMuted)
                            }
                            IconButton(onClick = { onDelete(playlist.name) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: Playlist,
    songs: List<Song>,
    favorites: Set<Long>,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onFavorite: (Long) -> Unit,
    onRemove: (Song) -> Unit,
    onQueueAction: (Song) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
            }
            Column {
                Text(playlist.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${songs.size} треков", color = PulseMuted)
            }
        }
        if (songs.isEmpty()) {
            EmptyState(Icons.Rounded.PlaylistAdd, "Добавьте музыку в этот плейлист")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(songs, key = Song::id) { song ->
                    SongRow(
                        song = song,
                        isFavorite = song.id in favorites,
                        onPlay = { onPlay(song, songs) },
                        onFavorite = { onFavorite(song.id) },
                        onPlaylistAction = { onRemove(song) },
                        onQueueAction = { onQueueAction(song) },
                        playlistActionIsRemove = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PulseSurfaceHigh),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(song.artworkUri, Modifier.size(50.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = PulseMuted, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Предыдущий")
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Пауза или воспроизведение",
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий")
            }
        }
    }
}

@Composable
private fun FullPlayer(
    state: PlayerUiState,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    val song = state.currentSong ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArtwork(
            artworkUri = song.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 34.dp,
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(song.artist, color = PulseMuted, fontSize = 16.sp)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Избранное",
                    tint = if (isFavorite) PulsePink else PulseMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Slider(
            value = state.positionMs.coerceAtMost(state.durationMs).toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.positionMs), color = PulseMuted, fontSize = 12.sp)
            Text(formatDuration(state.durationMs), color = PulseMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Перемешать",
                    tint = if (state.shuffleEnabled) PulseMint else PulseMuted,
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Предыдущий", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(74.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PulsePurple),
            ) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Пауза или воспроизведение",
                    modifier = Modifier.size(38.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onRepeat) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Повтор",
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) PulseMint else PulseMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = onOpenEqualizer) {
                Icon(Icons.Rounded.Equalizer, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Звук")
            }
            TextButton(onClick = onOpenQueue) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Очередь (${state.queue.size})")
            }
            TextButton(onClick = onOpenSleepTimer) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text(
                    if (state.sleepTimerRemainingMs > 0L) {
                        "Сон ${formatTimer(state.sleepTimerRemainingMs)}"
                    } else {
                        "Таймер сна"
                    },
                )
            }
        }
    }
}

@Composable
private fun EqualizerSheet(
    settings: EqualizerSettings,
    onEnabledChange: (Boolean) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onPreset: (EqualizerPreset) -> Unit,
) {
    val labels = listOf("60 Гц", "230 Гц", "910 Гц", "3,6 кГц", "14 кГц")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ЭКВАЛАЙЗЕР", fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text(
                    if (settings.enabled) "Обработка звука включена" else "Звук без обработки",
                    color = PulseMuted,
                )
            }
            Switch(checked = settings.enabled, onCheckedChange = onEnabledChange)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqualizerPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.bands == preset.bands,
                    onClick = { onPreset(preset) },
                    label = { Text(preset.title) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.width(70.dp), color = PulseMuted, fontSize = 12.sp)
                Slider(
                    value = settings.bands.getOrElse(index) { 0 }.toFloat(),
                    onValueChange = { onBandChange(index, it.toInt()) },
                    valueRange = -10f..10f,
                    steps = 19,
                    enabled = settings.enabled,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%+d".format(settings.bands.getOrElse(index) { 0 }),
                    modifier = Modifier.width(34.dp),
                    fontSize = 12.sp,
                    color = if (settings.enabled) PulseMint else PulseMuted,
                )
            }
        }
        Text(
            "Настройки применяются к фоновому воспроизведению и сохраняются между запусками.",
            color = PulseMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun QueueSheet(
    state: PlayerUiState,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 30.dp),
    ) {
        Text("ОЧЕРЕДЬ", fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Меняйте порядок или выбирайте следующий трек", color = PulseMuted)
        Spacer(Modifier.height(14.dp))
        if (state.queue.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Text("Очередь пока пуста", color = PulseMuted)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            ) {
                items(state.queue.size, key = { index -> "${state.queue[index].id}:$index" }) { index ->
                    val song = state.queue[index]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == state.currentQueueIndex) {
                                PulsePurple.copy(alpha = 0.22f)
                            } else {
                                PulseSurfaceHigh
                            },
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onPlay(index) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlbumArtwork(song.artworkUri, Modifier.size(46.dp), 14.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = PulseMuted, fontSize = 12.sp, maxLines = 1)
                            }
                            IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Выше")
                            }
                            IconButton(
                                onClick = { onMove(index, index + 1) },
                                enabled = index < state.queue.lastIndex,
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Ниже")
                            }
                            IconButton(onClick = { onRemove(index) }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Убрать", tint = PulsePink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueActionDialog(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToEnd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в очередь") },
        text = {
            Column {
                Text(song.title, color = PulseMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onPlayNext, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Воспроизвести следующим")
                }
                TextButton(onClick = onAddToEnd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить в конец очереди")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun SleepTimerDialog(
    remainingMs: Long,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = PulsePurple) },
        title = { Text("Таймер сна") },
        text = {
            Column {
                if (remainingMs > 0L) {
                    Text("Музыка остановится через ${formatTimer(remainingMs)}", color = PulseMint)
                    Spacer(Modifier.height(8.dp))
                }
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TextButton(
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Через $minutes минут")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (remainingMs > 0L) {
                TextButton(onClick = { onSelect(null) }) { Text("Отключить") }
            } else {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun SortDialog(
    settings: SortSettings,
    onDismiss: () -> Unit,
    onSelect: (SongSort) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Sort, contentDescription = null, tint = PulseMint) },
        title = { Text("Сортировка") },
        text = {
            Column {
                SongSort.entries.forEach { sort ->
                    TextButton(
                        onClick = { onSelect(sort) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            sortTitle(sort),
                            modifier = Modifier.weight(1f),
                            fontWeight = if (settings.sort == sort) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (settings.sort == sort) {
                            Icon(
                                if (settings.ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Повторный выбор текущего варианта меняет направление.",
                    color = PulseMuted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun PlaylistPickerDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onChoose: (Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в плейлист") },
        text = {
            Column {
                Text(song.title, color = PulseMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChoose(playlist) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = PulseMint)
                        Spacer(Modifier.width(10.dp))
                        Text(playlist.name)
                    }
                }
                if (playlists.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                TextButton(onClick = onCreate) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Новый плейлист")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Название") },
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = PulsePink) },
        title = { Text("Удалить плейлист?") },
        text = {
            Text(
                "Плейлист «$playlistName» будет удалён. Сами аудиофайлы останутся на телефоне.",
                color = PulseMuted,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PulsePink),
            ) {
                Text("Удалить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, onAdSettings: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = PulseMint) },
        title = { Text("Pulse Player ${BuildConfig.VERSION_NAME}") },
        text = {
            Column {
                Text(
                    "Офлайн-плеер для музыки на вашем телефоне. Медиатека и плейлисты не загружаются на сервер.",
                    color = PulseMuted,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Бесплатная версия содержит рекламу Яндекса.",
                    color = PulseMuted,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        uriHandler.openUri(
                            "https://github.com/maxx52/PulsePlayer/blob/main/docs/PRIVACY_POLICY.md",
                        )
                    },
                ) {
                    Text("Политика конфиденциальности")
                }
                TextButton(onClick = onAdSettings) {
                    Text("Настройки рекламы")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun AdConsentDialog(onSelect: (Boolean) -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = PulseMint) },
        title = { Text("Реклама и конфиденциальность") },
        text = {
            Column {
                Text(
                    "Pulse бесплатный и показывает рекламу Яндекса. Можно разрешить персонализацию или использовать менее персонализированную рекламу.",
                    color = PulseMuted,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        uriHandler.openUri(
                            "https://github.com/maxx52/PulsePlayer/blob/main/docs/PRIVACY_POLICY.md",
                        )
                    },
                ) {
                    Text("Открыть политику конфиденциальности")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(true) }) {
                Text("Разрешить")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSelect(false) }) {
                Text("Без персонализации")
            }
        },
    )
}

@Composable
private fun EmptyState(icon: ImageVector, title: String) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = PulsePurple, modifier = Modifier.size(62.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = PulseMuted, fontSize = 17.sp)
        }
    }
}

@Composable
private fun AlbumArtwork(
    artworkUri: Uri?,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 17.dp,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = artworkUri) {
        value = artworkUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(listOf(PulsePurple, PulsePink, PulseMint)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Обложка",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun sortTitle(sort: SongSort): String = when (sort) {
    SongSort.TITLE -> "по названию"
    SongSort.ARTIST -> "по исполнителю"
    SongSort.DATE_ADDED -> "по дате добавления"
    SongSort.DURATION -> "по длительности"
}

private fun folderDisplayName(folder: String): String = folder
    .trimEnd('/', '\\')
    .substringAfterLast('/')
    .substringAfterLast('\\')
    .ifBlank { "Корневая папка" }

private fun formatTimer(milliseconds: Long): String {
    val totalMinutes = (milliseconds.coerceAtLeast(0L) + 59_999L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours ч ${minutes.toString().padStart(2, '0')} мин" else "$minutes мин"
}
