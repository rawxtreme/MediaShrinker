package com.aaditya.mediashrinker

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import java.io.OutputStream

object PdfUtils {

    fun createPdf(

        context: Context,

        imageUris: List<Uri>

    ): Uri? {

        try {

            val filename =
                "MediaShrinker_${System.currentTimeMillis()}.pdf"

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        filename
                    )

                    put(
                        MediaStore.Files.FileColumns.MIME_TYPE,
                        "application/pdf"
                    )

                    put(
                        MediaStore.Files.FileColumns.RELATIVE_PATH,
                        "Documents/MediaShrinker"
                    )
                }

            val pdfUri =
                context.contentResolver.insert(

                    MediaStore.Files
                        .getContentUri("external"),

                    values
                )

            if (pdfUri != null) {

                val outputStream: OutputStream? =

                    context.contentResolver
                        .openOutputStream(pdfUri)

                val document =
                    Document()

                PdfWriter.getInstance(
                    document,
                    outputStream
                )

                document.open()

                for (uri in imageUris) {

                    val inputStream =
                        context.contentResolver
                            .openInputStream(uri)

                    val bitmap =
                        BitmapFactory
                            .decodeStream(inputStream)

                    val stream =
                        java.io.ByteArrayOutputStream()

                    bitmap.compress(

                        android.graphics.Bitmap
                            .CompressFormat.JPEG,

                        90,

                        stream
                    )

                    val image =
                        Image.getInstance(
                            stream.toByteArray()
                        )

                    image.scaleToFit(
                        500f,
                        700f
                    )

                    image.spacingAfter = 20f

                    document.add(image)
                }

                document.close()

                outputStream?.flush()

                outputStream?.close()

                return pdfUri
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }

        return null
    }
}