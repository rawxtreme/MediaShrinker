package com.aaditya.mediashrinker

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import java.io.ByteArrayOutputStream
import java.io.OutputStream

object PdfUtils {

    /**
     * Creates a PDF from a list of image URIs.
     * If [password] is non-null and non-blank, the PDF will be encrypted
     * with that password as the USER password (required to open the file).
     * [onProgress] is called after each photo is processed, with (current, total).
     */
    fun createPdf(
        context: Context,
        imageUris: List<Uri>,
        password: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Uri? {
        try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val filename = "MediaShrinker_$timeStamp.pdf"

            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, filename)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/MediaShrinker")
            }

            val pdfUri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            ) ?: return null

            val finalOutputStream: OutputStream? = context.contentResolver.openOutputStream(pdfUri)

            if (password.isNullOrBlank()) {
                // ===== No password — write directly =====
                val document = Document()
                PdfWriter.getInstance(document, finalOutputStream)
                document.open()
                addImagesToDocument(context, document, imageUris, onProgress)
                document.close()
                finalOutputStream?.flush()
                finalOutputStream?.close()
            } else {
                // ===== Password protected =====
                // Step 1: build the unencrypted PDF in memory
                val tempBytes = ByteArrayOutputStream()
                val document = Document()
                PdfWriter.getInstance(document, tempBytes)
                document.open()
                addImagesToDocument(context, document, imageUris, onProgress)
                document.close()

                // Step 2: re-read it and stamp it with encryption + password
                val reader = PdfReader(tempBytes.toByteArray())
                val stamper = PdfStamper(reader, finalOutputStream)

                val passwordBytes = password.toByteArray()

                stamper.setEncryption(
                    passwordBytes,         // user password (needed to open)
                    passwordBytes,         // owner password (full permissions)
                    PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                    PdfWriter.ENCRYPTION_AES_128
                )

                stamper.close()
                reader.close()
                finalOutputStream?.flush()
                finalOutputStream?.close()
            }

            return pdfUri

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Decodes a bitmap at a SAFE downsized resolution instead of full camera resolution.
    // A single full-res photo (e.g. 4000x3000) can eat 40-50MB of memory when decoded.
    // Decoding 10-15 of those back to back is enough to hit Android's per-app memory
    // limit and crash silently mid-loop. Capping the longer side at 1600px keeps every
    // photo sharp enough for a PDF page while using a fraction of the memory.
    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }

        var sampleSize = 1
        while ((boundsOptions.outWidth / sampleSize) > maxDimension ||
            (boundsOptions.outHeight / sampleSize) > maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    }

    private fun addImagesToDocument(
        context: Context,
        document: Document,
        imageUris: List<Uri>,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ) {
        val total = imageUris.size

        for ((index, uri) in imageUris.withIndex()) {
            try {
                val bitmap = decodeSampledBitmap(context, uri)
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    bitmap.recycle() // free memory immediately instead of waiting for garbage collector

                    val image = Image.getInstance(stream.toByteArray())
                    image.scaleToFit(500f, 700f)
                    image.spacingAfter = 20f

                    document.add(image)
                }
            } catch (e: Exception) {
                // One bad/corrupt photo should not kill the whole PDF — skip it and continue.
                e.printStackTrace()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }

            onProgress?.invoke(index + 1, total)
        }
    }
}