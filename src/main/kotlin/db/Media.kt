package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.db.Pic.Companion.ORIGINAL_MAX_BYTES
import com.neelkamath.omniChatBackend.db.Pic.Companion.THUMBNAIL_MAX_BYTES
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.annotation.processing.Generated
import javax.imageio.ImageIO

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [Audio.MAX_BYTES]. */
data class Audio(
    /** At most [Audio.MAX_BYTES]. */
    val bytes: ByteArray,
    val type: Type,
) {
    init {
        require(bytes.size <= MAX_BYTES) { "The audio mustn't exceed $MAX_BYTES bytes." }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Audio

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    enum class Type {
        MP3, MP4;

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"m4a"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension.toLowerCase()) {
                "mp3" -> MP3
                "mp4", "m4a", "m4p", "m4b", "m4r", "m4v" -> MP4
                else ->
                    throw IllegalArgumentException("The audio ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}

/**
 * Throws an [IllegalArgumentException] if the [original] exceeds [Pic.ORIGINAL_MAX_BYTES], or the [thumbnail] exceeds
 * [Pic.THUMBNAIL_MAX_BYTES].
 */
data class Pic(
    val type: Type,
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

        other as Pic

        if (!original.contentEquals(other.original)) return false
        if (type != other.type) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = original.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    enum class Type {
        PNG, JPEG;

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"pjpeg"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension.toLowerCase()) {
                "png" -> PNG
                "jpg", "jpeg", "jfif", "pjpeg", "pjp" -> JPEG
                else ->
                    throw IllegalArgumentException("The image ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    companion object {
        const val ORIGINAL_MAX_BYTES = 5 * 1024 * 1024
        const val THUMBNAIL_MAX_BYTES = 100 * 100

        /**
         * The [type] is found from the [name] (e.g., `"jpg"`, `"img.png"`). The [thumbnail] is created using the
         * [original].
         *
         * An [IllegalArgumentException] will be thrown if the [Type] is invalid, or the [original] is too big.
         */
        fun build(name: String, original: ByteArray): Pic {
            val extension = name.substringAfterLast(".")
            return build(Type.build(extension), original)
        }

        fun build(type: Type, original: ByteArray): Pic = Pic(type, original, createThumbnail(type, original))
    }
}

/**
 * Creates an image no larger than 100px by 100px where each pixel is at most 1 byte. It uses 8-bit color. The created
 * image will have the same [type] as the original [bytes].
 */
fun createThumbnail(type: Pic.Type, bytes: ByteArray): ByteArray {
    val bufferedPic = bytes.inputStream().use(ImageIO::read)
    if (bufferedPic.width <= 100 && bufferedPic.height <= 100) return bytes
    val multiplier = 100.0 / listOf(bufferedPic.width, bufferedPic.height).maxOrNull()!!
    val width = bufferedPic.width.times(multiplier).toInt()
    val height = bufferedPic.height.times(multiplier).toInt()
    val scaledPic = bufferedPic.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val bufferedThumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    bufferedThumbnail.createGraphics().drawImage(scaledPic, 0, 0, null)
    return ByteArrayOutputStream().use {
        ImageIO.write(bufferedThumbnail, type.toString(), it)
        it.toByteArray()
    }
}
