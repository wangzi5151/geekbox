package com.tinyai.geekbox.feature.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.File
import java.io.RandomAccessFile

data class AudioMeta(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val bitrate: Int,
    val mime: String?,
    val sampleRate: Int,
    val channels: Int
)

data class DecodeResult(
    val targetPath: String,
    val pcmBytes: Long,
    val sampleRate: Int,
    val channels: Int
)

data class VideoMeta(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bitrate: Int,
    val rotation: Int,
    val frameRate: String?,
    val mime: String?
)

class AudioRepository(private val context: Context) {

    fun metadata(path: String): AudioMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            var mime: String? = null
            var sampleRate = 0
            var channels = 0
            runCatching {
                val extractor = MediaExtractor()
                extractor.setDataSource(path)
                for (i in 0 until extractor.trackCount) {
                    val f = extractor.getTrackFormat(i)
                    val m = f.getString(MediaFormat.KEY_MIME) ?: continue
                    if (m.startsWith("audio/")) {
                        mime = m
                        if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        break
                    }
                }
                extractor.release()
            }
            AudioMeta(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0,
                mime = mime,
                sampleRate = sampleRate,
                channels = channels
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun videoMeta(path: String): VideoMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            fun meta(key: Int): String? = retriever.extractMetadata(key)
            VideoMeta(
                width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                bitrate = meta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0,
                rotation = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0,
                frameRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) meta(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE) else null,
                mime = meta(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun decodeToWav(sourcePath: String, target: File): DecodeResult {
        val extractor = MediaExtractor()
        extractor.setDataSource(sourcePath)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val m = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (m.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalArgumentException("未找到音频轨道")
        }
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("未知编码")
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        target.parentFile?.mkdirs()
        var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
        val bitsPerSample = 16
        var total = 0L
        val info = MediaCodec.BufferInfo()

        RandomAccessFile(target, "rw").use { raf ->
            raf.setLength(0)
            raf.write(ByteArray(44))
            var inputDone = false
            var outputDone = false
            var guard = 0
            while (!outputDone && guard < 20_000_000) {
                guard++
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        if (buffer == null) {
                            inputDone = true
                        } else {
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        val encoding = runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && codec.outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                codec.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            } else {
                                AudioFormat.ENCODING_PCM_16BIT
                            }
                        }.getOrDefault(AudioFormat.ENCODING_PCM_16BIT)

                        if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val floats = buffer.asFloatBuffer()
                            val count = info.size / 4
                            val out = ByteArray(count * 2)
                            for (i in 0 until count) {
                                val s = (floats.get(i) * 32767f).toInt().coerceIn(-32768, 32767)
                                out[i * 2] = s.toByte()
                                out[i * 2 + 1] = (s ushr 8).toByte()
                            }
                            raf.write(out)
                            total += out.size
                        } else {
                            val bytes = ByteArray(info.size)
                            buffer.position(info.offset)
                            buffer.get(bytes, 0, info.size)
                            raf.write(bytes)
                            total += bytes.size
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = codec.outputFormat
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { extractor.release() }
            raf.seek(0)
            raf.write(wavHeader(total.toInt(), sampleRate, channels, bitsPerSample))
        }
        return DecodeResult(target.absolutePath, total, sampleRate, channels)
    }

    private fun wavHeader(dataLen: Int, sampleRate: Int, channels: Int, bits: Int): ByteArray {
        val byteRate = sampleRate * channels * bits / 8
        val blockAlign = channels * bits / 8
        val h = ByteArray(44)
        fun str(off: Int, s: String) { for (i in s.indices) h[off + i] = s[i].toByte() }
        fun i32(off: Int, v: Int) {
            h[off] = v.toByte(); h[off + 1] = (v ushr 8).toByte(); h[off + 2] = (v ushr 16).toByte(); h[off + 3] = (v ushr 24).toByte()
        }
        fun i16(off: Int, v: Int) { h[off] = v.toByte(); h[off + 1] = (v ushr 8).toByte() }
        str(0, "RIFF"); i32(4, 36 + dataLen); str(8, "WAVE")
        str(12, "fmt "); i32(16, 16); i16(20, 1); i16(22, channels)
        i32(24, sampleRate); i32(28, byteRate); i16(32, blockAlign); i16(34, bits)
        str(36, "data"); i32(40, dataLen)
        return h
    }
}
