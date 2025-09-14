package com.notireader.app.domain.services

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.WorkerThread
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var notiRepository: NotiRepository

    private val TAG = "WhatsAppNL"
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Simple in-memory dedupe to avoid handling same notification multiple times in quick succession.
    // Key => timestamp of last processed (millis)
    private val recentHandled = ConcurrentHashMap<String, Long>()
    private val DEDUPE_WINDOW_MS = 5_000L

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras
        var title = extras.getString("android.title") ?: ""
        var message = extras.getCharSequence("android.text")?.toString() ?: ""
        val timeStamp = sbn.postTime

        // message filtering
        val newMsgRegex = Regex("\\d+ new messages", RegexOption.IGNORE_CASE)
        if (newMsgRegex.containsMatchIn(title) || newMsgRegex.containsMatchIn(message) || title.equals("WhatsApp", ignoreCase = true) || title.equals(
                "Backup in progress", ignoreCase = true
            )
        ) {
            Log.d(TAG, "Filtered summary/new messages")
            return
        }
        if (message.trim().equals("This message was deleted", ignoreCase = true)) {
            Log.d(TAG, "Filtered deleted message")
            return
        }

        // dedupe key: package + title + message + approximate time
        val dedupeKey = "$pkg|$title|${message.take(100)}"
        val last = recentHandled[dedupeKey] ?: 0L
        if (System.currentTimeMillis() - last < DEDUPE_WINDOW_MS) {
            Log.d(TAG, "Skipping duplicate notification (dedupe)")
            return
        }
        recentHandled[dedupeKey] = System.currentTimeMillis()

        if (":" in title) {
            val parts = title.split(":", limit = 2)
            if (parts.size == 2) {
                val groupName = parts[0].replace(Regex("\\(\\d+\\s+messages?\\)", RegexOption.IGNORE_CASE), "").trim()
                val senderName = parts[1].trim()
                // Ensure message includes the sender name
                message = if (message.startsWith(senderName)) message else "$senderName: $message"
                title = groupName
            }
        } else if (title.contains("(") && title.contains("new messages", ignoreCase = true)) {
            title = title.replace(Regex("\\(.*new messages.*\\)", RegexOption.IGNORE_CASE), "").trim()
        }

        // Determine media type (Image/Video/Audio/Document/Sticker/GIF)
        val mediaTypes = mapOf(
            "Photo" to MediaKind.IMAGE,
            "Image" to MediaKind.IMAGE,
            "Video" to MediaKind.VIDEO,
            "GIF" to MediaKind.IMAGE,
            "Sticker" to MediaKind.IMAGE,
            "Audio" to MediaKind.AUDIO,
            "Voice message" to MediaKind.AUDIO,
            "Document" to MediaKind.DOCUMENT
        )

        var matchedKind: MediaKind? = mediaTypes.entries.find { (k, _) ->
            message.contains(k, ignoreCase = true)
        }?.value

        // fallback to extension check
        if (matchedKind == null) {
            val extList = listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt")
            if (extList.any { message.lowercase().contains(it) }) matchedKind = MediaKind.DOCUMENT
        }

        scope.launch {
            val mediaCount = parseMediaCount(message)
            val waitTime = if (mediaCount > 1) (mediaCount * 1000L) else 3000L
            delay(waitTime) // let the media download
            try {
                val mediaPaths = findAndCopyWhatsAppMedia(matchedKind, timeStamp, title, mediaCount)
                val messageModel = MessageModel(
                    sender = title, message = message, timestamp = timeStamp, isDeleted = false, mediaPaths = emptyList(), // populated later
                    sourcePackage = pkg, iconRes = sbn.notification.largeIcon
                )
                notiRepository.onWhatsAppNotificationReceivedWithMedia(messageModel, mediaPaths)
                Log.d(TAG, "Processed notification: $messageModel")
            } catch (t: Throwable) {
                Log.e(TAG, "Error handling notification", t)
            }
        }
    }

    @WorkerThread
    private fun findAndCopyWhatsAppMedia(kind: MediaKind?, notificationTime: Long, title: String, count: Int): List<String> {
        if (kind == null) {
            Log.d(TAG, "No media type matched in notification")
            return emptyList()
        }

        // Ensure we have READ_MEDIA permissions. The host app is expected to handle permission flow.
        if (!hasRequiredMediaPermission(kind)) {
            Log.w(TAG, "Missing required READ_MEDIA permission for $kind")
            return emptyList()
        }

        // Query window (seconds). Start with +/- 120s then expand to +/- 1 day if not found.
        val windows = listOf(2L, 5L, 10L, 120L, 600L, 86_400L) // seconds
        val notificationTimeSeconds = notificationTime / 1000L

        for (window in windows) {
            val min = notificationTimeSeconds - window
            val max = notificationTimeSeconds + window
            val candidates = queryMediaStoreCandidates(kind, min, max)
            if (candidates.isNotEmpty()) {
                // pick candidate whose DATE_MODIFIED is closest to notificationTimeSeconds
//                val best = candidates.minByOrNull { candidate ->
//                    kotlin.math.abs(candidate.dateModified - notificationTimeSeconds)
//                }
//                best?.let {
//                    Log.d(TAG, "best matched file: ${it.uri}")
//                    return copyMediaToAppDir(it.uri, it.displayName, kind, title, notificationTime)
//                }
                // Sort by latest first
                val sorted = candidates.sortedByDescending { it.dateModified }

                // Take required count
                val selected = sorted.take(count)

                val copied = selected.mapNotNull {
                    copyMediaToAppDir(it.uri, it.displayName, kind, title, notificationTime)
                }

                if (copied.isNotEmpty()) return copied
            }
        }

        Log.d(TAG, "No matching WhatsApp media found for kind=$kind")
        return emptyList()
    }

    private fun parseMediaCount(message: String): Int {
        val regex = Regex("(\\d+)\\s+(photos?|videos?|audios?|documents?|media)", RegexOption.IGNORE_CASE)
        val match = regex.find(message)
        if (match != null) {
            return match.groups[1]?.value?.toIntOrNull() ?: 1
        }

        // Also check for emoji patterns
        val emojiRegex = Regex("[📷📹🎵📄]\\s*(\\d+)\\s+(photos?|videos?|audios?|documents?)", RegexOption.IGNORE_CASE)
        val emojiMatch = emojiRegex.find(message)
        if (emojiMatch != null) {
            return emojiMatch.groups[1]?.value?.toIntOrNull() ?: 1
        }

        Log.d(TAG, "parseMediaCount for message: '$message' -> returning 1")
        return 1
    }

    private fun hasRequiredMediaPermission(kind: MediaKind): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (kind) {
                MediaKind.IMAGE -> checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
                MediaKind.VIDEO -> checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                MediaKind.AUDIO, MediaKind.DOCUMENT -> checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } else {
            // For older devices, either READ_EXTERNAL_STORAGE or nothing depending on your target
            true
        }
    }

    private data class MediaCandidate(val uri: Uri, val displayName: String, val dateModified: Long)

    private fun queryMediaStoreCandidates(kind: MediaKind, minSeconds: Long, maxSeconds: Long): List<MediaCandidate> {
        val results = mutableListOf<MediaCandidate>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else "_data"
        )

        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        // date range
        selectionBuilder.append("${MediaStore.MediaColumns.DATE_MODIFIED} BETWEEN ? AND ?")
        selectionArgs.add(minSeconds.toString())
        selectionArgs.add(maxSeconds.toString())

        // filter for WhatsApp in relative path or display name
        // RELATIVE_PATH example: "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionBuilder.append(" AND (${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)")
            selectionArgs.add("%WhatsApp%")
            selectionArgs.add("%WA%") // many WA files contain WA in name (WA0001 etc)
        } else {
            // older devices: use display name fallback
            selectionBuilder.append(" AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?")
            selectionArgs.add("%WA%")
        }

        val selection = selectionBuilder.toString()
        val uri = when (kind) {
            MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.AUDIO, MediaKind.DOCUMENT -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri, projection, selection, selectionArgs.toTypedArray(), "${MediaStore.MediaColumns.DATE_MODIFIED} DESC" // recent first
            )

            if (cursor != null) {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: "file"
                    val dateModified = cursor.getLong(dateCol)
                    val itemUri = ContentUris.withAppendedId(uri, id)
                    results.add(MediaCandidate(itemUri, displayName, dateModified))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed", e)
        } finally {
            cursor?.close()
        }

        return results
    }

    private fun copyMediaToAppDir(sourceUri: Uri, displayName: String, kind: MediaKind, title: String, notificationTime: Long): String? {
        var pfd: ParcelFileDescriptor? = null
        var inStream: InputStream? = null
        var outStream: FileOutputStream? = null
        try {
            pfd = contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd == null) {
                Log.w(TAG, "openFileDescriptor returned null for $sourceUri")
                return null
            }
            inStream = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)

            // Prepare destination
            val mediaRoot = getExternalFilesDir(null) ?: filesDir // fallback if external not available
            val appMediaDir = File(mediaRoot, "NotiReader_WA_Media/${kind.directoryName}")
            if (!appMediaDir.exists()) appMediaDir.mkdirs()

            val ext = displayName.substringAfterLast('.', "")
            val safeTitle = title.replace(Regex("[^A-Za-z0-9_]"), "_")
            val destName = "${safeTitle}_${notificationTime}${if (ext.isNotBlank()) ".$ext" else ""}"
            val destFile = File(appMediaDir, destName)

            outStream = FileOutputStream(destFile)

            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                outStream.write(buffer, 0, read)
            }
            outStream.flush()
            Log.d(TAG, "Copied media to ${destFile.absolutePath}")
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy media", e)
            return null
        } finally {
            try {
                inStream?.close()
            } catch (_: Exception) {
            }
            try {
                outStream?.close()
            } catch (_: Exception) {
            }
            try {
                pfd?.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        if (sbn.packageName != "com.whatsapp") return

        val title = sbn.notification.extras.getString("android.title") ?: ""
        val message = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val timeStamp = sbn.postTime

        scope.launch {
            notiRepository.markMessageAsDeletedByDetails(title, message, timeStamp)
        }
    }

    private enum class MediaKind(val directoryName: String) {
        IMAGE("WhatsApp_Images"), VIDEO("WhatsApp_Video"), AUDIO("WhatsApp_Audio"), DOCUMENT("WhatsApp_Documents")
    }
}
