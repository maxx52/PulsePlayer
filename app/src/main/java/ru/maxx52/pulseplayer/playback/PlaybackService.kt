package ru.maxx52.pulseplayer.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ru.maxx52.pulseplayer.widget.PulsePlayerWidget

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var pulseEqualizer: PulseEqualizer? = null

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            pulseEqualizer?.attachTo(audioSessionId)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            PulsePlayerWidget.update(this@PlaybackService, player)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        pulseEqualizer = PulseEqualizer(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(playerListener)
            }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        pulseEqualizer?.release()
        pulseEqualizer = null
        mediaSession = null
        super.onDestroy()
    }
}
