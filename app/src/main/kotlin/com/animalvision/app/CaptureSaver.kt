package com.animalvision.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves captures to the system gallery via MediaStore (no storage permission needed
 * on API 29+; on 26–28 we write into the app-owned public pictures directory which
 * MediaStore indexes on scan). Filenames encode animal + timestamp (spec §29).
 */
object CaptureSaver {

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun saveJpeg(context: Context, bitmap: Bitmap, animalName: String): Uri? {
        val name = "AnimalVision_${animalName.replace(' ', '_')}_${timestamp()}"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AnimalVision")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { os ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
                } ?: return null
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "AnimalVision",
                )
                dir.mkdirs()
                val file = File(dir, "$name.jpg")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                }
                scanFile(context, file)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Moves a finished recording from cache into the Movies gallery. */
    fun publishVideo(context: Context, src: File, animalName: String): Uri? {
        if (!src.exists() || src.length() == 0L) return null
        val name = "AnimalVision_${animalName.replace(' ', '_')}_${timestamp()}"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AnimalVision")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { os ->
                    src.inputStream().use { it.copyTo(os) }
                } ?: return null
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                src.delete()
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "AnimalVision",
                )
                dir.mkdirs()
                val dst = File(dir, "$name.mp4")
                src.copyTo(dst, overwrite = true)
                src.delete()
                scanFile(context, dst)
                Uri.fromFile(dst)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scanFile(context: Context, file: File) {
        android.media.MediaScannerConnection.scanFile(
            context, arrayOf(file.absolutePath), null, null,
        )
    }
}
