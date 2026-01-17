package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.content.Context
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER_HIDE_TOP_SHADOW
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.LauncherUtils.Companion.restartLauncher
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class TopShadow(context: Context) : ModPack(context) {

    private var removeTopShadow = false

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            removeTopShadow = getBoolean(LAUNCHER_HIDE_TOP_SHADOW, false)
        }
        restartLauncher(mContext)
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        val sysUiScrimClass =
            findClass("com.android.launcher3.graphics.SysUiScrim")

        sysUiScrimClass
            .hookMethod("draw")
            .runBefore { param ->
                if (!removeTopShadow) return@runBefore
                param.result = null
            }
    }
}
