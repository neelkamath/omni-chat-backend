package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.ContactsData.Table.contact
import com.neelkamath.omniChat.db.ContactsData.Table.contactOwner
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object ContactsData {
    private const val userIdLength = 36

    /** Each user's ([contactOwner]'s) saved [contact]s. */
    object Table : IntIdTable() {
        override val tableName get() = "Contacts"

        /** User ID. */
        val contactOwner = varchar("contact_owner", userIdLength)

        /** User ID. */
        val contact = varchar("contact", userIdLength)
    }

    fun read(userId: String): UserIdList = DB.transact {
        UserIdList(Table.select { contactOwner eq userId }.map { it[contact] }.toSet())
    }

    fun create(userId: String, userIdList: UserIdList): Unit = DB.transact {
        Table.batchInsert(userIdList.userIdList) {
            this[contactOwner] = userId
            this[contact] = it
        }
    }

    fun delete(userId: String, userIdList: UserIdList): Unit = DB.transact {
        Table.deleteWhere { (contactOwner eq userId) and (contact inList userIdList.userIdList) }
    }

    /** Deletes any row containing a column with the [userId]. */
    fun deleteUserEntries(userId: String): Unit = DB.transact {
        Table.deleteWhere { (contactOwner eq userId) or (contact eq userId) }
    }
}