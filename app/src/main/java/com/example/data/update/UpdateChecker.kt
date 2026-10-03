package com.example.data.update

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val htmlUrl: String,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {

    suspend fun checkForUpdates(repoPath: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val cleanRepo = repoPath.trim().removePrefix("https://github.com/").removeSuffix("/")
        if (!cleanRepo.contains("/") || cleanRepo.split("/").size != 2) {
            return@withContext UpdateCheckResult.Error("Geçersiz GitHub deposu formatı. (Örnek: kullanici/repo)")
        }

        val apiUrl = "https://api.github.com/repos/$cleanRepo/releases/latest"
        var connection: HttpURLConnection? = null

        try {
            val url = URL(apiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "20-20-20-EyeCare-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext UpdateCheckResult.Error(
                    "Bu depoda henüz yayınlanmış bir GitHub Sürümü (Release) bulunamadı. Lütfen GitHub'da 'Releases' bölümünden bir sürüm yayınlayın."
                )
            } else if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error("GitHub API hatası: HTTP $responseCode")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseText = reader.use { it.readText() }
            val json = JSONObject(responseText)

            val rawTag = json.optString("tag_name", "").trim()
            val latestVersion = rawTag.removePrefix("v").removePrefix("V")
            val releaseTitle = json.optString("name", "Yeni Sürüm: $rawTag")
            val releaseNotes = json.optString("body", "Yeni özellikler ve hata düzeltmeleri.")
            val htmlUrl = json.optString("html_url", "https://github.com/$cleanRepo/releases")
            val publishedAt = json.optString("published_at", "")

            // Look for APK download URL in assets
            var apkDownloadUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            // Fallback: if no attached asset, fallback to html release page
            val finalDownloadUrl = apkDownloadUrl ?: htmlUrl

            val currentVersion = BuildConfig.VERSION_NAME

            if (isNewerVersion(latestVersion, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    UpdateInfo(
                        currentVersion = currentVersion,
                        latestVersion = latestVersion,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        downloadUrl = finalDownloadUrl,
                        htmlUrl = htmlUrl,
                        publishedAt = publishedAt
                    )
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion = currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Güncelleme kontrolü başarısız oldu: ${e.localizedMessage ?: "Bağlantı hatası"}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares version strings like "1.1.0" > "1.0.0" or "2.0" > "1.9.5"
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        try {
            val remoteParts = remote.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val localParts = local.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        } catch (_: Exception) {
            return remote != local
        }
    }
}
