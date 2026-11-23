package com.example.data.media

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaHelper(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    private val audioDir: File
        get() = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }

    private var recorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var player: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null

    fun copyImageFromUri(uri: Uri): String? {
        return runCatching {
            val dest = File(photosDir, "img_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0) dest.absolutePath else null
        }.getOrNull()
    }

    fun startRecording(): String? {
        stopRecording()
        return runCatching {
            val outFile = File(audioDir, "voice_${UUID.randomUUID()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentRecordingFile = outFile
            outFile.absolutePath
        }.getOrNull()
    }

    fun stopRecording(): String? {
        val path = currentRecordingFile?.absolutePath
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        val exists = path != null && File(path).let { it.exists() && it.length() > 0 }
        currentRecordingFile = null
        return if (exists) path else null
    }

    fun playAudio(path: String, onCompleted: () -> Unit) {
        stopAudio()
        runCatching {
            val file = File(path)
            if (!file.exists()) {
                onCompleted()
                return
            }
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stopAudio()
                    onCompleted()
                }
                prepare()
                start()
            }
        }.onFailure {
            stopAudio()
            onCompleted()
        }
    }

    fun stopAudio() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    fun playGentleChime() {
        runCatching {
            val gen = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45).also {
                toneGenerator = it
            }
            gen.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        }
    }

    fun deleteAllMediaFiles() {
        stopRecording()
        stopAudio()
        runCatching { toneGenerator?.release() }
        toneGenerator = null
        photosDir.deleteRecursively()
        audioDir.deleteRecursively()
    }
}
