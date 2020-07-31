package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.PostgresEnum
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Used for profile pics, and group chat pics.
 *
 * @see [PicMessages]
 */
object Pics : IntIdTable() {
    private val bytes: Column<ByteArray> = binary("bytes", Pic.MAX_BYTES)
    private val type: Column<Pic.Type> = customEnumeration(
        name = "type",
        sql = "pic_type",
        fromDb = { Pic.Type.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("pic_type", it) }
    )

    /** Returns the ID of the pic. */
    fun create(pic: Pic): Int = transaction {
        insertAndGetId {
            it[bytes] = pic.bytes
            it[type] = pic.type
        }.value
    }

    fun read(id: Int): Pic = transaction {
        select { Pics.id eq id }.first()
    }.let { Pic(it[bytes], it[type]) }

    /**
     * Returns the [pic]'s [id] after updating it.
     *
     * - If the [id] is an [Int], and the [pic] is a [Pic], the pic will be updated, and its ID will be returned.
     * - If the [id] is an [Int], and the [pic] is `null`, the pic will be deleted, and `null` will be returned.
     * - If the [id] is `null`, and the [pic] is a [Pic], the pic will be created, and its ID will be returned.
     * - If the [id] is `null`, and the [pic] is `null`, `null` will be returned.
     */
    fun update(id: Int?, pic: Pic?): Int? = when {
        id != null && pic != null -> {
            transaction {
                update({ Pics.id eq id }) {
                    it[bytes] = pic.bytes
                    it[type] = pic.type
                }
            }
            id
        }
        id != null && pic == null -> {
            delete(id)
            null
        }
        id == null && pic != null -> create(pic)
        id == null && pic == null -> null
        else -> throw NoWhenBranchMatchedException()
    }

    fun delete(id: Int): Unit = transaction {
        deleteWhere { Pics.id eq id }
    }
}