package com.tesseractplay.floatoon.ads.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable

@Composable
fun AdLoadingDialogScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading Ad...", color = White)
        }
    }
}

fun showAdLoadingDialog(activity: Activity): Dialog {
    val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val composeView = ComposeView(activity).apply {
            setContent {
                AdLoadingDialogScreen()
            }
        }
        setContentView(composeView)
        setCancelable(false)
        show()
    }
    return dialog
}
