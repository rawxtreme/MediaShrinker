package com.aaditya.mediashrinker

import android.content.ContentValues
import android.content.Context
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
     */
    fun createPdf(
        context: Context,
        imageUris: List<Uri>,
        password: String? = null
    ): Uri? {
        try {
            val filename = "MediaShrinker_${System.currentTimeMillis()}.pdf"

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
                addImagesToDocument(context, document, imageUris)
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
                addImagesToDocument(context, document, imageUris)
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

    private fun addImagesToDocument(context: Context, document: Document, imageUris: List<Uri>) {
        for (uri in imageUris) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val stream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)

            val image = Image.getInstance(stream.toByteArray())
            image.scaleToFit(500f, 700f)
            image.spacingAfter = 20f

            document.add(image)
            inputStream?.close()
        }
    }
}