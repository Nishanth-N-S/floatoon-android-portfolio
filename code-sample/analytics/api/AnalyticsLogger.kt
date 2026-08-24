package com.tesseractplay.floatoon.analytics.api

interface AnalyticsLogger {
    fun logEvent(event: AnalyticsEvent)
    fun setUserProperty(property: UserProperty)
    fun setUserId(userId: String?)
}
