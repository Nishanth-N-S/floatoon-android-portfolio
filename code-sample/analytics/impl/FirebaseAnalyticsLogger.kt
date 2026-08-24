package com.tesseractplay.floatoon.analytics.impl

import android.os.Bundle
import com.tesseractplay.floatoon.analytics.api.AnalyticsEvent
import com.tesseractplay.floatoon.analytics.api.AnalyticsLogger
import com.tesseractplay.floatoon.analytics.api.UserProperty
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsLogger {

    override fun logEvent(event: AnalyticsEvent) {
        val bundle = Bundle().apply {
            event.params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(event.name, bundle)
    }

    override fun setUserProperty(property: UserProperty) {
        firebaseAnalytics.setUserProperty(property.key, property.value)
    }

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
}
