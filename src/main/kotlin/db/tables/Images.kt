package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.db.ImageType
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Used for profile images, and group chat image.
 *
 * @see ImageMessages
 */
object Images : IntIdTable() {
    private val original: Column<ByteArray> = binary("original", ProcessedImage.ORIGINAL_MAX_BYTES)
    private val thumbnail: Column<ByteArray> = binary("thumbnail", ProcessedImage.THUMBNAIL_MAX_BYTES)

    /** Returns the ID of the image. */
    fun create(image: ProcessedImage): Int = transaction {
        insertAndGetId {
            it[original] = image.original
            it[thumbnail] = image.thumbnail
        }.value
    }

    fun read(imageId: Int, type: ImageType): ByteArray = transaction {
        val image = select(Images.id eq imageId).first()
        when (type) {
            ImageType.ORIGINAL -> image[original]
            ImageType.THUMBNAIL -> image[thumbnail]
        }
    }

    /**
     * Returns the [image]'s [imageId] after updating it.
     *
     * - If the [imageId] is an [Int], and the [image] is a [ProcessedImage], the pic will be updated, and its ID will be
     * returned.
     * - If the [imageId] is an [Int], and the [image] is `null`, the pic will be deleted, and `null` will be returned.
     * - If the [imageId] is `null`, and the [image] is a [ProcessedImage], the pic will be created, and its ID will be returned.
     * - If the [imageId] is `null`, and the [image] is `null`, `null` will be returned.
     */
    fun update(imageId: Int?, image: ProcessedImage?): Int? = when {
        imageId != null && image != null -> {
            transaction {
                update({ Images.id eq imageId }) {
                    it[original] = image.original
                    it[thumbnail] = image.thumbnail
                }
            }
            imageId
        }
        imageId != null && image == null -> {
            delete(imageId)
            null
        }
        imageId == null && image != null -> create(image)
        imageId == null && image == null -> null
        else -> throw NoWhenBranchMatchedException()
    }

    fun delete(imageId: Int): Unit = transaction {
        deleteWhere { Images.id eq imageId }
    }
}
