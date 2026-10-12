package ru.maxx52.pulseplayer.ads

import android.content.Context
import com.yandex.mobile.ads.common.YandexAds

object PulseAds {
    fun initialize(context: Context, personalized: Boolean) {
        YandexAds.setUserConsent(personalized)
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(false)
        YandexAds.initialize(context) { }
    }
    fun setUserConsent(personalized: Boolean) {
        YandexAds.setUserConsent(personalized)
    }
}
