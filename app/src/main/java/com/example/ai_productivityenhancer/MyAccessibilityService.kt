package com.example.ai_productivityenhancer

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null) {
                if (pkg == applicationContext.packageName) {
                    return
                }
                val sharedPrefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                val blockedSet = sharedPrefs.getStringSet("blocked_pkg_names", emptySet()) ?: emptySet()
                Log.d("MyAccessibilityService", "Event for package: $pkg")
                Log.d("MyAccessibilityService", "Blocked set: $blockedSet")

                if (pkg in blockedSet) {
                    Log.d("MyAccessibilityService", "Blocking package: $pkg")
                    val intent = Intent(this, BlockingActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("blocked_app_pkg", pkg)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
