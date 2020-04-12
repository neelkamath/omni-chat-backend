package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.Contacts.contact
import com.neelkamath.omniChat.db.Contacts.contactOwner
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

/** Each user's ([contactOwner]'s) saved [contact]s. */
object Contacts : IntIdTable() {
    /** User ID. */
    val contactOwner = varchar("contact_owner", Auth.userIdLength)

    /** User ID. */
    val contact = varchar("contact", Auth.userIdLength)

    fun read(userId: String): UserIdList = DB.transact {
        UserIdList(select { contactOwner eq userId }.map { it[contact] }.toSet())
    }

    fun create(userId: String, userIdList: UserIdList): Unit = DB.transact {
        batchInsert(userIdList.userIdList) {
            this[contactOwner] = userId
            this[contact] = it
        }
    }

    fun delete(userId: String, userIdList: UserIdList): Unit = DB.transact {
        deleteWhere { (contactOwner eq userId) and (contact inList userIdList.userIdList) }
    }

    /** Deletes any row containing a column with the [userId]. */
    fun deleteUserEntries(userId: String): Unit = DB.transact {
        deleteWhere { (contactOwner eq userId) or (contact eq userId) }
    }
}