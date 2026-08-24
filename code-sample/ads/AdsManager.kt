package com.tesseractplay.floatoon.ads

import android.app.Activity
import android.content.Context
import com.tesseractplay.floatoon.ads.model.AdResult
import com.tesseractplay.floatoon.ads.model.AdType

interface AdsManager {
    fun initialize(context: Context)
    
    fun loadAd(type: AdType)
    
    suspend fun showAd(type: AdType, activity: Activity): AdResult
}
