package com.kien.bubble

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kien.bubble.service.BubbleForegroundService

object BubbleManager {
    @JvmStatic fun startBubble(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, BubbleForegroundService::class.java))
            } else {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        } else {
            context.startService(Intent(context, BubbleForegroundService::class.java))
        }
    }
}