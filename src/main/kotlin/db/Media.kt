package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.db.ProcessedImage.Companion.ORIGINAL_MAX_BYTES
import com.neelkamath.omniChatBackend.db.ProcessedImage.Companion.THUMBNAIL_MAX_BYTES
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.annotation.processing.Generated
import javax.imageio.ImageIO

/** A filename (e.g., `"image.png"`) which is at most 255 characters. */
typealias Filename = String

/** An MP4 video. Throws an [IllegalArgumentException] if the [bytes] exceeds [VideoFile.MAX_BYTES]. */
data class VideoFile(
    val filename: Filename,
    /** At most [VideoFile.MAX_BYTES]. */
    val bytes: ByteArray,
) {
    init {
        require(bytes.size <= MAX_BYTES) { "The video mustn't exceed $MAX_BYTES bytes." }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoFile

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    companion object {
        const val MAX_BYTES = 3 * 1_024 * 1_024
    }
}

/** An [IllegalArgumentException] will be thrown if the [bytes] exceeds [DocFile.MAX_BYTES]. */
data class DocFile(val filename: Filename, val bytes: ByteArray) {
    init {
        require(bytes.size <= MAX_BYTES) { "The doc cannot exceed $MAX_BYTES bytes." }
    }

    companion object {
        /** Docs cannot exceed 3 MiB. */
        const val MAX_BYTES = 3 * 1_024 * 1_024
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocFile

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [ImageFile.MAX_BYTES]. */
data class ImageFile(val filename: Filename, val bytes: ByteArray) {
    init {
        require(bytes.size <= MAX_BYTES) { "The image mustn't exceed $MAX_BYTES bytes." }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageFile

        if (filename != other.filename) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    companion object {
        const val MAX_BYTES = 3 * 1_024 * 1_024
    }
}

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [AudioFile.MAX_BYTES]. */
data class AudioFile(
    val filename: Filename,
    /** At most [AudioFile.MAX_BYTES]. */
    val bytes: ByteArray,
) {
    init {
        require(bytes.size <= MAX_BYTES) { "The audio mustn't exceed $MAX_BYTES bytes." }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioFile

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    companion object {
        const val MAX_BYTES = 3 * 1_024 * 1_024

        /** Whether the [extension] is one of the supported audio types (i.e., MP3 and MP4). */
        fun isValidExtension(extension: String): Boolean =
            setOf("mp3", "mp4", "m4a", "m4p", "m4b", "m4r", "m4v").any { it.equals(extension, ignoreCase = true) }
    }
}

/**
 * Throws an [IllegalArgumentException] if the [original] exceeds [ProcessedImage.ORIGINAL_MAX_BYTES], or the
 * [thumbnail] exceeds [ProcessedImage.THUMBNAIL_MAX_BYTES].
 */
data class ProcessedImage(
    val filename: Filename,
    /** At most [ORIGINAL_MAX_BYTES]. */
    val original: ByteArray,
    /** At most [THUMBNAIL_MAX_BYTES]. */
    val thumbnail: ByteArray,
) {
    init {
        require(original.size <= ORIGINAL_MAX_BYTES) { "The original image mustn't exceed $ORIGINAL_MAX_BYTES bytes." }
        require(thumbnail.size <= THUMBNAIL_MAX_BYTES) { "The thumbnail mustn't exceed $THUMBNAIL_MAX_BYTES bytes." }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedImage

        if (!original.contentEquals(other.original)) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = original.contentHashCode()
        result = 31 * result + thumbnail.contentHashCode()
        return result
    }

    enum class Type {
        PNG, JPEG;

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"pjpeg"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension.lowercase()) {
                "png" -> PNG
                "jpg", "jpeg", "jfif", "pjpeg", "pjp" -> JPEG
                else ->
                    throw IllegalArgumentException("The image ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    companion object {
        const val ORIGINAL_MAX_BYTES = 3 * 1_024 * 1_024
        const val THUMBNAIL_MAX_BYTES = 100 * 100

        /**
         * The [thumbnail] is created using the [original].
         *
         * An [IllegalArgumentException] will be thrown if either the [extension] isn't a supported [Type] or the
         * [original] is bigger than [ORIGINAL_MAX_BYTES].
         */
        fun build(filename: Filename, original: ByteArray): ProcessedImage {
            val type = Type.build(filename.substringAfterLast("."))
            val thumbnail = createThumbnail(type, original)
            return ProcessedImage(filename, original, thumbnail)
        }
    }
}

enum class ImageType { ORIGINAL, THUMBNAIL }

/**
 * Creates an image no larger than 100px by 100px where each pixel is at most 1 byte. It uses 8-bit color. The created
 * image will have the same [type] as the original [bytes].
 */
fun createThumbnail(type: ProcessedImage.Type, bytes: ByteArray): ByteArray {
    val bufferedImage = bytes.inputStream().use(ImageIO::read)
    if (bufferedImage.width <= 100 && bufferedImage.height <= 100) return bytes
    val multiplier = 100.0 / listOf(bufferedImage.width, bufferedImage.height).maxOrNull()!!
    val width = bufferedImage.width.times(multiplier).toInt()
    val height = bufferedImage.height.times(multiplier).toInt()
    val scaledImage = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val bufferedThumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    bufferedThumbnail.createGraphics().drawImage(scaledImage, 0, 0, null)
    return ByteArrayOutputStream().use {
        ImageIO.write(bufferedThumbnail, type.toString(), it)
        it.toByteArray()
    }
}
