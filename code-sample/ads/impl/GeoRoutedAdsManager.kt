package com.tesseractplay.floatoon.ads.impl

import android.app.Activity
import android.content.Context
import com.tesseractplay.floatoon.ads.model.AdResult
import com.tesseractplay.floatoon.ads.model.AdType
import com.tesseractplay.floatoon.ads.AdsManager
import com.tesseractplay.floatoon.domain.repository.UserRepository
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoRoutedAdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adMobAdsManager: AdMobAdsManager,
    private val unityAdsManager: UnityAdsManager,
    private val userRepository: UserRepository
) : AdsManager {

    private var isPremiumUser = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.getPremiumState().collect { premium ->
                isPremiumUser = premium
            }
        }
    }

    private val activeManager: AdsManager by lazy {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val networkCountry = telephonyManager?.networkCountryIso

        val isRussia = if (!networkCountry.isNullOrBlank()) {
            networkCountry.equals("ru", ignoreCase = true)
        } else {
            Locale.getDefault().country.equals("RU", ignoreCase = true)
        }

        if (isRussia) {
            unityAdsManager
        } else {
            adMobAdsManager
        }
    }

    override fun initialize(context: Context) {
        activeManager.initialize(context)
    }

    override fun loadAd(type: AdType) {
        if (isPremiumUser) return
        activeManager.loadAd(type)
    }

    override suspend fun showAd(type: AdType, activity: Activity): AdResult {
        if (userRepository.getPremiumState().first()) {
            return AdResult.SUCCESS
        }
        return activeManager.showAd(type, activity)
    }
}
