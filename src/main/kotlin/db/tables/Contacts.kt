package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.contactsBroker
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object Contacts : IntIdTable() {
    /** The ID of the user who has saved the [contactId]. */
    private val contactOwnerId: Column<String> = varchar("contact_owner_id", USER_ID_LENGTH)

    /** The ID of the user whose contact has been saved by the [contactId]. */
    private val contactId: Column<String> = varchar("contact_id", USER_ID_LENGTH)

    /**
     * Saves the [ownerId]'s [contactIdList], ignoring existing contacts. The [ownerId] will be notified of the
     * [NewContact] if they've [Broker.subscribe]d via [contactsBroker].
     */
    fun create(ownerId: String, contactIdList: Set<String>) {
        val existingContacts = readIdList(ownerId)
        val newContacts = contactIdList.filterNot { it in existingContacts }
        transact {
            batchInsert(newContacts) {
                this[contactOwnerId] = ownerId
                this[contactId] = it
            }
        }
        newContacts.forEach { userId ->
            contactsBroker.notify(NewContact.fromUserId(userId)) { it.userId == ownerId }
        }
    }

    /** @return the user ID list of the contacts saved by the contact [ownerId]. */
    fun readIdList(ownerId: String): List<String> = transact {
        select { contactOwnerId eq ownerId }.map { it[contactId] }
    }

    /** @return the [ownerId]'s contacts. */
    private fun readRows(ownerId: String): List<AccountEdge> = transact {
        select { contactOwnerId eq ownerId }.map { AccountEdge(readUserById(it[contactId]), it[Contacts.id].value) }
    }

    /** @see [readIdList] */
    fun read(ownerId: String, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readRows(ownerId), pagination)

    /**
     * Case-insensitively [query]s the [ownerId]'s contacts' usernames, first names, last names, and email addresses.
     */
    fun search(ownerId: String, query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val rows = readRows(ownerId).filter { it.node.matches(query) }
        return AccountsConnection.build(rows, pagination)
    }

    /**
     * Deletes the [ownerId]'s contacts from the [contactIdList], ignoring nonexistent contacts. The [ownerId] will be
     * notified of the [DeletedContact]s if they've [Broker.subscribe]d via [contactsBroker].
     */
    fun delete(ownerId: String, contactIdList: List<String>) {
        val contacts = readIdList(ownerId).intersect(contactIdList)
        transact {
            deleteWhere { (contactOwnerId eq ownerId) and (contactId inList contacts) }
        }
        contacts.forEach { contact ->
            contactsBroker.notify(DeletedContact(contact)) { it.userId == ownerId }
        }
    }

    /**
     * Deletes every contact who owns, or is owned by, the given [userId]. Clients who have [Broker.subscribe]d
     * via [contactsBroker], and have the [userId] in their contacts, will be notified of the [DeletedContact].
     */
    fun deleteUserEntries(userId: String) {
        contactsBroker.notify(DeletedContact(userId)) { userId in readIdList(it.userId) }
        transact {
            deleteWhere { (contactOwnerId eq userId) or (contactId eq userId) }
        }
    }
}