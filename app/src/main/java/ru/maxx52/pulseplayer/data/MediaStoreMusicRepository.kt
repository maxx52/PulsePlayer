package ru.maxx52.pulseplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maxx52.pulseplayer.model.Song

class MediaStoreMusicRepository(
    private val context: Context,
) {
    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_TRACK_DURATION_MS.toString())

        buildList {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    add(
                        Song(
                            id = id,
                            title = cursor.getString(titleColumn).orUnknown("Без названия"),
                            artist = cursor.getString(artistColumn).orUnknown("Неизвестный исполнитель"),
                            album = cursor.getString(albumColumn).orUnknown("Без альбома"),
                            durationMs = cursor.getLong(durationColumn),
                            contentUri = ContentUris.withAppendedId(collection, id),
                            artworkUri = albumId.takeIf { it > 0 }?.let {
                                ContentUris.withAppendedId(ALBUM_ART_URI, it)
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun String?.orUnknown(fallback: String): String =
        this?.takeUnless { it.isBlank() || it == "<unknown>" } ?: fallback

    private companion object {
        const val MIN_TRACK_DURATION_MS = 15_000L
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
