package com.example.jetpack.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import java.lang.ref.WeakReference

/**
 * App-open ad on every cold start after the first launch.
 * The first-launch flag lives in app private preferences.
 */
internal object AppOpenAds : Application.ActivityLifecycleCallbacks {
    private const val TAG = "KreditAds"
    const val UNIT_ID = "R-M-20246529-1"
    private const val PREFS = "kredit_launch"
    private const val KEY_LAUNCHED = "launched_before"

    private var coldStartHandled = false
    private var ad: AppOpenAd? = null
    private var loader: AppOpenAdLoader? = null
    private var host: WeakReference<Activity>? = null

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        host = WeakReference(activity)
        if (coldStartHandled) {
            showIfReady()
            return
        }
        coldStartHandled = true
        if (!shouldShowOnThisLaunch(activity)) return
        loader = AppOpenAdLoader(activity.applicationContext)
        val request = AdRequest.Builder(UNIT_ID).build()
        loader?.loadAd(request, object : AppOpenAdLoadListener {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                ad = appOpenAd
                showIfReady()
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.v(TAG, error.description)
            }
        })
    }

    private fun shouldShowOnThisLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val launchedBefore = prefs.getBoolean(KEY_LAUNCHED, false)
        if (!launchedBefore) {
            prefs.edit().putBoolean(KEY_LAUNCHED, true).apply()
            return false
        }
        return true
    }

    private fun showIfReady() {
        val current = ad ?: return
        val activity = host?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        current.setAdEventListener(object : AppOpenAdEventListener {
            override fun onAdShown() = Unit

            override fun onAdFailedToShow(adError: AdError) {
                Log.v(TAG, adError.description)
                clear()
            }

            override fun onAdDismissed() {
                clear()
            }

            override fun onAdClicked() = Unit

            override fun onAdImpression(impressionData: ImpressionData?) = Unit
        })
        ad = null
        current.show(activity)
    }

    private fun clear() {
        ad?.setAdEventListener(null)
        ad = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (host?.get() === activity) host = null
    }
}
