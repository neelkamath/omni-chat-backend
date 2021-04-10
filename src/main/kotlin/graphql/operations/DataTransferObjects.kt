@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.operations

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.BlockedUsers
import com.neelkamath.omniChatBackend.db.tables.Stargazers
import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.graphql.routing.Account
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.StarredMessage
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

/**
 * @param messages The [Stargazers.MessageCursor]s used to retrieve the edges for this connection. They must be sorted
 * in ascending order of their [Stargazers.MessageCursor.cursor].
 * @param pagination The pagination used to retrieve the [messages].
 */
class StarredMessagesConnectionDto(
    private val messages: LinkedHashSet<Stargazers.MessageCursor>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<StarredMessageEdgeDto> = messages.map { StarredMessageEdgeDto(it.messageId) }

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo =
        Stargazers.readPageInfo(env.userId!!, messages.lastOrNull()?.cursor, pagination)
}

class StarredMessageEdgeDto(private val messageId: Int) {
    fun getCursor(): Int = messageId

    fun getNode(env: DataFetchingEnvironment): StarredMessage = StarredMessage.build(env.userId!!, messageId)
}

/**
 * @param blockedUsers The [BlockedUsers.BlockedUserCursor]s used to retrieve the edges for this connection. They must
 * be sorted in ascending order of their [BlockedUsers.BlockedUserCursor.cursor].
 * @param pagination The pagination used to retrieve the [blockedUsers].
 */
class AccountsConnectionDto(
    private val blockedUsers: LinkedHashSet<BlockedUsers.BlockedUserCursor>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<AccountEdgeDto> = blockedUsers.map { AccountEdgeDto(it.blockedUserId) }

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo =
        BlockedUsers.readPageInfo(env.userId!!, blockedUsers.lastOrNull()?.cursor, pagination)
}

class AccountEdgeDto(private val userId: Int) {
    fun getCursor(): Int = userId

    fun getNode(): Account = Users.read(userId).toAccount()
}
