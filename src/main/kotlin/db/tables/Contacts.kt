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
     * Saves the [ownerId]'s [contactId].
     *
     * If the [contactId] was either previously saved or a nonexistent user, then `false` will be returned. Otherwise,
     * the [ownerId] will be notified of the [NewContact] if they've [Notifier.subscribe]d via [accountsNotifier], and
     * `true` will be returned.
     */
    fun create(ownerId: Int, contactId: Int): Boolean {
        if (hasContact(ownerId, contactId)) return false
        transaction {
            insert {
                it[contactOwnerId] = ownerId
                it[this.contactId] = contactId
            }
        }
        accountsNotifier.publish(NewContact.build(contactId), ownerId)
        return true
    }

    private fun hasContact(ownerId: Int, contactId: Int): Boolean = transaction {
        select { (contactOwnerId eq ownerId) and (Contacts.contactId eq contactId) }.empty().not()
    }

    /** Returns the ID of every user who has the [contactId] in their contacts. */
    fun readOwners(contactId: Int): Set<Int> = transaction {
        select { Contacts.contactId eq contactId }.map { it[contactOwnerId] }.toSet()
    }

    /**
     * The user ID list of the contacts saved by the contact [ownerId].
     *
     * @see [read]
     */
    fun readIdList(ownerId: Int): Set<Int> = transaction {
        select { contactOwnerId eq ownerId }.map { it[contactId] }.toSet()
    }

    /** The [ownerId]'s contacts. */
    private fun readRows(ownerId: Int): Set<AccountEdge> = transaction {
        select { contactOwnerId eq ownerId }
            .map {
                val account = Users.read(it[contactId]).toAccount()
                AccountEdge(account, cursor = it[Contacts.id].value)
            }
            .toSet()
    }

    /** @see [readIdList] */
    fun read(ownerId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readRows(ownerId), pagination)

    /**
     * Case-insensitively [query]s the [ownerId]'s contacts' usernames, first names, last names, and email addresses.
     */
    fun search(ownerId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val rows = readRows(ownerId).filter { it.node.matches(query) }.toSet()
        return AccountsConnection.build(rows, pagination)
    }

    /**
     * Deletes the [ownerId]'s [contactId].
     *
     * If the [contactId] either wasn't saved or is a nonexistent user, then `false` will be returned. Otherwise, the
     * [ownerId] will be notified of the [DeletedContact]s if they've [Notifier.subscribe]d via [accountsNotifier], and
     * `true` will be returned.
     */
    fun delete(ownerId: Int, contactId: Int): Boolean {
        val count = transaction {
            deleteWhere { (contactOwnerId eq ownerId) and (Contacts.contactId eq contactId) }
        }
        return if (count == 0) false
        else {
            accountsNotifier.publish(DeletedContact(contactId), ownerId)
            true
        }
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
