package com.tesseractplay.floatoon.analytics.api

sealed interface UserProperty {
    val key: String
    val value: String
}

data class MockProperty1(val value: String){
    override val key: String = "mock_property_1"
    override val key: String = value
}
