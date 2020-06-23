package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts.contactId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.keycloak.representations.idm.UserRepresentation

object Contacts : IntIdTable() {
    /** The ID of the user who has saved the [contactId]. */
    private val contactOwnerId: Column<String> = varchar("contact_owner_id", USER_ID_LENGTH)

    /** The ID of the user whose contact has been saved by the [contactId]. */
    private val contactId: Column<String> = varchar("contact_id", USER_ID_LENGTH)

    /** Saves the [ownerId]'s [contacts], ignoring existing contacts. */
    fun create(ownerId: String, contacts: Set<String>) {
        val existingContacts = readIdList(ownerId)
        transact {
            batchInsert(contacts.filterNot { it in existingContacts }) {
                this[contactOwnerId] = ownerId
                this[contactId] = it
            }
        }
    }

    /** Returns the user ID list of the contacts saved by the contact [ownerId]. */
    fun readIdList(ownerId: String): List<String> = transact {
        select { contactOwnerId eq ownerId }.map { it[contactId] }
    }

    /** @return the [ownerId]'s contacts. */
    private fun readRows(ownerId: String): List<AccountEdge> = transact {
        select { contactOwnerId eq ownerId }.map { AccountEdge(findUserById(it[contactId]), it[Contacts.id].value) }
    }

    /** @see [readIdList] */
    fun read(ownerId: String, pagination: ForwardPagination? = null): AccountsConnection =
        buildAccountsConnection(readRows(ownerId), pagination)

    /**
     * Case-insensitively [query]s the [ownerId]'s contacts' usernames, first names, last names, and email addresses.
     */
    fun search(ownerId: String, query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val rows = readRows(ownerId).filter { it.node.matches(query) }
        return buildAccountsConnection(rows, pagination)
    }

    /** Deletes the [ownerId]'s contacts from the [contactIdList], ignoring nonexistent contacts. */
    fun delete(ownerId: String, contactIdList: List<String>): Unit = transact {
        deleteWhere { (contactOwnerId eq ownerId) and (contactId inList contactIdList) }
    }

    /**
     * Case-insensitively searches for the [query] in the [UserRepresentation.username], [UserRepresentation.firstName],
     * [UserRepresentation.lastName], and [UserRepresentation.email].
     */
    private fun Account.matches(query: String): Boolean =
        listOfNotNull(username, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }

    /** Deletes every contact who owns, or is owned by, the given [userId]. */
    fun deleteUserEntries(userId: String): Unit = transact {
        deleteWhere { (contactOwnerId eq userId) or (contactId eq userId) }
    }
}