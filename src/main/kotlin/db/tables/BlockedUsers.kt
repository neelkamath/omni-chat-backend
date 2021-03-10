package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.accountsNotifier
import com.neelkamath.omniChat.graphql.routing.AccountEdge
import com.neelkamath.omniChat.graphql.routing.AccountsConnection
import com.neelkamath.omniChat.graphql.routing.BlockedAccount
import com.neelkamath.omniChat.graphql.routing.UnblockedAccount
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
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
        if (blockerUserId == blockedUserId || exists(blockerUserId, blockedUserId)) return
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
            selectAll()
                .filter { it[blockerUserId] == userId }
                .map { AccountEdge.build(it[blockedUserId], it[BlockedUsers.id].value) }
                .toSet()
        }
        return AccountsConnection.build(edges, pagination)
    }

    /** Whether the [blockerUserId] has blocked the [blockedUserId]. */
    fun exists(blockerUserId: Int, blockedUserId: Int): Boolean = transaction {
        select { (BlockedUsers.blockedUserId eq blockedUserId) and (BlockedUsers.blockerUserId eq blockerUserId) }
            .empty()
            .not()
    }

    /**
     * Has the [blockerUserId] unblock the [blockedUserId]. Does nothing if the user wasn't blocked. The [blockerUserId]
     * will be notified of the [UnblockedAccount] via [accountsNotifier].
     */
    fun delete(blockerUserId: Int, blockedUserId: Int): Unit = transaction {
        deleteWhere { (BlockedUsers.blockerUserId eq blockerUserId) and (BlockedUsers.blockedUserId eq blockedUserId) }
            .takeIf { it == 1 }
            ?.let { accountsNotifier.publish(blockerUserId to UnblockedAccount(blockedUserId)) }
    }

    /** Deletes every entry where the [userId] has either blocked or been blocked. */
    fun deleteUser(userId: Int): Unit = transaction {
        deleteWhere { (blockedUserId eq userId) or (blockerUserId eq userId) }
    }
}
