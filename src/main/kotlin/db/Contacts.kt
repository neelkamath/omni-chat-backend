package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.db.Contacts.contactOwnerUserId
import com.neelkamath.omniChat.db.Contacts.contactUserId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

/** Each user's (denoted by their [contactOwnerUserId]) saved contacts (denoted by the [contactUserId]s). */
object Contacts : IntIdTable() {
    val contactOwnerUserId = varchar("contact_owner", Auth.userIdLength)
    val contactUserId = varchar("contact", Auth.userIdLength)

    fun create(userId: String, userIdList: Set<String>): Unit = DB.transact {
        batchInsert(userIdList) {
            this[contactOwnerUserId] = userId
            this[contactUserId] = it
        }
    }

    /** Returns the user ID list of the contacts saved by the contact owner (denoted by the [userId]). */
    fun read(userId: String): Set<String> = DB.transact {
        select { contactOwnerUserId eq userId }.map { it[contactUserId] }.toSet()
    }

    fun delete(userId: String, userIdList: Set<String>): Unit = DB.transact {
        deleteWhere { (contactOwnerUserId eq userId) and (contactUserId inList userIdList) }
    }

    /** Deletes any row containing a column with the [userId]. */
    fun deleteUserEntries(userId: String): Unit = DB.transact {
        deleteWhere { (contactOwnerUserId eq userId) or (contactUserId eq userId) }
    }
}