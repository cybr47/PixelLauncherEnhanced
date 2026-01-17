package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER_HIDE_TOP_SHADOW
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class TopShadow(context: Context) : ModPack(context) {

    private var hideTopShadow = false

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            hideTopShadow = getBoolean(LAUNCHER_HIDE_TOP_SHADOW, false)
        }
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {

        val allAppsStateClass =
            findClass("com.android.launcher3.uioverrides.states.AllAppsState")

        allAppsStateClass
            .hookMethod("getWorkspaceScrimColor")
            .runAfter { param ->
                if (!hideTopShadow) return@runAfter

                param.result = when (val res = param.result) {
                    is Int -> Color.TRANSPARENT
                    else -> res // fallback safety
                }
            }
    }
}
