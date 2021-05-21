package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.accountsNotifier
import com.neelkamath.omniChatBackend.db.searchUsers
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.BlockedAccount
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnblockedAccount
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction

/** The users each user has blocked. */
object BlockedUsers : Table() {
    override val tableName = "blocked_users"
    private val blockerUserId: Column<Int> = integer("blocker_user_id").references(Users.id)
    private val blockedUserId: Column<Int> = integer("blocked_user_id").references(Users.id)

    /**
     * Has the [blockerUserId] block the [blockedUserId] if they haven't already.
     *
     * The [blockerUserId] will be notified of the [BlockedAccount] via [accountsNotifier] unless the [blockedUserId]
     * was already blocked. Does nothing if the [blockerUserId] is the [blockedUserId].
     */
    fun create(blockerUserId: Int, blockedUserId: Int) {
        if (blockerUserId == blockedUserId || isBlocked(blockerUserId, blockedUserId)) return
        transaction {
            insert {
                it[this.blockerUserId] = blockerUserId
                it[this.blockedUserId] = blockedUserId
            }
        }
        accountsNotifier.publish(blockerUserId to BlockedAccount(blockedUserId))
    }

    /**
     * Returns the IDs of users blocked by the [blockerUserId] as per the [pagination]. The returned user IDs returned
     * are sorted in ascending order.
     */
    fun readBlockedUserIdList(blockerUserId: Int, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = BlockedUsers.blockerUserId eq blockerUserId
        if (pagination?.after != null) op = op and (blockedUserId greater pagination.after)
        return transaction {
            select(op)
                .orderBy(blockedUserId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[blockedUserId] }
                .toLinkedHashSet()
        }
    }

    /** Reads the [type] of cursor for the [blockerUserId] (`null` if the [blockerUserId] hasn't blocked any users). */
    fun readCursor(blockerUserId: Int, type: CursorType): Cursor? {
        val order = when (type) {
            CursorType.END -> SortOrder.DESC
            CursorType.START -> SortOrder.ASC
        }
        return transaction {
            select(BlockedUsers.blockerUserId eq blockerUserId)
                .orderBy(blockedUserId, order)
                .limit(1)
                .firstOrNull()
                ?.get(blockedUserId)
        }
    }

    /**
     * Case-insensitively [query]s each user's username, first name, last name, and email address which the [userId]
     * has blocked. The user IDs are returned in ascending order.
     */
    fun search(userId: Int, query: String): LinkedHashSet<Int> {
        val userIdList = transaction {
            select(blockerUserId eq userId).orderBy(blockedUserId).map { it[blockedUserId] }.toLinkedHashSet()
        }
        return searchUsers(userIdList, query)
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
