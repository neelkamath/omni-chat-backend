package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.PostgresEnum
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.Generated

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [Pics.MAX_PIC_BYTES]. */
data class Pic(
    /** At most [Pics.MAX_PIC_BYTES]. */
    val bytes: ByteArray,
    val type: Type
) {
    init {
        if (bytes.size > Pics.MAX_PIC_BYTES)
            throw IllegalArgumentException("The pic mustn't exceed ${Pics.MAX_PIC_BYTES} bytes.")
    }

    /** @see [buildType] */
    enum class Type {
        PNG {
            override fun toString() = "png"
        },
        JPEG {
            override fun toString() = "jpg"
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pic

        if (!bytes.contentEquals(other.bytes)) return false
        if (type != other.type) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    companion object {
        /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"pjpeg"`) isn't one of the [Type]s. */
        fun buildType(extension: String): Type = when (extension) {
            "png" -> Type.PNG
            "jpg", "jpeg", "jfif", "pjpeg", "pjp" -> Type.JPEG
            else ->
                throw IllegalArgumentException("The pic ($extension) must be one of ${Type.values().joinToString()}.")
        }
    }
}

object Pics : IntIdTable() {
    /** The pic cannot exceed 25 MiB. */
    const val MAX_PIC_BYTES = 25 * 1024 * 1024

    private val bytes: Column<ByteArray> = binary("bytes", MAX_PIC_BYTES)
    private val type: Column<Pic.Type> = customEnumeration(
        name = "type",
        sql = "pic_type",
        fromDb = { Pic.Type.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("pic_type", it) }
    )

    /** Returns the ID of the pic. */
    fun create(pic: Pic): Int = transaction {
        insertAndGetId {
            it[this.bytes] = pic.bytes
            it[this.type] = pic.type
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