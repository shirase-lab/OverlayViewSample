package com.shiraselab.overlayviewsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // デバイスの起動後またはアプリの起動時にサービスを開始
        if (Settings.canDrawOverlays(context)) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                val serviceIntent = Intent(context, OverlayService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)  // フォアグラウンドサービスを開始
            }
        }
    }
}
