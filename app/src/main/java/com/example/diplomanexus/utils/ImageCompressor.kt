package com.example.diplomanexus.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {

    /**
     * Gets the file size of a Uri in bytes.
     */
    fun getMediaSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * Compresses and resizes an image from a given Uri without loading the full image into memory.
     * Prevents OutOfMemoryErrors even for extremely large images (e.g. 50MP/300MB).
     *
     * @param context The application context
     * @param uri The Uri of the selected image
     * @param targetWidth The desired maximum width of the output image
     * @param targetHeight The desired maximum height of the output image
     * @param quality The compression quality (0-100)
     * @return Base64 data URI string of the compressed JPEG image, or null if compression fails.
     */
    fun compressImageFromUri(
        context: Context,
        uri: Uri,
        targetWidth: Int = 1080,
        targetHeight: Int = 1080,
        quality: Int = 80
    ): String? {
        var inputStream: InputStream? = null
        try {
            // Step 1: Decode image dimensions first (no pixels loaded into memory)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            // Step 2: Calculate the optimal inSampleSize to downscale the image
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false

            // Step 3: Decode the bitmap with inSampleSize (loads downscaled version only)
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) return null

            // Step 4: Compress the bitmap to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val imageBytes = outputStream.toByteArray()
            
            // Release bitmap memory immediately
            bitmap.recycle()

            // Step 5: Convert to Base64
            return "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun calculateInSampleSize(outWidth: Int, outHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (outHeight > reqHeight || outWidth > reqWidth) {
            val halfHeight = outHeight / 2
            val halfWidth = outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
