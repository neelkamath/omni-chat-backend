package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Contacts
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

    fun read(userId: String): Contacts = DB.transact {
        Contacts(Table.select { contactOwner eq userId }.map { it[contact] }.toSet())
    }

    fun create(userId: String, contacts: Contacts): Unit = DB.transact {
        Table.batchInsert(contacts.userIdList) {
            this[contactOwner] = userId
            this[contact] = it
        }
    }

    fun delete(userId: String, contacts: Contacts): Unit = DB.transact {
        Table.deleteWhere { (contactOwner eq userId) and (contact inList contacts.userIdList) }
    }

    /** Deletes any row containing a column with the [userId]. */
    fun deleteUserEntries(userId: String): Unit = DB.transact {
        Table.deleteWhere { (contactOwner eq userId) or (contact eq userId) }
    }
}