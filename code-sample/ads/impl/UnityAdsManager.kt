package com.tesseractplay.floatoon.ads.impl

import android.app.Activity
import android.content.Context
import com.tesseractplay.floatoon.BuildConfig
import com.tesseractplay.floatoon.ads.model.AdResult
import com.tesseractplay.floatoon.ads.model.AdType
import com.tesseractplay.floatoon.ads.AdsManager
import com.tesseractplay.floatoon.ads.model.InternalAdState
import com.tesseractplay.floatoon.ads.ui.showAdLoadingDialog
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
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
class UnityAdsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AdsManager, IUnityAdsInitializationListener {

    private val adStates = mutableMapOf<AdType, InternalAdState>()
    private val rewardedLoadCallbacks = mutableListOf<(Boolean) -> Unit>()

    init {
        AdType.entries.forEach {
            adStates[it] = InternalAdState.IDLE
        }
    }

    override fun initialize(context: Context) {
        if (!UnityAds.isInitialized) {
            UnityAds.initialize(context, BuildConfig.UNITY_GAME_ID, BuildConfig.DEBUG, this)
        }
    }

    override fun onInitializationComplete() {
    }

    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
    }

    private fun getAdUnitIdForType(type: AdType): String? {
        return when (type) {
            AdType.INTERSTITIAL -> BuildConfig.UNITY_AD_UNIT_INTERSTITIAL
            AdType.REWARDED -> BuildConfig.UNITY_AD_UNIT_REWARDED
        }
    }

    override fun loadAd(type: AdType) {
        val currentState = adStates[type] ?: InternalAdState.IDLE
        if (currentState == InternalAdState.LOADING || currentState == InternalAdState.LOADED) {
            return
        }

        val adUnitId = getAdUnitIdForType(type)
        if (adUnitId == null) {
            adStates[type] = InternalAdState.IDLE
            return
        }

        adStates[type] = InternalAdState.LOADING

        UnityAds.load(adUnitId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                adStates[type] = InternalAdState.LOADED
                if (type == AdType.REWARDED) {
                    val callbacks = rewardedLoadCallbacks.toList()
                    rewardedLoadCallbacks.clear()
                    callbacks.forEach { it(true) }
                }
            }

            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                adStates[type] = InternalAdState.IDLE
                if (type == AdType.REWARDED) {
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
                } else if (type == AdType.INTERSTITIAL) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000.milliseconds)
                        if (adStates[type] == InternalAdState.IDLE) {
                            loadAd(type)
                        }
                    }
                }
            }
        })
    }

    override suspend fun showAd(type: AdType, activity: Activity): AdResult {
        return suspendCancellableCoroutine { continuation ->
            when (type) {
                AdType.INTERSTITIAL -> {
                    if (adStates[type] == InternalAdState.LOADED) {
                        showUnityAd(type, activity) { _ ->
                            if (continuation.isActive) continuation.resume(AdResult.SUCCESS)
                        }
                    } else {
                        if (continuation.isActive) continuation.resume(AdResult.SUCCESS)
                    }
                }
                AdType.REWARDED -> {
                    if (adStates[type] == InternalAdState.LOADED) {
                        showUnityAd(type, activity) { result ->
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
                                if (success) {
                                    showUnityAd(type, activity) { result ->
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

    private fun showUnityAd(type: AdType, activity: Activity, onComplete: (AdResult) -> Unit) {
        val adUnitId = getAdUnitIdForType(type)
        if (adUnitId == null) {
            onComplete(AdResult.FAILURE)
            return
        }

        UnityAds.show(activity, adUnitId, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                adStates[type] = InternalAdState.IDLE
                onComplete(AdResult.FAILURE)
            }

            override fun onUnityAdsShowStart(placementId: String) {
            }

            override fun onUnityAdsShowClick(placementId: String) {
            }

            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                adStates[type] = InternalAdState.IDLE
                if (type == AdType.REWARDED && state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onComplete(AdResult.SUCCESS)
                } else {
                    onComplete(if (type == AdType.INTERSTITIAL) AdResult.SUCCESS else AdResult.FAILURE)
                }
            }
        })
    }
}
