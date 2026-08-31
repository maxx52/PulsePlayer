package ru.maxx52.pulseplayer.model

data class Playlist(
    val name: String,
    val songIds: Set<Long>,
)
