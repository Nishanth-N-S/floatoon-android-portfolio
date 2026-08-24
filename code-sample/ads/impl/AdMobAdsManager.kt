package com.tesseractplay.floatoon.ads.impl

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.tesseractplay.floatoon.BuildConfig
import com.tesseractplay.floatoon.ads.model.AdResult
import com.tesseractplay.floatoon.ads.model.AdType
import com.tesseractplay.floatoon.ads.AdsManager
import com.tesseractplay.floatoon.ads.model.InternalAdState
import com.tesseractplay.floatoon.ads.ui.showAdLoadingDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class AdMobAdsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AdsManager {


    private val adStates = mutableMapOf<AdType, InternalAdState>()
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private val rewardedLoadCallbacks = mutableListOf<(Boolean) -> Unit>()

    init {
        AdType.entries.forEach {
            adStates[it] = InternalAdState.IDLE
        }
    }

    override fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    private fun getAdUnitIdForType(type: AdType): String {
        return when (type) {
            AdType.INTERSTITIAL -> BuildConfig.AD_UNIT_INTERSTITIAL
            AdType.REWARDED -> BuildConfig.AD_UNIT_REWARDED
        }
    }

    override fun loadAd(type: AdType) {
        val currentState = adStates[type] ?: InternalAdState.IDLE
        if (currentState == InternalAdState.LOADING || currentState == InternalAdState.LOADED) {
            return
        }

        adStates[type] = InternalAdState.LOADING
        val adRequest = AdRequest.Builder().build()
        val resolvedAdUnitId = getAdUnitIdForType(type)

        when (type) {
            AdType.INTERSTITIAL -> {
                InterstitialAd.load(
                    context,
                    resolvedAdUnitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            interstitialAd = null
                            adStates[type] = InternalAdState.IDLE
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000.milliseconds)
                                if (adStates[type] == InternalAdState.IDLE) {
                                    loadAd(type)
                                }
                            }
                        }

                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            adStates[type] = InternalAdState.LOADED
                        }
                    }
                )
            }
            AdType.REWARDED -> {
                RewardedAd.load(
                    context,
                    resolvedAdUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            rewardedAd = null
                            adStates[type] = InternalAdState.IDLE
                            
                            val callbacks = rewardedLoadCallbacks.toList()
                            rewardedLoadCallbacks.clear()
                            callbacks.forEach { it(false) }

                            if (callbacks.isEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(3000.milliseconds)
                                    if (adStates[type] == InternalAdState.IDLE) {
                                        loadAd(type)
                                    }
                                }
                            }
                        }

                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            adStates[type] = InternalAdState.LOADED
                            
                            val callbacks = rewardedLoadCallbacks.toList()
                            rewardedLoadCallbacks.clear()
                            callbacks.forEach { it(true) }
                        }
                    }
                )
            }
        }
    }

    override suspend fun showAd(type: AdType, activity: Activity): AdResult {
        return suspendCancellableCoroutine { continuation ->
            when (type) {
                AdType.INTERSTITIAL -> {
                    if (adStates[type] == InternalAdState.LOADED && interstitialAd != null) {
                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                adStates[type] = InternalAdState.IDLE
                                if (continuation.isActive) continuation.resume(AdResult.SUCCESS)
                            }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                interstitialAd = null
                                adStates[type] = InternalAdState.IDLE
                                if (continuation.isActive) continuation.resume(AdResult.SUCCESS)
                            }
                        }
                        interstitialAd?.show(activity)
                    } else {
                        if (continuation.isActive) continuation.resume(AdResult.SUCCESS)
                    }
                }
                AdType.REWARDED -> {
                    if (adStates[type] == InternalAdState.LOADED && rewardedAd != null) {
                        showRewardedAd(activity) { result ->
                            if (continuation.isActive) continuation.resume(result)
                        }
                    } else {
                        val dialog = showAdLoadingDialog(activity)
                        rewardedLoadCallbacks.add { success ->
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    dialog.dismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (success && rewardedAd != null) {
                                    showRewardedAd(activity) { result ->
                                        if (continuation.isActive) continuation.resume(result)
                                    }
                                } else {
                                    if (continuation.isActive) continuation.resume(AdResult.FAILURE)
                                }
                            }
                        }
                        if (adStates[type] != InternalAdState.LOADING) {
                            loadAd(type)
                        }
                    }
                }
            }
        }
    }

    private fun showRewardedAd(activity: Activity, onComplete: (AdResult) -> Unit) {
        var rewarded = false
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                adStates[AdType.REWARDED] = InternalAdState.IDLE
                onComplete(if (rewarded) AdResult.SUCCESS else AdResult.FAILURE)
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                adStates[AdType.REWARDED] = InternalAdState.IDLE
                onComplete(AdResult.FAILURE)
            }
        }
        rewardedAd?.show(activity) {
            rewarded = true
        }
    }
}
