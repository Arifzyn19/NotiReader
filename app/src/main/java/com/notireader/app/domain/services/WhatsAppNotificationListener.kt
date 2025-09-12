package com.notireader.app.domain.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import com.notireader.app.util.MediaCopyUtil
import com.notireader.app.util.MediaStoreUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var notiRepository: NotiRepository

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
//        Log.d("xyz", "Notification received: ${sbn?.packageName}, id=${sbn?.id}, key=${sbn?.key}")
        val validPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
        val sourcePackage = sbn?.packageName ?: ""
        if (sourcePackage in validPackages) {
            val isGroupSummary = sbn?.notification?.extras?.getBoolean("android.isGroupSummary", false)
            if (isGroupSummary == true) {
                Log.d("xyz", "Skipping group summary notification: id=${sbn.id}, key=${sbn.key}")
                return
            }
            var title = sbn?.notification?.extras?.getString("android.title") ?: ""
            var message = sbn?.notification?.extras?.getCharSequence("android.text")?.toString() ?: ""
            val timeStamp = sbn?.postTime
            val largeIcon = sbn?.notification?.largeIcon
            val creationTime = sbn?.notification?.`when`
//            Log.d("xyz", "creation time: ${sbn?.notification?.`when`}")
            val newMsgRegex = Regex("\\d+ new messages", RegexOption.IGNORE_CASE)
            if (newMsgRegex.containsMatchIn(title) || newMsgRegex.containsMatchIn(message) || title.equals("WhatsApp", ignoreCase = true)) {
                Log.d("xyz", "Filtered out notification: title=$title, message=$message")
                return
            }
            if (":" in title) {
                val parts = title.split(":", limit = 2)
                if (parts.size == 2) {
                    val groupName = parts[0].replace(Regex("\\(.*messages.*\\)", RegexOption.IGNORE_CASE), "").trim()
                    val senderName = parts[1].trim()
                    message = if (message.startsWith(senderName)) message else "$senderName: $message"
                    title = groupName
                }
            } else if (title.contains("(") && title.contains("new messages", ignoreCase = true)) {
                title = title.replace(Regex("\\(.*new messages.*\\)", RegexOption.IGNORE_CASE), "").trim()
            }
            if (newMsgRegex.matches(message.trim())) {
                Log.d("xyz", "Filtered out notification with 'new messages' as message: $message")
                return
            }
            if (message.trim().equals("This message was deleted", ignoreCase = true)) {
                Log.d("xyz", "Filtered out deleted message notification: $message")
                return
            }
            Log.d("xyz", "Notification details: title=$title, message=$message, timeStamp=$timeStamp, creationTime=$creationTime")
            val mediaTypes = mapOf(
                "Photo" to "WhatsApp Images",
                "Video" to "WhatsApp Video",
                "Sticker" to "WhatsApp Stickers",
                "GIF" to "WhatsApp Animated Gifs",
                "Audio" to "WhatsApp Audio",
                "Document" to "WhatsApp Documents",
                "Voice message" to "WhatsApp Voice Notes"
            )
            var matchedType = mediaTypes.entries.find { entry ->
                message.contains(entry.key, ignoreCase = true)
            }
            if (matchedType == null) {
                val docExtensions = listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt")
                if (docExtensions.any { message.lowercase().contains(it) }) {
                    matchedType = mapOf("Document" to "WhatsApp Documents").entries.first()
                }
            }
            Log.d("xyz", "Matched media type: ${matchedType?.key} -> ${matchedType?.value}")
            CoroutineScope(Dispatchers.IO).launch {
                var mediaPathLocal: String? = null
                if (matchedType != null) {
                    try {
                        var bestMatchFileUri: android.net.Uri? = null

                        // PRIMARY: Try DocumentFile approach first (more reliable for WhatsApp)
                        val prefs = getSharedPreferences("noti_reader_prefs", MODE_PRIVATE)
                        val waMediaUriString = prefs.getString("folder_uri", null)
                        Log.d("xyz", "waMediaUriString: $waMediaUriString")

                        if (waMediaUriString != null) {
                            val waMediaUri = waMediaUriString.toUri()
                            val waDir = DocumentFile.fromTreeUri(this@WhatsAppNotificationListener, waMediaUri)
                            Log.d("xyz", "waDir: ${waDir?.uri}")

                            val comWhatsappDir = waDir?.findFile(
                                if (sourcePackage == "com.whatsapp.w4b") "com.whatsapp.w4b" else "com.whatsapp"
                            )
                            val whatsAppDir = comWhatsappDir?.findFile(
                                if (sourcePackage == "com.whatsapp.w4b") "WhatsApp Business" else "WhatsApp"
                            )
                            val mediaDir = whatsAppDir?.findFile("Media")
                            val subDir = mediaDir?.findFile(matchedType.value)
                            Log.d("xyz", "subDir: ${subDir?.uri}")

                            if (subDir != null) {
                                val files = when (matchedType.value) {
                                    "WhatsApp Voice Notes" -> {
                                        val validVoiceExtensions = listOf(".opus", ".m4a", ".mp3", ".wav")
                                        subDir.listFiles()
                                            .filter { it.isDirectory }
                                            .flatMap { it.listFiles().toList() }
                                            .filter {
                                                it.isFile && it.name?.let { name ->
                                                    validVoiceExtensions.any { ext -> name.endsWith(ext, ignoreCase = true) }
                                                } == true
                                            }
                                    }
                                    else -> {
                                        subDir.listFiles().filter { it.isFile && it.name != ".nomedia" }
                                    }
                                }

                                // Find the file with timestamp closest to the notification timestamp
                                val notificationTime = timeStamp ?: creationTime ?: System.currentTimeMillis()
                                val bestMatchFile = findBestMatchingFile(files, notificationTime)
                                Log.d("xyz", "DocumentFile bestMatchFile: ${bestMatchFile?.uri}")

                                if (bestMatchFile != null) {
                                    // Copy using existing DocumentFile method
                                    val appMediaRoot = waDir.findFile("com.notireader.app")
                                        ?: waDir.createDirectory("com.notireader.app")
                                    if (appMediaRoot != null) {
                                        val rootFolderName = when (sourcePackage) {
                                            "com.whatsapp" -> "NotiReader_WA_Media"
                                            "com.whatsapp.w4b" -> "NotiReader_Business_Media"
                                            else -> "NotiReader_Other_Media"
                                        }
                                        val appMediaDir = appMediaRoot.findFile(rootFolderName)
                                            ?: appMediaRoot.createDirectory(rootFolderName)
                                        if (appMediaDir != null) {
                                            val appTypeDir = appMediaDir.findFile(matchedType.value)
                                                ?: appMediaDir.createDirectory(matchedType.value)
                                            Log.d("xyz", "appTypeDir: ${appTypeDir?.uri}")
                                            if (appTypeDir != null) {
                                                val ext = bestMatchFile.name?.substringAfterLast('.', "") ?: ""
                                                val newFileName = "${title}_${timeStamp}.${ext}"
                                                val copiedUri = MediaCopyUtil.copyMediaFile(
                                                    this@WhatsAppNotificationListener,
                                                    bestMatchFile.uri,
                                                    appTypeDir.uri,
                                                    matchedType.value,
                                                    newFileName,
                                                    sourcePackage
                                                )
                                                Log.d("xyz", "DocumentFile copiedUri: $copiedUri")
                                                mediaPathLocal = copiedUri?.toString()
                                                bestMatchFileUri = bestMatchFile.uri
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // FALLBACK: If DocumentFile didn't find a file, try MediaStore API
                        if (bestMatchFileUri == null) {
                            Log.d("xyz", "DocumentFile failed, trying MediaStore API as fallback")
                            val mediaStoreUri = MediaStoreUtil.findMediaUsingMediaStore(
                                this@WhatsAppNotificationListener,
                                matchedType.key,
                                timeStamp ?: creationTime,
                                sourcePackage
                            )

                            if (mediaStoreUri != null) {
                                Log.d("xyz", "MediaStore found file: $mediaStoreUri")
                                val ext = getFileExtensionFromUri(mediaStoreUri) ?: ""
                                val newFileName = "${title}_${timeStamp}.${ext}"

                                val copiedUri = MediaStoreUtil.copyMediaStoreFileToAppStorage(
                                    this@WhatsAppNotificationListener,
                                    mediaStoreUri,
                                    matchedType.value,
                                    newFileName,
                                    sourcePackage
                                )
                                Log.d("xyz", "MediaStore copiedUri: $copiedUri")
                                mediaPathLocal = copiedUri?.toString()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("xyz", "Exception during media copy", e)
                    }
                }

                val messageModel = MessageModel(
                    sender = title,
                    message = message,
                    timestamp = timeStamp!!,
                    isDeleted = false,
                    mediaPath = mediaPathLocal,
                    sourcePackage = sourcePackage,
                    iconRes = largeIcon,
                )
                notiRepository.onWhatsAppNotificationReceived(messageModel)
                Log.d("xyz", "onWhatsAppNotificationReceived: $messageModel")
            }
//            cancelNotification(sbn.key)
            return
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        if (sbn?.packageName == "com.whatsapp") {
            if (reason == REASON_APP_CANCEL) {
                val title = sbn.notification.extras.getString("android.title") ?: ""
                val message = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
                val timeStamp = sbn.postTime
                CoroutineScope(Dispatchers.IO).launch {
                    notiRepository.markMessageAsDeletedByDetails(title, message, timeStamp)
                }
            }
        }
    }

    private fun findBestMatchingFile(files: List<DocumentFile>, notificationTime: Long): DocumentFile? {
        if (files.isEmpty()) return null

        if (files.size == 1) return files.first()

        val filesWithTimestamps = files.mapNotNull { file ->
            val filename = file.name ?: return@mapNotNull null
            val timestamp = extractTimestampFromFilename(filename)
            if (timestamp != null) {
                file to timestamp
            } else {
                file to file.lastModified()
            }
        }

        if (filesWithTimestamps.isEmpty()) {
            return files.maxByOrNull { it.lastModified() }
        }

        return filesWithTimestamps.minByOrNull { (_, fileTime) ->
            kotlin.math.abs(fileTime - notificationTime)
        }?.first
    }

    private fun extractTimestampFromFilename(filename: String): Long? {
        try {
            val pattern1 = Regex("(IMG|VID|AUD|DOC)-(\\d{8})-WA\\d+\\.")
            val match1 = pattern1.find(filename)
            if (match1 != null) {
                val dateStr = match1.groupValues[2]
                return parseWhatsAppDate(dateStr)
            }

            val pattern2 = Regex("(IMG|VID|AUD)_(\\d{8})_(\\d{6})\\.")
            val match2 = pattern2.find(filename)
            if (match2 != null) {
                val dateStr = match2.groupValues[2]
                val timeStr = match2.groupValues[3]
                return parseWhatsAppDateTime(dateStr, timeStr)
            }

            val pattern3 = Regex("PTT-(\\d{8})-WA\\d+\\.")
            val match3 = pattern3.find(filename)
            if (match3 != null) {
                val dateStr = match3.groupValues[1]
                return parseWhatsAppDate(dateStr)
            }

        } catch (e: Exception) {
            Log.w("xyz", "Error parsing timestamp from filename: $filename", e)
        }
        return null
    }

    private fun parseWhatsAppDate(dateStr: String): Long? {
        try {
            if (dateStr.length != 8) return null
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt() - 1
            val day = dateStr.substring(6, 8).toInt()

            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, day, 0, 0, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseWhatsAppDateTime(dateStr: String, timeStr: String): Long? {
        try {
            if (dateStr.length != 8 || timeStr.length != 6) return null
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt() - 1
            val day = dateStr.substring(6, 8).toInt()
            val hour = timeStr.substring(0, 2).toInt()
            val minute = timeStr.substring(2, 4).toInt()
            val second = timeStr.substring(4, 6).toInt()

            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, day, hour, minute, second)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        } catch (e: Exception) {
            return null
        }
    }

    private fun getFileExtensionFromUri(uri: android.net.Uri): String? {
        return try {
            val mimeType = contentResolver.getType(uri)
            when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/3gpp" -> "3gp"
                "audio/mpeg" -> "mp3"
                "audio/ogg" -> "ogg"
                "audio/mp4" -> "m4a"
                "audio/opus" -> "opus"
                "application/pdf" -> "pdf"
                "application/msword" -> "doc"
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                else -> {
                    val projection = arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val displayName = cursor.getString(0)
                            displayName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
                        } else null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("xyz", "Error getting file extension from URI", e)
            null
        }
    }
}
