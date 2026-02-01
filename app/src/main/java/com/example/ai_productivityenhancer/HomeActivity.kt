package com.example.ai_productivityenhancer

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

data class AppInfo(val name: String, val packageName: String)

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userName =
            FirebaseAuth.getInstance().currentUser?.displayName ?: "User"

        val sharedPrefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val appSettings = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setContent {
            val manualOverrides = getSharedPreferences("manual_overrides", Context.MODE_PRIVATE)
            val blockedSet = sharedPrefs.getStringSet("blocked_pkg_names", emptySet()) ?: emptySet()
            val manualBlocked = manualOverrides.all.mapValues { it.value as Boolean }

            val blockStates = remember {
                mutableStateMapOf<String, Boolean>().apply {
                    TargetedApps.apps.forEach { appInfo ->
                        val packageName = appInfo.packageName
                        this[packageName] = manualBlocked[packageName] ?: (packageName in blockedSet)
                    }
                }
            }

            var isAutoMode by remember { mutableStateOf(appSettings.getBoolean("is_auto_mode", false)) }

            Box(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    var isServiceEnabled by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            isServiceEnabled = isAccessibilityServiceEnabled(this@HomeActivity, MyAccessibilityService::class.java)
                            kotlinx.coroutines.delay(1000)
                        }
                    }

                    if (!isServiceEnabled) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Accessibility Service Not Enabled",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "The app blocking feature requires the accessibility service to be enabled. Please enable it in your device settings.",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = "Welcome, $userName",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("Enable Accessibility Service")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manual Mode", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAutoMode,
                            onCheckedChange = {
                                isAutoMode = it
                                appSettings.edit().putBoolean("is_auto_mode", it).apply()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto Mode", fontWeight = FontWeight.Bold)
                    }

                    LaunchedEffect(isAutoMode) {
                        val workManager = WorkManager.getInstance(this@HomeActivity)
                        if (isAutoMode) {
                            val periodicWorkRequest = PeriodicWorkRequestBuilder<AIBlockingWorker>(1, TimeUnit.HOURS).build()
                            workManager.enqueueUniquePeriodicWork(
                                "AIBlockingWorker",
                                ExistingPeriodicWorkPolicy.REPLACE,
                                periodicWorkRequest
                            )
                        } else {
                            workManager.cancelUniqueWork("AIBlockingWorker")
                            sharedPrefs.edit().putStringSet("blocked_pkg_names", emptySet()).apply()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAutoMode) {
                        AutoModeScreen()
                    } else {
                        LazyColumn {
                            items(TargetedApps.apps) { appInfo ->
                                val usageMs = remember {
                                    mutableStateOf(getAppUsageToday(appInfo.packageName))
                                }

                                LaunchedEffect(Unit) {
                                    while (true) {
                                        usageMs.value = getAppUsageToday(appInfo.packageName)
                                        kotlinx.coroutines.delay(1000)
                                    }
                                }

                                AppBlockCard(
                                    appName = appInfo.name,
                                    isBlocked = blockStates[appInfo.packageName] == true,
                                    usageMs = usageMs.value,
                                    onBlock = {
                                        blockStates[appInfo.packageName] = true
                                        manualOverrides.edit().putBoolean(appInfo.packageName, true).apply()
                                        updateBlockedSet(sharedPrefs, blockStates)
                                    },
                                    onUnblock = {
                                        blockStates[appInfo.packageName] = false
                                        manualOverrides.edit().remove(appInfo.packageName).apply()
                                        updateBlockedSet(sharedPrefs, blockStates)
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getAppUsageToday(packageName: String): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats.firstOrNull {
            it.packageName == packageName
        }?.totalTimeInForeground ?: 0L
    }

    private fun updateBlockedSet(sharedPrefs: android.content.SharedPreferences, blockStates: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>) {
        val newBlockedSet = blockStates.entries
            .filter { it.value }
            .map { it.key }
            .toSet()
        sharedPrefs.edit()
            .putStringSet("blocked_pkg_names", newBlockedSet)
            .apply()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(service.name)) {
                return true
            }
        }
        return false
    }
}

@Composable
fun AutoModeScreen() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    val workInfo by workManager.getWorkInfosForUniqueWorkLiveData("AIBlockingWorker").observeAsState(null)

    val isAnalyzing = workInfo?.any { it.state == WorkInfo.State.RUNNING } == true

    Column {
        if (isAnalyzing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI is analyzing...")
            }
        }

        val usageLimitsPrefs = context.getSharedPreferences("usage_limits", Context.MODE_PRIVATE)

        val appLimits = remember {
            mutableStateMapOf<String, Float>().apply {
                TargetedApps.apps.forEach { appInfo ->
                    this[appInfo.packageName] = usageLimitsPrefs.getFloat(appInfo.packageName, 2f)
                }
            }
        }

        LazyColumn {
            item {
                Text(
                    "Usage Limits",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(TargetedApps.apps) { appInfo ->
                UsageLimitCard(
                    appName = appInfo.name,
                    limit = appLimits[appInfo.packageName] ?: 2f,
                    onLimitChange = {
                        appLimits[appInfo.packageName] = it
                        usageLimitsPrefs.edit().putFloat(appInfo.packageName, it).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun UsageLimitCard(appName: String, limit: Float, onLimitChange: (Float) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$appName Usage Limit: ${"%.1f".format(limit)} hours", fontWeight = FontWeight.Bold)
            Slider(
                value = limit,
                onValueChange = onLimitChange,
                valueRange = 0f..12f,
                steps = 23
            )
        }
    }
}

@Composable
fun AppBlockCard(
    appName: String,
    isBlocked: Boolean,
    usageMs: Long,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    val duration = usageMs.milliseconds
    val hours = duration.inWholeHours
    val minutes = duration.inWholeMinutes % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "Usage Today: ${hours}h ${minutes}m",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isBlocked) {
                Button(
                    onClick = onUnblock,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Unblock")
                }
            } else {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Block")
                }
            }
        }
    }
}
