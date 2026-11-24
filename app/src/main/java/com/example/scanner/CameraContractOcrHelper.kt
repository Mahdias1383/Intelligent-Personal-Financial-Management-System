package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device ML Kit Text Recognition helper for scanning paper loan contracts and receipts
 * from CameraX captures or zero-permission Android Photo Picker URIs.
 */
object CameraContractOcrHelper {

    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        if (cont.isActive) {
                            cont.resume(visionText.text)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (cont.isActive) {
                            cont.resumeWithException(error)
                        }
                    }
            }
        } finally {
            recognizer.close()
        }
    }

    suspend fun recognizeTextFromUri(
        context: Context,
        uri: Uri
    ): String {
        val decodedBitmap = withContext(Dispatchers.IO) {
            decodeSafeScaledBitmap(context, uri, maxDimensionPx = 2048)
        }
        return if (decodedBitmap != null) {
            try {
                recognizeTextFromBitmap(decodedBitmap, rotationDegrees = 0)
            } finally {
                decodedBitmap.recycle()
            }
        } else {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = withContext(Dispatchers.IO) {
                InputImage.fromFilePath(context, uri)
            }
            try {
                suspendCancellableCoroutine { cont ->
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            if (cont.isActive) cont.resume(visionText.text)
                        }
                        .addOnFailureListener { error ->
                            if (cont.isActive) cont.resumeWithException(error)
                        }
                }
            } finally {
                recognizer.close()
            }
        }
    }

    fun rotateBitmapIfNeeded(source: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return source
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun decodeSafeScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int
    ): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val rawW = boundsOpts.outWidth
            val rawH = boundsOpts.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            while ((rawW / sampleSize) > maxDimensionPx || (rawH / sampleSize) > maxDimensionPx) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }
}
