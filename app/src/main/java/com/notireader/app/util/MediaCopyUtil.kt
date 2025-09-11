package com.notireader.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

object MediaCopyUtil {
    fun copyMediaFile(
        context: Context,
        sourceUri: Uri,
        matchedType: String,
        newFileName: String,
        sourcePackage: String
    ): Uri? {
        return try {
            val rootFolderName = when (sourcePackage) {
                "com.whatsapp" -> "NotiReader_WA_Media"
                "com.whatsapp.w4b" -> "NotiReader_Business_Media"
                else -> "NotiReader_Other_Media"
            }

            val appMediaRoot = DocumentFile.fromFile(context.getExternalFilesDir(null)!!)
                .findFile("com.notireader.app")
                ?: DocumentFile.fromFile(context.getExternalFilesDir(null)!!).createDirectory("com.notireader.app")

            val rootDir = getOrCreateSubDir(appMediaRoot, rootFolderName)
            val destDir = getOrCreateSubDir(rootDir, matchedType)

            if (destDir != null) {
                val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
                destDir.findFile(newFileName)?.delete()
                val destFile = destDir.createFile(mimeType, newFileName)
                Log.i("xyz", "destFile: ${destFile?.uri}")
                if (destFile != null) {
                    context.contentResolver.openInputStream(sourceUri).use { input ->
                        context.contentResolver.openOutputStream(destFile.uri).use { output ->
                            if (input != null && output != null) {
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                    }
                    return destFile.uri
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getOrCreateSubDir(parent: DocumentFile?, name: String): DocumentFile? {
        if (parent == null || !parent.isDirectory) return null
        var dir = parent.findFile(name)
        if (dir == null || !dir.isDirectory) {
            dir = parent.createDirectory(name)
        }
        return dir
    }
}
