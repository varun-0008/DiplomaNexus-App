package com.example.diplomanexus.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val TAG = "UpdateManager"

    /**
     * Download APK from [downloadUrl] and track progress via [onProgress] (0.0 to 1.0).
     * Returns the downloaded File or null if failed.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Resolve relative URLs against backend base URL if needed
            val fullUrl = if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                downloadUrl
            } else {
                "http://10.0.2.2:5000/${downloadUrl.removePrefix("/")}"
            }

            Log.d(TAG, "Downloading APK from: $fullUrl")

            val url = URL(fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}: ${connection.responseMessage}")
                return@withContext null
            }

            val fileLength = connection.contentLength
            val apkFile = File(context.cacheDir, "diplomanexus_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val input = connection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = total.toFloat() / fileLength.toFloat()
                    withContext(Dispatchers.Main) {
                        onProgress(progress.coerceIn(0f, 1f))
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            Log.d(TAG, "APK Downloaded successfully to ${apkFile.absolutePath}")
            return@withContext apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            return@withContext null
        }
    }

    /**
     * Check whether app has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Open settings page to allow installing unknown apps.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launch PackageInstaller intent to prompt the user to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch install intent", e)
        }
    }
}
