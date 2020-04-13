package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select

object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    val userId = varchar("user_id", Auth.userIdLength)
    val groupChatId = integer("group_chat_id").references(GroupChats.id)

    fun create(groupId: Int, userIdList: Set<String>): Unit = DB.transact {
        batchInsert(userIdList) {
            this[userId] = it
            this[groupChatId] = groupId
        }
    }

    /** Returns the user ID list from the specified [groupChatId]. */
    fun readUserIdList(groupChatId: Int): Set<String> = DB.transact {
        select { GroupChatUsers.groupChatId eq groupChatId }.map { it[userId] }.toSet()
    }
}