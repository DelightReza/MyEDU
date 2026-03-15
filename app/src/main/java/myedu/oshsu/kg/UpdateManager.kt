package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class UpdateManager {

    suspend fun checkForUpdate(appContext: Context): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = appContext.getString(R.string.update_repo_path)
            val release = NetworkClient.githubApi.getLatestRelease(apiUrl)
            val currentVer = BuildConfig.VERSION_NAME
            val remoteVer = release.tagName.replace("v", "")
            val localVer = currentVer.replace("v", "")
            if (remoteVer != localVer && isNewerVersion(remoteVer, localVer)) release else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val lParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            val length = maxOf(rParts.size, lParts.size)
            for (i in 0 until length) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    fun startDownload(context: Context, release: GitHubRelease): Long? {
        val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: return null
        return try {
            val url = apkAsset.downloadUrl
            val fileName = apkAsset.name
            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.update_notif_title))
                .setDescription(context.getString(R.string.update_notif_desc, release.tagName))
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType(AppConstants.MIME_APK)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            manager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.update_error_download, e.message), Toast.LENGTH_LONG).show()
            null
        }
    }

    data class DownloadStatus(val progress: Float, val isComplete: Boolean, val isFailed: Boolean, val contentUri: Uri?)

    suspend fun monitorDownload(context: Context, id: Long, onUpdate: suspend (DownloadStatus) -> Unit) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        var downloading = true
        while (downloading && coroutineContext.isActive) {
            val query = android.app.DownloadManager.Query().setFilterById(id)
            var cursor: Cursor? = null
            try {
                cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                    val bytesDl = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTot = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    when (status) {
                        android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            val contentUri = manager.getUriForDownloadedFile(id)
                            onUpdate(DownloadStatus(1f, isComplete = true, isFailed = false, contentUri = contentUri))
                        }
                        android.app.DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            onUpdate(DownloadStatus(0f, isComplete = false, isFailed = true, contentUri = null))
                        }
                        else -> {
                            if (bytesTot > 0) {
                                onUpdate(DownloadStatus(bytesDl.toFloat() / bytesTot.toFloat(), isComplete = false, isFailed = false, contentUri = null))
                            }
                        }
                    }
                } else {
                    downloading = false
                }
            } catch (e: Exception) {
                downloading = false
            } finally {
                cursor?.close()
            }
            delay(AppConstants.UPDATE_POLL_INTERVAL_MS)
        }
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        manager.remove(downloadId)
    }

    fun installUpdate(context: Context, uri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, AppConstants.MIME_APK)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(installIntent)
    }
}
