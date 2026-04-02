package com.drdisagree.pixellauncherenhanced.data.config

import android.content.ComponentName
import android.content.SharedPreferences

object AppLabelPreferences {

    private const val CUSTOM_LABEL_KEY_PREFIX = "xposed_custom_label_"

    fun getCustomLabelKey(componentName: ComponentName): String {
        return "$CUSTOM_LABEL_KEY_PREFIX${componentName.flattenToString()}"
    }

    fun getCustomLabel(prefs: SharedPreferences, componentName: ComponentName): String? {
        return prefs.getString(getCustomLabelKey(componentName), null)
    }

    fun setCustomLabel(editor: SharedPreferences.Editor, componentName: ComponentName, label: String) {
        editor.putString(getCustomLabelKey(componentName), label).apply()
    }

    fun removeCustomLabel(editor: SharedPreferences.Editor, componentName: ComponentName) {
        editor.remove(getCustomLabelKey(componentName)).apply()
    }
}
