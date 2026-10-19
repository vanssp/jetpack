package com.example.jetpack

import android.app.Application
import com.example.jetpack.ads.AppOpenAds
import com.yandex.mobile.ads.common.YandexAds

class KreditApp : Application() {
    override fun onCreate() {
        super.onCreate()
        YandexAds.initialize(this) {}
        AppOpenAds.install(this)
    }
}
