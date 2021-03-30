package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.accountsNotifier
import com.neelkamath.omniChat.graphql.routing.AccountEdge
import com.neelkamath.omniChat.graphql.routing.AccountsConnection
import com.neelkamath.omniChat.graphql.routing.BlockedAccount
import com.neelkamath.omniChat.graphql.routing.UnblockedAccount
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** The users each user has blocked. */
object BlockedUsers : IntIdTable() {
    override val tableName = "blocked_users"
    private val blockerUserId: Column<Int> = integer("blocker_user_id").references(Users.id)
    private val blockedUserId: Column<Int> = integer("blocked_user_id").references(Users.id)

    /**
     * Has the [blockerUserId] block the [blockedUserId] if they haven't already. The [blockerUserId] will be notified
     * of the [BlockedAccount] via [accountsNotifier]. Does nothing if the [blockerUserId] is the [blockedUserId].
     */
    fun create(blockerUserId: Int, blockedUserId: Int) {
        if (blockerUserId == blockedUserId || isBlocked(blockerUserId, blockedUserId)) return
        transaction {
            insert {
                it[this.blockerUserId] = blockerUserId
                it[this.blockedUserId] = blockedUserId
            }
        }
        accountsNotifier.publish(blockerUserId to BlockedAccount.build(blockedUserId))
    }

    /** Reads the list of users the [userId] has blocked. */
    fun read(userId: Int, pagination: ForwardPagination? = null): AccountsConnection {
        val edges = transaction {
            select(blockerUserId eq userId).map { AccountEdge.build(it[blockedUserId], it[BlockedUsers.id].value) }
        }
        return AccountsConnection.build(edges.toSet(), pagination)
    }

    /**
     * Case-insensitively [query]s each user's username, first name, last name, and email address which the [userId]
     * has blocked.
     */
    fun search(userId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val edges = transaction {
            select(blockerUserId eq userId).map { it[blockedUserId] }
        }
            .let(Users::readList)
            .filter {
                it.username.value.contains(query, ignoreCase = true) ||
                        it.firstName.value.contains(query, ignoreCase = true) ||
                        it.lastName.value.contains(query, ignoreCase = true) ||
                        it.emailAddress.contains(query, ignoreCase = true)
            }
            .map { AccountEdge(it.toAccount(), it.id) }
        return AccountsConnection.build(edges.toSet(), pagination)
    }

    /** Whether the [blockerUserId] has blocked the [blockedUserId]. */
    private fun isBlocked(blockerUserId: Int, blockedUserId: Int): Boolean = transaction {
        select((BlockedUsers.blockedUserId eq blockedUserId) and (BlockedUsers.blockerUserId eq blockerUserId))
            .empty()
            .not()
    }

    /**
     * Has the [blockerUserId] unblock the [blockedUserId].
     *
     * If the user either wasn't blocked or doesn't exist, then `false` will be returned. Otherwise, the [blockerUserId]
     * will be notified of the [UnblockedAccount] via [accountsNotifier], and `true` will be returned.
     */
    fun delete(blockerUserId: Int, blockedUserId: Int): Boolean {
        val count = transaction {
            deleteWhere {
                (BlockedUsers.blockerUserId eq blockerUserId) and (BlockedUsers.blockedUserId eq blockedUserId)
            }
        }
        return if (count == 0) false
        else {
            accountsNotifier.publish(blockerUserId to UnblockedAccount(blockedUserId))
            true
        }
    }

    /** Deletes every entry where the [userId] has either blocked or been blocked. */
    fun deleteUser(userId: Int): Unit = transaction {
        deleteWhere { (blockedUserId eq userId) or (blockerUserId eq userId) }
    }
}
