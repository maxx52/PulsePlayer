package ru.maxx52.pulseplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import ru.maxx52.pulseplayer.MainActivity
import ru.maxx52.pulseplayer.R
import ru.maxx52.pulseplayer.playback.PlaybackService

class PulsePlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, createRemoteViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action !in PLAYER_ACTIONS) return
        val pendingResult = goAsync()
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    when (intent.action) {
                        ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                        ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                        ACTION_NEXT -> controller.seekToNextMediaItem()
                    }
                }
                MediaController.releaseFuture(future)
                pendingResult.finish()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    companion object {
        const val ACTION_PREVIOUS = "ru.maxx52.pulseplayer.widget.PREVIOUS"
        const val ACTION_PLAY_PAUSE = "ru.maxx52.pulseplayer.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "ru.maxx52.pulseplayer.widget.NEXT"
        private val PLAYER_ACTIONS = setOf(ACTION_PREVIOUS, ACTION_PLAY_PAUSE, ACTION_NEXT)

        fun update(context: Context, player: Player) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PulsePlayerWidget::class.java)
            val views = createRemoteViews(
                context = context,
                title = player.mediaMetadata.title?.toString().orEmpty(),
                artist = player.mediaMetadata.artist?.toString().orEmpty(),
                isPlaying = player.isPlaying,
            )
            manager.updateAppWidget(component, views)
        }

        private fun createRemoteViews(
            context: Context,
            title: String? = null,
            artist: String? = null,
            isPlaying: Boolean? = null,
        ): RemoteViews {
            val saved = context.getSharedPreferences("pulse_widget", Context.MODE_PRIVATE)
            val resolvedTitle = title ?: saved.getString("title", null).orEmpty()
            val resolvedArtist = artist ?: saved.getString("artist", null).orEmpty()
            val resolvedPlaying = isPlaying ?: saved.getBoolean("playing", false)

            if (title != null || artist != null || isPlaying != null) {
                saved.edit()
                    .putString("title", resolvedTitle)
                    .putString("artist", resolvedArtist)
                    .putBoolean("playing", resolvedPlaying)
                    .apply()
            }

            return RemoteViews(context.packageName, R.layout.widget_pulse_player).apply {
                setTextViewText(
                    R.id.widget_title,
                    resolvedTitle.ifBlank { context.getString(R.string.widget_no_track) },
                )
                setTextViewText(
                    R.id.widget_artist,
                    resolvedArtist.ifBlank { context.getString(R.string.widget_tap_to_open) },
                )
                setImageViewResource(
                    R.id.widget_play_pause,
                    if (resolvedPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                )
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
                setOnClickPendingIntent(R.id.widget_previous, actionIntent(context, ACTION_PREVIOUS, 1))
                setOnClickPendingIntent(R.id.widget_play_pause, actionIntent(context, ACTION_PLAY_PAUSE, 2))
                setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT, 3))
            }
        }

        private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, PulsePlayerWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
