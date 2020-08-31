package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.tables.OnetimeTokens.create
import com.neelkamath.omniChat.db.tables.OnetimeTokens.delete
import com.neelkamath.omniChat.db.tables.OnetimeTokens.exists
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * JWTs meant for onetime.
 *
 * Here's how to use onetime tokens:
 * 1. [create] an entry to get the `"jti"` claim to use when building the JWT.
 * 1. Build the JWT using the ID you received in the previous step.
 * 1. When a client uses the onetime token, check if it's `"jti"` claim [exists]. It will since it's the first time its
 * being used. Therefore, the client is authorized. [delete] the entry.
 * 1. When a client uses the onetime token, check if it's `"jti"` claim [exists]. It won't since it's not the first
 * time. Therefore, the client is unauthorized.
 */
object OnetimeTokens : IntIdTable() {
    /** Creates an entry for a onetime token. Returns the `"jti"` claim for the onetime token. */
    fun create(): Int = transaction {
        insertAndGetId {}.value
    }

    fun exists(id: Int): Boolean = transaction {
        select { OnetimeTokens.id eq id }.empty().not()
    }

    /** Deletes the onetime token by its JWT [id] claim. This is safe to call even if the [id] doesn't exist. */
    fun delete(id: Int): Unit = transaction {
        deleteWhere { OnetimeTokens.id eq id }
    }
}