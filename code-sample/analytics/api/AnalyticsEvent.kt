package com.tesseractplay.floatoon.analytics.api

sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any>
}

data class MockEvent1(val mockParam: String) : AnalyticsEvent {
    override val name: String = "mock_event1"
    override val params: Map<String, Any> = mapOf("mock_param" to mockParam)
}

data class MockEvent2(val mockParam: String) : AnalyticsEvent {
    override val name: String = "mock_event2"
    override val params: Map<String, Any> = mapOf("mock_param" to mockParam)
}

data class MockEvent3(val mockParam: String) : AnalyticsEvent {
    override val name: String = "mock_event3"
    override val params: Map<String, Any> = mapOf("mock_param" to mockParam)
}
