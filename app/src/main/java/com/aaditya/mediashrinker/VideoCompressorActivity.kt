package com.aaditya.mediashrinker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class VideoCompressorActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    private lateinit var selectVideoButton: Button
    private lateinit var compressVideoButton: Button
    private lateinit var shareVideoButton: Button

    private lateinit var resultText: TextView

    private lateinit var resolutionSpinner: Spinner

    private var selectedVideoUri: Uri? = null

    private var compressedVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_video_compressor
        )

        videoView =
            findViewById(R.id.videoView)

        selectVideoButton =
            findViewById(R.id.selectVideoButton)

        compressVideoButton =
            findViewById(R.id.compressVideoButton)

        shareVideoButton =
            findViewById(R.id.shareVideoButton)

        resultText =
            findViewById(R.id.resultText)

        resolutionSpinner =
            findViewById(R.id.resolutionSpinner)

        val modes = arrayOf(

            "1080p High Quality",

            "720p Balanced",

            "480p Small Size"
        )

        val adapter =
            ArrayAdapter(

                this,

                android.R.layout.simple_spinner_dropdown_item,

                modes
            )

        resolutionSpinner.adapter =
            adapter

        selectVideoButton.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )

            intent.type =
                "video/*"

            startActivityForResult(
                intent,
                200
            )
        }

        compressVideoButton.setOnClickListener {

            if (selectedVideoUri == null) {

                Toast.makeText(

                    this,

                    "Select video first",

                    Toast.LENGTH_SHORT

                ).show()

                return@setOnClickListener
            }

            compressVideo()
        }

        shareVideoButton.setOnClickListener {

            if (compressedVideoUri == null) {

                Toast.makeText(

                    this,

                    "No compressed video",

                    Toast.LENGTH_SHORT

                ).show()

                return@setOnClickListener
            }

            val shareIntent =
                Intent(Intent.ACTION_SEND)

            shareIntent.type =
                "video/*"

            shareIntent.putExtra(

                Intent.EXTRA_STREAM,

                compressedVideoUri
            )

            shareIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(

                Intent.createChooser(
                    shareIntent,
                    "Share Video"
                )
            )
        }
    }

    private fun compressVideo() {

        try {

            resultText.text =
                "Compressing Video..."

            val inputFile =
                File(
                    cacheDir,
                    "input_video.mp4"
                )

            val inputStream =
                contentResolver.openInputStream(
                    selectedVideoUri!!
                )

            val cacheStream =
                inputFile.outputStream()

            inputStream?.copyTo(
                cacheStream
            )

            inputStream?.close()

            cacheStream.close()

            val outputFile =
                File(
                    cacheDir,
                    "compressed_${System.currentTimeMillis()}.mp4"
                )

            val selectedMode =
                resolutionSpinner.selectedItem.toString()

            val scaleCommand =

                when {

                    selectedMode.contains("1080") ->

                        "scale=1920:1080"

                    selectedMode.contains("720") ->

                        "scale=1280:720"

                    else ->

                        "scale=854:480"
                }

            val command =

                "-y " +
                        "-i \"${inputFile.absolutePath}\" " +
                        "-vf $scaleCommand " +
                        "-vcodec libx264 " +
                        "-preset ultrafast " +
                        "-crf 35 " +
                        "-acodec aac " +
                        "-b:a 128k " +
                        "\"${outputFile.absolutePath}\""

            FFmpegKit.executeAsync(command) { session ->

                val returnCode =
                    session.returnCode

                runOnUiThread {

                    if (ReturnCode.isSuccess(returnCode)) {

                        saveCompressedVideo(
                            outputFile
                        )

                    } else {

                        resultText.text =
                            "Compression Failed"
                    }
                }
            }

        } catch (e: Exception) {

            resultText.text =
                "Compression Failed"
        }
    }

    private fun saveCompressedVideo(
        outputFile: File
    ) {

        try {

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Video.Media.DISPLAY_NAME,
                        "compressed_video_${System.currentTimeMillis()}.mp4"
                    )

                    put(
                        MediaStore.Video.Media.MIME_TYPE,
                        "video/mp4"
                    )

                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/MediaShrinker"
                    )
                }

            val savedUri =
                contentResolver.insert(

                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,

                    values
                )

            if (savedUri != null) {

                compressedVideoUri =
                    savedUri

                val outputStream =
                    contentResolver.openOutputStream(savedUri)

                val inputStream =
                    outputFile.inputStream()

                inputStream.copyTo(outputStream!!)

                inputStream.close()

                outputStream.flush()

                outputStream.close()

                resultText.text =
                    "Video Compressed Successfully"

                Toast.makeText(

                    this,

                    "Saved in Movies/MediaShrinker",

                    Toast.LENGTH_LONG

                ).show()
            }

        } catch (e: Exception) {

            resultText.text =
                "Save Failed"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (

            requestCode == 200 &&

            resultCode == Activity.RESULT_OK &&

            data != null
        ) {

            selectedVideoUri =
                data.data

            videoView.setVideoURI(
                selectedVideoUri
            )

            videoView.setOnPreparedListener {

                it.isLooping = true

                videoView.start()
            }

            resultText.text =
                "Video Selected"
        }
    }
}