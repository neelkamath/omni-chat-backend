package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.Notifier
import com.neelkamath.omniChat.db.accountsNotifier
import com.neelkamath.omniChat.graphql.routing.AccountEdge
import com.neelkamath.omniChat.graphql.routing.AccountsConnection
import com.neelkamath.omniChat.graphql.routing.DeletedContact
import com.neelkamath.omniChat.graphql.routing.NewContact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Contacts : IntIdTable() {
    /** The ID of the user who has saved the [contactId]. */
    private val contactOwnerId: Column<Int> = integer("contact_owner_id").references(Users.id)

    /** The ID of the user whose contact has been saved by the [contactOwnerId]. */
    private val contactId: Column<Int> = integer("contact_id").references(Users.id)

    /**
     * Saves the [ownerId]'s [contactIdList], ignoring existing contacts. The [ownerId] will be notified of the
     * [NewContact] if they've [Notifier.subscribe]d via [accountsNotifier].
     */
    fun create(ownerId: Int, contactIdList: Collection<Int>) {
        val existingContacts = readIdList(ownerId)
        val newContacts = contactIdList.filter { it !in existingContacts }.toSet()
        transaction {
            batchInsert(newContacts) {
                this[contactOwnerId] = ownerId
                this[contactId] = it
            }
        }
        for (newContact in newContacts) accountsNotifier.publish(NewContact.build(newContact), ownerId)
    }

    /** Returns the ID of every user who has the [contactId] in their contacts. */
    fun readOwners(contactId: Int): List<Int> = transaction {
        select { Contacts.contactId eq contactId }.map { it[contactOwnerId] }
    }

    /**
     * The user ID list of the contacts saved by the contact [ownerId].
     *
     * @see [read]
     */
    fun readIdList(ownerId: Int): List<Int> = transaction {
        select { contactOwnerId eq ownerId }.map { it[contactId] }
    }

    /** The [ownerId]'s contacts. */
    private fun readRows(ownerId: Int): List<AccountEdge> = transaction {
        select { contactOwnerId eq ownerId }.map {
            val account = Users.read(it[contactId]).toAccount()
            AccountEdge(account, cursor = it[Contacts.id].value)
        }
    }

    /** @see [readIdList] */
    fun read(ownerId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readRows(ownerId), pagination)

    /**
     * Case-insensitively [query]s the [ownerId]'s contacts' usernames, first names, last names, and email addresses.
     */
    fun search(ownerId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val rows = readRows(ownerId).filter { it.node.matches(query) }
        return AccountsConnection.build(rows, pagination)
    }

    /**
     * Deletes the [ownerId]'s contacts from the [contactIdList], ignoring nonexistent contacts. The [ownerId] will be
     * notified of the [DeletedContact]s if they've [Notifier.subscribe]d via [accountsNotifier].
     */
    fun delete(ownerId: Int, contactIdList: List<Int>) {
        val contacts = readIdList(ownerId).intersect(contactIdList)
        transaction {
            deleteWhere { (contactOwnerId eq ownerId) and (contactId inList contacts) }
        }
        for (contact in contacts) accountsNotifier.publish(DeletedContact(contact), ownerId)
    }

    /**
     * If the [userId] doesn't exist, nothing will happen. Otherwise, every contact who owns, or is owned by, the given
     * [userId] is deleted, and subscribers who have the [userId] in their contacts will be notified of the
     * [DeletedContact] via [accountsNotifier].
     */
    fun deleteUserEntries(userId: Int) {
        accountsNotifier.publish(DeletedContact(userId), readOwners(userId))
        transaction {
            deleteWhere { (contactOwnerId eq userId) or (contactId eq userId) }
        }
    }
}
