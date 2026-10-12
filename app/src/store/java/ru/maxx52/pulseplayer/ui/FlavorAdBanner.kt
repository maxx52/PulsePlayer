package ru.maxx52.pulseplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import ru.maxx52.pulseplayer.BuildConfig

@Composable
fun FlavorAdBanner(modifier: Modifier = Modifier) {
    val adUnitId = BuildConfig.YANDEX_BANNER_ID
    if (adUnitId.isBlank()) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var loaded by remember(adUnitId, widthDp) { mutableStateOf(false) }
    val adSize = remember(context, widthDp) { BannerAdSize.sticky(context, widthDp) }
    val banner = remember(adUnitId, widthDp) {
        BannerAdView(context).apply {
            setAdSize(adSize)
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    loaded = true
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    loaded = false
                }

                override fun onAdClicked() = Unit

                override fun onImpression(impressionData: ImpressionData?) = Unit
            })
            loadAd(AdRequest.Builder(adUnitId).build())
        }
    }

    DisposableEffect(banner) {
        onDispose { banner.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (loaded) (adSize.getHeightInPixels(context) / context.resources.displayMetrics.density).dp else 1.dp),
    ) {
        AndroidView(
            factory = { banner },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
