package com.drdisagree.pixellauncherenhanced.data.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfoModel(
    val appName: String,
    val packageName: String,
    val componentName: ComponentName?,
    val appIcon: Drawable,
    var isSelected: Boolean,
    var subtitle: String? = null
)