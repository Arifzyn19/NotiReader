package com.notireader.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

object MediaStoreUtil {

    fun findMediaUsingMediaStore(
        context: Context,
        matchedTypeKey: String,
        timeStamp: Long?,
        sourcePackage: String
    ): Uri? {
        return try {
            val collectionUri = when (matchedTypeKey) {
                "Photo", "Sticker", "GIF" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "Video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "Audio", "Voice message" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                "Document" -> MediaStore.Files.getContentUri("external")
                else -> return null
            }

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE
            )

            val notificationTimeInSeconds = (timeStamp ?: System.currentTimeMillis()) / 1000
            Log.d("xyz", "=== MediaStore Debug Info ===")
            Log.d("xyz", "Media type: $matchedTypeKey -> Collection: $collectionUri")
            Log.d("xyz", "Notification time: $notificationTimeInSeconds (${java.util.Date(timeStamp ?: 0)})")
            Log.d("xyz", "Source package: $sourcePackage")

            // Try multiple times with increasing search windows
            val maxRetries = 4
            val retryDelays = listOf(1000L, 3000L, 7000L, 15000L)
            val maxSearchWindows = listOf(30L, 60L, 120L, 300L)

            for (attempt in 0 until maxRetries) {
                Log.d("xyz", "=== MediaStore attempt ${attempt + 1}/$maxRetries ===")

                val maxSearchAfter = maxSearchWindows[attempt]

                val selection = "${MediaStore.MediaColumns.DATE_ADDED} > ? AND ${MediaStore.MediaColumns.DATE_ADDED} <= ?"
                val selectionArgs = arrayOf(
                    notificationTimeInSeconds.toString(),
                    (notificationTimeInSeconds + maxSearchAfter).toString()
                )
                val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} ASC"

                Log.d(
                    "xyz",
                    "Search window: ${notificationTimeInSeconds} to ${notificationTimeInSeconds + maxSearchAfter} (${maxSearchAfter}s after)"
                )

                var bestMatchUri: Uri? = null
                var bestMatchTimeDiff: Long = Long.MAX_VALUE
                var filesFound = 0
                var whatsappFilesFound = 0

                context.contentResolver.query(
                    collectionUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    filesFound = cursor.count
                    Log.d("xyz", "Total files found in time window: $filesFound")

                    while (cursor.moveToNext()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                        val fileId = cursor.getLong(idColumn)
                        val fileName = cursor.getString(nameColumn) ?: "Unknown"
                        val filePath = cursor.getString(dataColumn) ?: ""
                        val fileDate = cursor.getLong(dateColumn)
                        val fileSize = cursor.getLong(sizeColumn)

                        Log.d("xyz", "File: $fileName")
                        Log.d("xyz", "  Path: $filePath")
                        Log.d("xyz", "  Date: $fileDate (${java.util.Date(fileDate * 1000)})")
                        Log.d("xyz", "  Size: ${fileSize} bytes")
                        Log.d("xyz", "  Time diff: ${fileDate - notificationTimeInSeconds}s")

                        // Check if this file is from WhatsApp media directory
                        if (isWhatsAppMediaFile(filePath, sourcePackage)) {
                            whatsappFilesFound++
                            val timeDiff = fileDate - notificationTimeInSeconds
                            Log.d("xyz", "  ✅ WHATSAPP FILE FOUND! Time diff: ${timeDiff}s")

                            // Find the file with minimum positive time difference (closest after notification)
                            if (timeDiff > 0 && timeDiff < bestMatchTimeDiff) {
                                bestMatchTimeDiff = timeDiff
                                bestMatchUri = Uri.withAppendedPath(collectionUri, fileId.toString())
                                Log.d("xyz", "  🎯 NEW BEST MATCH: $fileName with time diff ${timeDiff}s")
                            }
                        } else {
                            Log.d("xyz", "  ❌ Not a WhatsApp file")
                        }
                    }
                }

                Log.d("xyz", "Summary for attempt ${attempt + 1}: Total files: $filesFound, WhatsApp files: $whatsappFilesFound")

                // If we found a match, return it
                if (bestMatchUri != null) {
                    Log.d("xyz", "🎉 MediaStore found best matching file with time diff: ${bestMatchTimeDiff}s")

                    // Try to take persistent permission if possible
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            bestMatchUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Log.d("xyz", "✅ Persistent permission granted")
                    } catch (e: SecurityException) {
                        Log.d("xyz", "⚠️ Could not take persistent permission: ${e.message}")
                    }

                    return bestMatchUri
                }

                // If not the last attempt, wait before retrying
                if (attempt < maxRetries - 1) {
                    Log.d("xyz", "No WhatsApp media found in attempt ${attempt + 1}, waiting ${retryDelays[attempt]}ms for download...")
                    Thread.sleep(retryDelays[attempt])
                }
            }

            Log.d("xyz", "❌ MediaStore query found no matching file after $maxRetries attempts")

            // DEBUG: Let's see what files exist in a wider time range for debugging
            Log.d("xyz", "=== DEBUG: Checking wider time range for any files ===")
            val debugSelection = "${MediaStore.MediaColumns.DATE_ADDED} >= ? AND ${MediaStore.MediaColumns.DATE_ADDED} <= ?"
            val debugSelectionArgs = arrayOf(
                (notificationTimeInSeconds - 300).toString(), // 5 minutes before
                (notificationTimeInSeconds + 300).toString()  // 5 minutes after
            )

            context.contentResolver.query(
                collectionUri,
                projection,
                debugSelection,
                debugSelectionArgs,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                Log.d("xyz", "DEBUG: Found ${cursor.count} files in 10-minute window around notification")
                var debugCount = 0
                while (cursor.moveToNext() && debugCount < 10) { // Show max 10 files
                    val fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "Unknown"
                    val filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)) ?: ""
                    val fileDate = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                    val timeDiff = fileDate - notificationTimeInSeconds

                    Log.d("xyz", "DEBUG File ${debugCount + 1}: $fileName")
                    Log.d("xyz", "  Path: $filePath")
                    Log.d("xyz", "  Time diff: ${timeDiff}s")
                    Log.d("xyz", "  Is WhatsApp: ${isWhatsAppMediaFile(filePath, sourcePackage)}")
                    debugCount++
                }
            }

            null
        } catch (e: Exception) {
            Log.e("xyz", "💥 Error searching MediaStore", e)
            null
        }
    }

    private fun isWhatsAppMediaFile(filePath: String?, sourcePackage: String): Boolean {
        if (filePath == null || filePath.isEmpty()) {
            Log.d("xyz", "    File path is null or empty")
            return false
        }

        Log.d("xyz", "    Checking if path belongs to WhatsApp: $filePath")

        val isWhatsAppFile = when (sourcePackage) {
            "com.whatsapp" -> {
                val checks = listOf(
                    filePath.contains("/WhatsApp/Media/", ignoreCase = true),
                    filePath.contains("/com.whatsapp/", ignoreCase = true),
                    filePath.contains("WhatsApp Images", ignoreCase = true),
                    filePath.contains("WhatsApp Video", ignoreCase = true),
                    filePath.contains("WhatsApp Audio", ignoreCase = true),
                    filePath.contains("WhatsApp Documents", ignoreCase = true),
                    filePath.contains("WhatsApp Voice Notes", ignoreCase = true),
                    filePath.contains("WhatsApp Stickers", ignoreCase = true),
                    filePath.contains("WhatsApp Animated Gifs", ignoreCase = true),
                    // Additional patterns for newer Android versions
                    filePath.contains("/Android/media/com.whatsapp/", ignoreCase = true),
                    filePath.contains("/storage/emulated/0/WhatsApp/", ignoreCase = true)
                )

                checks.forEachIndexed { index, check ->
                    if (check) {
                        Log.d("xyz", "    ✅ Matched WhatsApp pattern #${index + 1}")
                        return true
                    }
                }
                false
            }

            "com.whatsapp.w4b" -> {
                val checks = listOf(
                    filePath.contains("/WhatsApp Business/Media/", ignoreCase = true),
                    filePath.contains("/com.whatsapp.w4b/", ignoreCase = true),
                    filePath.contains("WhatsApp Business", ignoreCase = true),
                    // Additional patterns
                    filePath.contains("/Android/media/com.whatsapp.w4b/", ignoreCase = true),
                    filePath.contains("/storage/emulated/0/WhatsApp Business/", ignoreCase = true)
                )

                checks.forEachIndexed { index, check ->
                    if (check) {
                        Log.d("xyz", "    ✅ Matched WhatsApp Business pattern #${index + 1}")
                        return true
                    }
                }
                false
            }

            else -> false
        }

        if (!isWhatsAppFile) {
            Log.d("xyz", "    ❌ No WhatsApp patterns matched for $sourcePackage")
        }

        return isWhatsAppFile
    }

    fun copyMediaStoreFileToAppStorage(
        context: Context,
        sourceUri: Uri,
        matchedTypeValue: String,
        newFileName: String,
        sourcePackage: String
    ): Uri? {
        return try {
            val appMediaDir = context.getExternalFilesDir("media") ?: return null

            val rootFolderName = when (sourcePackage) {
                "com.whatsapp" -> "NotiReader_WA_Media"
                "com.whatsapp.w4b" -> "NotiReader_Business_Media"
                else -> "NotiReader_Other_Media"
            }

            val rootDir = java.io.File(appMediaDir, rootFolderName)
            if (!rootDir.exists()) rootDir.mkdirs()

            val typeDir = java.io.File(rootDir, matchedTypeValue)
            if (!typeDir.exists()) typeDir.mkdirs()

            val destFile = java.io.File(typeDir, newFileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            Log.d("xyz", "MediaStore file copied to app storage: ${destFile.absolutePath}")
            return Uri.fromFile(destFile)

        } catch (e: Exception) {
            Log.e("xyz", "Error copying MediaStore file to app storage", e)
            null
        }
    }
}
