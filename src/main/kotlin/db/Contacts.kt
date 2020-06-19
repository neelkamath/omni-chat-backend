package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.USER_ID_LENGTH
import com.neelkamath.omniChat.db.Contacts.contactId
import org.jetbrains.exposed.sql.*

object Contacts : Table() {
    /** The ID of the user who has saved the [contactId]. */
    private val contactOwnerId: Column<String> = varchar("contact_owner_id", USER_ID_LENGTH)

    /** The ID of the user whose contact has been saved by the [contactId]. */
    private val contactId: Column<String> = varchar("contact_id", USER_ID_LENGTH)

    /** Saves the [ownerId]'s [contacts], ignoring existing contacts. */
    fun create(ownerId: String, contacts: Set<String>) {
        val existingContacts = read(ownerId)
        val newContacts = contacts.filterNot { it in existingContacts }
        transact {
            batchInsert(newContacts) {
                this[contactOwnerId] = ownerId
                this[contactId] = it
            }
        }
    }

    /** Returns the user ID list of the contacts saved by the contact owner (denoted by the [userId]). */
    fun read(userId: String): List<String> = transact {
        select { contactOwnerId eq userId }.map { it[contactId] }
    }

    /** Deletes the [ownerId]'s contacts from the [contactIdList], ignoring nonexistent contacts. */
    fun delete(ownerId: String, contactIdList: List<String>): Unit = transact {
        deleteWhere { (contactOwnerId eq ownerId) and (contactId inList contactIdList) }
    }

    /** Deletes every contact who owns, or is owned by, the given [userId]. */
    fun deleteUserEntries(userId: String): Unit = transact {
        deleteWhere { (contactOwnerId eq userId) or (contactId eq userId) }
    }
}