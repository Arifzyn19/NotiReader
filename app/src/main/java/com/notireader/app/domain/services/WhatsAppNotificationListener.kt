package com.notireader.app.domain.services

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.net.toUri
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import com.notireader.app.util.MediaCopyUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.checkerframework.checker.regex.qual.Regex
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var notiRepository: NotiRepository

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
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
                        val latestUri = getLatestWhatsAppMedia(this@WhatsAppNotificationListener, matchedType.value)
                        if (latestUri != null) {
                            val newFileName = "${title}_${timeStamp}.${getExtensionFromMimeType(this@WhatsAppNotificationListener, latestUri)}"
                            val copiedUri = MediaCopyUtil.copyMediaFile(
                                this@WhatsAppNotificationListener,
                                latestUri,
                                matchedType.value,
                                newFileName,
                                sourcePackage
                            )
                            mediaPathLocal = copiedUri?.toString()
                            Log.d("xyz", "Media copied to: $mediaPathLocal")
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

    private fun getLatestWhatsAppMedia(context: Context, mediaType: String) =
        when (mediaType) {
            "WhatsApp Images" -> queryLatest(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            "WhatsApp Video" -> queryLatest(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            "WhatsApp Audio", "WhatsApp Voice Notes" -> queryLatest(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            "WhatsApp Documents" -> queryLatest(context, MediaStore.Files.getContentUri("external"))
            else -> null
        }

    private fun queryLatest(context: Context, collection: android.net.Uri): android.net.Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.RELATIVE_PATH)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%WhatsApp%")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT 1"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun getExtensionFromMimeType(context: Context, uri: android.net.Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return "dat"
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "dat"
    }
}
