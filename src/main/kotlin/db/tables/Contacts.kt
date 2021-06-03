package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedContact
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.NewContact
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction

object Contacts : Table() {
    /** The ID of the user who has saved the [contactUserId]. */
    private val contactOwnerUserId: Column<Int> = integer("contact_owner_user_id").references(Users.id)

    /** The ID of the user whose contact has been saved by the [contactOwnerUserId]. */
    private val contactUserId: Column<Int> = integer("contact_user_id").references(Users.id)

    /**
     * Saves the [ownerUserId]'s [contactUserId].
     *
     * If the [contactUserId] was either previously saved or a non-existing user, then `false` will be returned.
     * Otherwise, the [ownerUserId] will be notified of the [NewContact] if they've [Notifier.subscribe]d via
     * [accountsNotifier], and `true` will be returned.
     */
    fun create(ownerUserId: Int, contactUserId: Int): Boolean {
        if (hasContact(ownerUserId, contactUserId)) return false
        transaction {
            insert {
                it[contactOwnerUserId] = ownerUserId
                it[this.contactUserId] = contactUserId
            }
        }
        accountsNotifier.publish(NewContact(contactUserId), UserId(ownerUserId))
        return true
    }

    private fun hasContact(ownerUserId: Int, contactUserId: Int): Boolean = transaction {
        select((contactOwnerUserId eq ownerUserId) and (Contacts.contactUserId eq contactUserId)).empty().not()
    }

    /** Returns the ID of every user who has the [contactUserId] in their contacts. */
    fun readOwnerUserIdList(contactUserId: Int): Set<Int> = transaction {
        select(Contacts.contactUserId eq contactUserId).map { it[contactOwnerUserId] }.toSet()
    }

    /**
     * Returns the ID of each user in the [ownerUserId]'s contacts. The returned user IDs are sorted in ascending order.
     */
    fun readIdList(ownerUserId: Int, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = contactOwnerUserId eq ownerUserId
        pagination?.after?.let { op = op and (contactUserId greater pagination.after) }
        return transaction {
            select(op)
                .orderBy(contactUserId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[contactUserId] }
                .toLinkedHashSet()
        }
    }

    /** Returns the [type] of [Cursor] for the [ownerUserId]'s contacts. */
    fun readCursor(ownerUserId: Int, type: CursorType): Cursor? {
        val order = when (type) {
            CursorType.START -> SortOrder.ASC
            CursorType.END -> SortOrder.DESC
        }
        return transaction {
            select(contactOwnerUserId eq ownerUserId)
                .orderBy(contactUserId, order)
                .limit(1)
                .firstOrNull()
                ?.get(contactUserId)
        }
    }

    /**
     * Case-insensitively [query]s the [ownerUserId]'s contacts' usernames, first names, last names, and email
     * addresses. The user IDs returned are sorted in ascending order.
     */
    fun search(ownerUserId: Int, query: String): LinkedHashSet<Int> {
        val userIdList = transaction {
            select(contactOwnerUserId eq ownerUserId).orderBy(contactUserId).map { it[contactUserId] }.toLinkedHashSet()
        }
        return searchUsers(userIdList, query)
    }

    /**
     * Deletes the [ownerUserId]'s [contactUserId].
     *
     * If the [contactUserId] either wasn't saved or is a non-existing user, then `false` will be returned. Otherwise,
     * the [ownerUserId] will be notified of the [DeletedContact]s if they've [Notifier.subscribe]d via
     * [accountsNotifier], and `true` will be returned.
     */
    fun delete(ownerUserId: Int, contactUserId: Int): Boolean {
        val count = transaction {
            deleteWhere { (contactOwnerUserId eq ownerUserId) and (Contacts.contactUserId eq contactUserId) }
        }
        return if (count == 0) false
        else {
            accountsNotifier.publish(DeletedContact(contactUserId), UserId(ownerUserId))
            true
        }
    }

    /**
     * If the [userId] doesn't exist, nothing will happen. Otherwise, every contact who owns, or is owned by, the given
     * [userId] is deleted, and subscribers who have the [userId] in their contacts will be notified of the
     * [DeletedContact] via [accountsNotifier].
     */
    fun deleteUserEntries(userId: Int) {
        accountsNotifier.publish(DeletedContact(userId), readOwnerUserIdList(userId).map(::UserId))
        transaction {
            deleteWhere { (contactOwnerUserId eq userId) or (contactUserId eq userId) }
        }
    }
}
