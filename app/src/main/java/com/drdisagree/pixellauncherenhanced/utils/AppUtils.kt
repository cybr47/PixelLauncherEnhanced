package com.drdisagree.pixellauncherenhanced.utils

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.drdisagree.pixellauncherenhanced.PLEnhanced.Companion.appContext
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER3_PACKAGE
import com.drdisagree.pixellauncherenhanced.data.common.Constants.PIXEL_LAUNCHER_PACKAGE
import com.drdisagree.pixellauncherenhanced.data.model.AppInfoModel

object AppUtils {

    val isPixelLauncher = isAppInstalled(PIXEL_LAUNCHER_PACKAGE)
    val isLauncher3 = isAppInstalled(LAUNCHER3_PACKAGE)

    fun getAllLaunchableApps(): List<AppInfoModel> {
        val appList: MutableList<AppInfoModel> = ArrayList()
        val packageManager = appContext.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)

        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val appInfo = activityInfo.applicationInfo
            val packageName = appInfo.packageName

            packageManager.getLaunchIntentForPackage(packageName) ?: continue

            val appName = appInfo.loadLabel(packageManager).toString()
            val appIcon = appInfo.loadIcon(packageManager)
            val componentName = ComponentName(packageName, activityInfo.name)
            val app = AppInfoModel(appName, packageName, componentName, appIcon, false, packageName)

            appList.add(app)
        }

        appList.sortWith(compareBy<AppInfoModel> { it.appName.lowercase() }.thenBy { it.packageName.lowercase() })
        return appList
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            val pm = appContext.packageManager
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            pm.getApplicationInfo(packageName, 0).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}