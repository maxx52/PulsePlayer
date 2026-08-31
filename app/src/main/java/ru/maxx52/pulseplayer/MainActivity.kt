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
import ru.maxx52.pulseplayer.ui.PulseApp
import ru.maxx52.pulseplayer.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                val playerViewModel: MainViewModel = viewModel()
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

                LaunchedEffect(permissionGranted) {
                    if (permissionGranted) playerViewModel.loadLibrary()
                }

                PulseApp(
                    viewModel = playerViewModel,
                    permissionGranted = permissionGranted,
                    requestPermission = { permissionLauncher.launch(audioPermission) },
                )
            }
        }
    }
}
