/**
 * UpdateChecker
 *
 * - Silent GitHub release checker
 * - Runs once per app open
 * - Compares current app version with latest release tag
 * - Calls back only when update is available
 */

package com.example.waller.ui.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    suspend fun check(
        currentVersion: String,
        repoOwner: String,
        repoName: String,
        onUpdateAvailable: (latestVersion: String, releaseUrl: String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val apiUrl =
                    "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

                val response = URL(apiUrl).readText()
                val json = JSONObject(response)

                val latestTag = json.getString("tag_name").removePrefix("v")
                val releaseUrl = json.getString("html_url")

                if (isNewer(latestTag, currentVersion)) {
                    withContext(Dispatchers.Main) {
                        onUpdateAvailable(latestTag, releaseUrl)
                    }
                }
            } catch (_: Exception) {
                // Silent failure by design
            }
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
