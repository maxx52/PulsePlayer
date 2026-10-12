package ru.maxx52.pulseplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yandex.mobile.ads.common.YandexAds
import ru.maxx52.pulseplayer.ui.PulseApp
import ru.maxx52.pulseplayer.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adPreferences = getSharedPreferences("pulse_ads", MODE_PRIVATE)
        YandexAds.setUserConsent(adPreferences.getBoolean("personalized_ads", false))
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(false)
        YandexAds.initialize(this) { }
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                val playerViewModel: MainViewModel = viewModel()
                var showAdConsent by remember {
                    mutableStateOf(!adPreferences.getBoolean("choice_saved", false))
                }
                val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                var permissionGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            audioPermission,
                        ) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    permissionGranted = granted
                    if (granted) playerViewModel.loadLibrary()
                }
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(permissionGranted) {
                    if (permissionGranted) {
                        playerViewModel.loadLibrary()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            val prompts = getSharedPreferences("pulse_prompts", MODE_PRIVATE)
                            if (!prompts.getBoolean("notification_requested", false)) {
                                prompts.edit().putBoolean("notification_requested", true).apply()
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }

                PulseApp(
                    viewModel = playerViewModel,
                    permissionGranted = permissionGranted,
                    requestPermission = { permissionLauncher.launch(audioPermission) },
                    showAdConsent = showAdConsent,
                    requestAdSettings = { showAdConsent = true },
                    saveAdConsent = { personalized ->
                        adPreferences.edit()
                            .putBoolean("choice_saved", true)
                            .putBoolean("personalized_ads", personalized)
                            .apply()
                        YandexAds.setUserConsent(personalized)
                        showAdConsent = false
                    },
                )
            }
        }
    }
}
