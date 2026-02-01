package com.example.ai_productivityenhancer

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar

class AIBlockingWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val usageLimitsPrefs = applicationContext.getSharedPreferences("usage_limits", Context.MODE_PRIVATE)
        val blockedSet = sharedPrefs.getStringSet("blocked_pkg_names", emptySet())?.toMutableSet() ?: mutableSetOf()

        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            getStartTime(),
            System.currentTimeMillis()
        )

        val manualOverrides = applicationContext.getSharedPreferences("manual_overrides", Context.MODE_PRIVATE)
        val manualBlocked = manualOverrides.all.keys

        TargetedApps.apps.forEach { appInfo ->
            if (appInfo.packageName !in manualBlocked) {
                val usageLimit = usageLimitsPrefs.getFloat(appInfo.packageName, 2f) * 60 * 60 * 1000
                val usage = stats.firstOrNull { it.packageName == appInfo.packageName }?.totalTimeInForeground ?: 0L
                if (usage > usageLimit) {
                    blockedSet.add(appInfo.packageName)
                }
            }
        }

        sharedPrefs.edit().putStringSet("blocked_pkg_names", blockedSet).apply()

        return Result.success()
    }

    private fun getStartTime(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
