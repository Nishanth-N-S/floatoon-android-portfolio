package com.tesseractplay.floatoon.analytics.impl

import android.util.Log
import com.tesseractplay.floatoon.analytics.api.AnalyticsEvent
import com.tesseractplay.floatoon.analytics.api.AnalyticsLogger
import com.tesseractplay.floatoon.analytics.api.UserProperty
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugAnalyticsLogger @Inject constructor() : AnalyticsLogger {
    private val TAG = "Analytics"

    override fun logEvent(event: AnalyticsEvent) {
        Log.d(TAG, "Event: ${event.name}, Params: ${event.params}")
    }

    override fun setUserProperty(property: UserProperty) {
        Log.d(TAG, "User Property - Key: ${property.key}, Value: ${property.value}")
    }

    override fun setUserId(userId: String?) {
        Log.d(TAG, "User ID: $userId")
    }
}