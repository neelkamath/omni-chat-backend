package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.GroupChat
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

data class GroupChatWithId(val id: Int, val chat: GroupChat)

object GroupChats : IntIdTable() {
    override val tableName get() = "group_chats"
    val adminUserId = varchar("admin_user_id", Auth.userIdLength)
    const val maxTitleLength = 70
    val title = varchar("title", maxTitleLength)
    const val maxDescriptionLength = 1000
    val description = varchar("description", maxDescriptionLength).nullable()

    /** Returns the chat ID after creating it. */
    fun create(adminUserId: String, chat: GroupChat): Int = DB.transact {
        val groupId = insertAndGetId {
            it[this.adminUserId] = adminUserId
            it[title] = chat.title
            it[description] = chat.description
        }.value
        GroupChatUsers.create(groupId, chat.userIdList)
        groupId
    }

    fun read(adminUserId: String): List<GroupChatWithId> = DB.transact {
        GroupChats.select { GroupChats.adminUserId eq adminUserId }.map {
            val userIdList = GroupChatUsers.readUserIdList(it[id].value)
            GroupChatWithId(it[id].value, GroupChat(userIdList, it[title], it[description]))
        }
    }
}