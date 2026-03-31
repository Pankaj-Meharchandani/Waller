/**
 * UpdateChecker
 *
 * - Silent GitHub release checker
 * - Runs once per app open
 * - Fetches latest version + release notes
 * - Calls back only when update is available
 *
 * Stable users  (e.g. "2.7")      → only prompted for stable releases
 * Beta users    (e.g. "2.7-beta") → prompted for stable AND newer beta releases
 *
 * Supported suffix separators: '-' or '.'
 * Examples: 2.7-beta, 2.7.beta, 2.7-rc1, 2.7.1-beta, 2.7.1-rc2
 */

package com.example.waller.ui.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    suspend fun check(
        currentVersion: String,
        repoOwner: String,
        repoName: String,
        onUpdateAvailable: (
            latestVersion: String,
            releaseNotes: String,
            releaseUrl: String
        ) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (isBetaVersion(currentVersion)) {
                    // Beta user: check all releases (stable + pre-release)
                    checkAllReleases(repoOwner, repoName, currentVersion, onUpdateAvailable)
                } else {
                    // Stable user: only check latest stable release
                    checkLatestStable(repoOwner, repoName, currentVersion, onUpdateAvailable)
                }
            } catch (_: Exception) {
                // Silent failure by design
            }
        }
    }

    // ── Stable path: /releases/latest ─────────────────────────────────────────

    private suspend fun checkLatestStable(
        repoOwner: String,
        repoName: String,
        currentVersion: String,
        onUpdateAvailable: (String, String, String) -> Unit
    ) {
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val json = JSONObject(URL(apiUrl).readText())

        val latestTag    = json.getString("tag_name").removePrefix("v")
        val releaseNotes = json.optString("body", "").trim()
        val releaseUrl   = json.getString("html_url")

        if (isNewer(latestTag, currentVersion)) {
            withContext(Dispatchers.Main) {
                onUpdateAvailable(latestTag, releaseNotes, releaseUrl)
            }
        }
    }

    // ── Beta path: /releases (all, including pre-releases) ────────────────────

    private suspend fun checkAllReleases(
        repoOwner: String,
        repoName: String,
        currentVersion: String,
        onUpdateAvailable: (String, String, String) -> Unit
    ) {
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=20"
        val releases = JSONArray(URL(apiUrl).readText())

        var bestTag   = ""
        var bestNotes = ""
        var bestUrl   = ""

        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            if (release.optBoolean("draft", false)) continue

            val tag   = release.getString("tag_name").removePrefix("v")
            val notes = release.optString("body", "").trim()
            val url   = release.getString("html_url")

            if (isNewer(tag, currentVersion)) {
                if (bestTag.isEmpty() || isNewer(tag, bestTag)) {
                    bestTag   = tag
                    bestNotes = notes
                    bestUrl   = url
                }
            }
        }

        if (bestTag.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                onUpdateAvailable(bestTag, bestNotes, bestUrl)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the version string contains a pre-release suffix.
     * Detects: 2.7-beta, 2.7.beta, 2.7-rc1, 2.7.1-alpha, etc.
     */
    private fun isBetaVersion(version: String): Boolean =
        version.contains('-') ||
                version.split(".").last().toIntOrNull() == null

    /**
     * Parses only the numeric parts of a version string,
     * stripping any pre-release suffix separated by '-' or '.'.
     *
     *   "2.7"       → [2, 7]
     *   "2.7-beta"  → [2, 7]
     *   "2.7.1"     → [2, 7, 1]
     *   "2.7.1-rc2" → [2, 7, 1]
     *   "2.7.beta"  → [2, 7]
     */
    private fun parseNumeric(version: String): List<Int> =
        version.split(".").mapNotNull { segment ->
            segment.split("-").first().trimEnd { !it.isDigit() }.toIntOrNull()
        }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseNumeric(latest)
        val c = parseNumeric(current)

        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}