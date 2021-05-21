package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.PicType
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Used for profile pics, and group chat pics.
 *
 * @see PicMessages
 */
object Pics : IntIdTable() {
    private val original: Column<ByteArray> = binary("original", Pic.ORIGINAL_MAX_BYTES)
    private val thumbnail: Column<ByteArray> = binary("thumbnail", Pic.THUMBNAIL_MAX_BYTES)

    /** Returns the ID of the pic. */
    fun create(pic: Pic): Int = transaction {
        insertAndGetId {
            it[original] = pic.original
            it[thumbnail] = pic.thumbnail
        }.value
    }

    fun read(picId: Int, type: PicType): ByteArray = transaction {
        val pic = select(Pics.id eq picId).first()
        when (type) {
            PicType.ORIGINAL -> pic[original]
            PicType.THUMBNAIL -> pic[thumbnail]
        }
    }

    /**
     * Returns the [pic]'s [picId] after updating it.
     *
     * - If the [picId] is an [Int], and the [pic] is a [Pic], the pic will be updated, and its ID will be returned.
     * - If the [picId] is an [Int], and the [pic] is `null`, the pic will be deleted, and `null` will be returned.
     * - If the [picId] is `null`, and the [pic] is a [Pic], the pic will be created, and its ID will be returned.
     * - If the [picId] is `null`, and the [pic] is `null`, `null` will be returned.
     */
    fun update(picId: Int?, pic: Pic?): Int? = when {
        picId != null && pic != null -> {
            transaction {
                update({ Pics.id eq picId }) {
                    it[original] = pic.original
                    it[thumbnail] = pic.thumbnail
                }
            }
            picId
        }
        picId != null && pic == null -> {
            delete(picId)
            null
        }
        picId == null && pic != null -> create(pic)
        picId == null && pic == null -> null
        else -> throw NoWhenBranchMatchedException()
    }

    fun delete(picId: Int): Unit = transaction {
        deleteWhere { Pics.id eq picId }
    }
}
