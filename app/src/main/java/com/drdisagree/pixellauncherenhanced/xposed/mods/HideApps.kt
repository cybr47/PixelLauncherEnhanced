package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.drdisagree.pixellauncherenhanced.data.common.Constants.APP_BLOCK_LIST
import com.drdisagree.pixellauncherenhanced.data.common.Constants.SEARCH_HIDDEN_APPS
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.LauncherUtils.Companion.reloadLauncher
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.*
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Modifier
import java.util.Arrays

class HideApps(context: Context) : ModPack(context) {

    private var appBlockList: Set<String> = emptySet()
    private var searchHiddenApps = false

    private var activityAllAppsContainerViewInstance: Any? = null
    private var hotseatPredictionControllerInstance: Any? = null
    private var hybridHotseatOrganizerInstance: Any? = null
    private var predictionRowViewInstance: Any? = null

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            appBlockList = getStringSet(APP_BLOCK_LIST, emptySet())!!
            searchHiddenApps = getBoolean(SEARCH_HIDDEN_APPS, false)
        }

        if (key.firstOrNull() == APP_BLOCK_LIST) {
            updateLauncherIcons()
        }
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {

        val activityAllAppsContainerViewClass =
            findClass("com.android.launcher3.allapps.ActivityAllAppsContainerView")
        val hotseatPredictionControllerClass =
            findClass("com.android.launcher3.hybridhotseat.HotseatPredictionController")
        val hybridHotseatOrganizerClass =
            findClass("com.android.launcher3.util.HybridHotseatOrganizer", suppressError = true)
        val predictionRowViewClass =
            findClass("com.android.launcher3.appprediction.PredictionRowView")
        val alphabeticalAppsListClass =
            findClass("com.android.launcher3.allapps.AlphabeticalAppsList")
        val allAppsStoreClass =
            findClass("com.android.launcher3.allapps.AllAppsStore")
        val appInfoClass =
            findClass("com.android.launcher3.model.data.AppInfo")
        val defaultSearchClass =
            findClass(
                "com.android.launcher3.allapps.DefaultAppSearchAlgorithm",
                "com.android.launcher3.allapps.search.DefaultAppSearchAlgorithm"
            )
        val launcherModelClass =
            findClass("com.android.launcher3.LauncherModel")

        val baseModelUpdateTaskClass =
            findClass(
                "com.android.launcher3.model.BaseModelUpdateTask",
                suppressError = Build.VERSION.SDK_INT >= 36
            )

        activityAllAppsContainerViewClass.hookConstructor().runAfter {
            activityAllAppsContainerViewInstance = it.thisObject
        }

        hotseatPredictionControllerClass.hookConstructor().runAfter {
            hotseatPredictionControllerInstance = it.thisObject
        }

        hybridHotseatOrganizerClass?.hookConstructor()?.runAfter {
            hybridHotseatOrganizerInstance = it.thisObject
        }

        predictionRowViewClass.hookConstructor().runAfter {
            predictionRowViewInstance = it.thisObject
        }

        allAppsStoreClass
            .hookMethod("setApps")
            .runAfter { param ->
                param.thisObject.setExtraField("mAppsBackup", param.args[0])
            }

        allAppsStoreClass
            .hookMethod("getApp")
            .runBefore { param ->
                val componentKey = param.args[0]
                val comparator = if (param.args.size > 1) {
                    param.args[1]
                } else {
                    appInfoClass.getStaticField("COMPONENT_KEY_COMPARATOR")
                } as Comparator<Any?>

                val apps =
                    param.thisObject.getExtraFieldSilently("mAppsBackup") as? Array<*> ?: return@runBefore

                val componentName =
                    componentKey.getFieldSilently("componentName") as? ComponentName
                val user = componentKey.getFieldSilently("user")

                val tempInfo = param.thisObject.getField("mTempInfo").apply {
                    setField("componentName", componentName)
                    setField("user", user)
                }

                val index = Arrays.binarySearch(apps, tempInfo, comparator)
                param.result = if (index >= 0) apps[index] else null
            }

        alphabeticalAppsListClass
            .hookMethod("onAppsUpdated")
            .runAfter { param ->
                val items =
                    (param.thisObject.getField("mAdapterItems") as ArrayList<*>).toMutableList()

                val iterator = items.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    val pkg =
                        item.getFieldSilently("itemInfo")
                            ?.getComponentName()
                            ?.packageName

                    if (matchesBlocklist(pkg)) {
                        iterator.remove()
                    }
                }

                param.thisObject.setField("mAdapterItems", ArrayList(items))
            }

        defaultSearchClass
            .hookMethod("getTitleMatchResult")
            .runBefore { param ->
                if (searchHiddenApps) return@runBefore

                val index = if (param.args[0] is Context) 1 else 0
                val list = (param.args[index] as List<*>).toMutableList()

                list.removeIf {
                    matchesBlocklist(it.getComponentName()?.packageName)
                }

                param.args[index] = list
            }

        launcherModelClass
            .hookMethod("enqueueModelUpdateTask")
            .runBefore { param ->
                if (searchHiddenApps) return@runBefore

                val task = param.args[0]
                if (baseModelUpdateTaskClass == null ||
                    task::class.java.name != baseModelUpdateTaskClass.name
                ) return@runBefore

                task::class.java
                    .hookMethod("execute")
                    .runBefore { p ->
                        val apps =
                            p.thisObject.getFieldSilently("mApps") ?: return@runBefore
                        val data =
                            apps.getFieldSilently("data") as? ArrayList<*> ?: return@runBefore

                        data.removeIf {
                            matchesBlocklist(it.getComponentName()?.packageName)
                        }

                        apps.setField("data", data)
                    }
            }

        predictionRowViewClass
            .hookMethod("applyPredictionApps")
            .runBefore { param ->
                val list =
                    (param.thisObject.getField("mPredictedApps") as ArrayList<*>).toMutableList()

                list.removeIf {
                    matchesBlocklist(it.getComponentName()?.packageName)
                }

                param.thisObject.setField("mPredictedApps", ArrayList(list))
            }
    }

    private fun matchesBlocklist(pkg: String?): Boolean {
        return !pkg.isNullOrEmpty() && appBlockList.contains(pkg)
    }

    private fun Any?.getComponentName(): ComponentName? {
        return getFieldSilently("componentName") as? ComponentName
            ?: getFieldSilently("mComponentName") as? ComponentName
            ?: runCatching { callMethod("getTargetComponent") as ComponentName }.getOrNull()
    }

    private fun updateLauncherIcons() {
        activityAllAppsContainerViewInstance?.callMethod("onAppsUpdated")
        hotseatPredictionControllerInstance?.callMethodSilently("fillGapsWithPrediction", true)
        hybridHotseatOrganizerInstance?.callMethodSilently("fillGapsWithPrediction", true)
        predictionRowViewInstance?.callMethod("applyPredictionApps")
        reloadLauncher(mContext)
    }
}
