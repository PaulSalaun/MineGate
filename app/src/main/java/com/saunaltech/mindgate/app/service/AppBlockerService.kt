package com.saunaltech.mindgate.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences

class AppBlockerService : AccessibilityService() {

    companion object {
        val unlockedSessions = mutableSetOf<String>()
    }

    private val prefs by lazy { MindGatePreferences(applicationContext) }
    private var currentForegroundPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (isIgnoredPackage(packageName)) return
        if (packageName == currentForegroundPackage) return

        val previous = currentForegroundPackage
        currentForegroundPackage = packageName

        previous?.let { unlockedSessions.remove(it) }

        if (packageName !in prefs.loadBlockedApps()) return
        if (packageName in unlockedSessions) return

        launchOverlay(packageName)
    }

    private fun isIgnoredPackage(packageName: String): Boolean {
        return packageName == applicationContext.packageName
                || packageName == "android"
                || packageName == "com.android.systemui"
    }

    private fun launchOverlay(packageName: String) {
        startService(Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.Companion.EXTRA_PACKAGE_NAME, packageName)
        })
    }

    override fun onInterrupt() {}
}