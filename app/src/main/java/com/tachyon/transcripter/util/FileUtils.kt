package com.tachyon.transcripter.util

// FileUtils.kt

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.DecimalFormat

/**
 * Utility object for file operations.
 */
object FileUtils {

    /**
     * Format file size to human-readable string.
     *
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()

        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val format = DecimalFormat("#,##0.#")

        return "${format.format(value)} ${units[digitGroups]}"
    }

    /**
     * Get file extension from filename.
     *
     * @param filename Filename
     * @return Extension (e.g., "mp3") or empty string
     */
    fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot + 1)
        } else {
            ""
        }
    }

    /**
     * Get MIME type from file extension.
     *
     * @param extension File extension
     * @return MIME type or null
     */
    fun getMimeType(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    /**
     * Copy file from source to destination.
     *
     * @param source Source file
     * @param destination Destination file
     * @return True if successful
     */
    fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Delete file or directory recursively.
     *
     * @param file File or directory to delete
     * @return True if successful
     */
    fun deleteRecursively(file: File): Boolean {
        return try {
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculate MD5 hash of file.
     *
     * @param file File to hash
     * @return MD5 hash string or null on error
     */
    fun calculateMd5(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    md.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            md.digest().joinToString("") { byte -> "%02x".format(byte) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if external storage is available for read/write.
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Check if external storage is available for read.
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED ||
                state == Environment.MEDIA_MOUNTED_READ_ONLY
    }

    /**
     * Get available storage space on device.
     *
     * @param path Path to check (defaults to internal storage)
     * @return Available bytes
     */
    fun getAvailableSpace(path: File): Long {
        return try {
            path.usableSpace
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get total storage space on device.
     *
     * @param path Path to check
     * @return Total bytes
     */
    fun getTotalSpace(path: File): Long {
        return try {
            path.totalSpace
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Create directory if it doesn't exist.
     *
     * @param directory Directory to create
     * @return True if directory exists or was created
     */
    fun ensureDirectoryExists(directory: File): Boolean {
        return directory.exists() || directory.mkdirs()
    }

    /**
     * Get file creation time.
     *
     * @param file File
     * @return Creation time in milliseconds, or 0 if unavailable
     */
    fun getFileCreationTime(file: File): Long {
        return if (file.exists()) {
            file.lastModified()
        } else {
            0L
        }
    }

    /**
     * Validate filename (remove invalid characters).
     *
     * @param filename Original filename
     * @return Valid filename
     */
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(255) // Max filename length
    }

    /**
     * Get content URI for file.
     *
     * @param context Context
     * @param file File
     * @return Content URI or null
     */
    fun getContentUri(context: Context, file: File): Uri? {
        return try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}