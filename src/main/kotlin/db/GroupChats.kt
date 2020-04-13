package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.GroupChat
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId

object GroupChats : IntIdTable() {
    override val tableName get() = "group_chats"
    val adminUserId = varchar("admin_user_id", Auth.userIdLength)
    const val maxTitleLength = 70
    val title = varchar("title", maxTitleLength)
    const val maxDescriptionLength = 1000
    val description = varchar("description", maxDescriptionLength).nullable()

    fun create(adminUserId: String, chat: GroupChat): Unit = DB.transact {
        val groupId = insertAndGetId {
            it[this.adminUserId] = adminUserId
            it[title] = chat.title
            it[description] = chat.description
        }
        GroupChatUsers.create(groupId.value, chat.userIdList)
    }
}