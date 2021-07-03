@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Chats
import com.neelkamath.omniChatBackend.db.tables.GroupChatUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatDescription
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatTitle
import graphql.schema.DataFetchingEnvironment

sealed interface BareGroupChat {
    /** The [Chats.id]. */
    val id: Int

    fun getChatId(): Int = id

    fun getTitle(): GroupChatTitle = GroupChats.readTitle(id)

    fun getDescription(): GroupChatDescription = GroupChats.readDescription(id)

    fun getAdminIdList(): List<Int> = GroupChatUsers.readAdminIdList(id).toList()

    fun getUsers(env: DataFetchingEnvironment): AccountsConnection {
        val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
        val userIdList = GroupChatUsers.readUserIdList(id, pagination)
        val startCursor = Messages.readGroupChatCursor(id, CursorType.START)
        val endCursor = Messages.readGroupChatCursor(id, CursorType.END)
        return AccountsConnection(startCursor, endCursor, userIdList, pagination)
    }

    fun getIsBroadcast(): Boolean = GroupChats.isBroadcastChat(id)

    fun getPublicity(): GroupChatPublicity = GroupChats.readPublicity(id)
}
