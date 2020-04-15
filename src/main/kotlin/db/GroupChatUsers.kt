package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select

object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val userId = varchar("user_id", Auth.userIdLength)
    private val groupChatId = integer("group_chat_id").references(GroupChats.id)

    private fun isUserInChat(groupChatId: Int, userId: String): Boolean = Db.transact {
        !select { (GroupChatUsers.groupChatId eq groupChatId) and (GroupChatUsers.userId eq userId) }.empty()
    }

    fun create(groupId: Int, userIdList: Set<String>): Unit = Db.transact {
        batchInsert(userIdList) {
            this[userId] = it
            this[groupChatId] = groupId
        }
    }

    /** Returns the user ID list from the specified [groupChatId]. */
    fun readUserIdList(groupChatId: Int): Set<String> = Db.transact {
        select { GroupChatUsers.groupChatId eq groupChatId }.map { it[userId] }.toSet()
    }

    /** Adds every user in the [userIdList] to the chat (specified by the [groupChatId]) if they aren't in it. */
    fun addUsers(groupChatId: Int, userIdList: Set<String>): Unit = Db.transact {
        batchInsert(userIdList.filterNot { isUserInChat(groupChatId, it) }) {
            this[GroupChatUsers.groupChatId] = groupChatId
            this[userId] = it
        }
    }

    /** Removes every user in the [userIdList] in the chat (specified by the [groupChatId]) if they're in it.  */
    fun removeUsers(groupChatId: Int, userIdList: Set<String>): Unit = Db.transact {
        deleteWhere { (GroupChatUsers.groupChatId eq groupChatId) and (userId inList userIdList) }
    }

    /** Returns the chat ID list of every chat the [userId] is in. */
    fun getChatIdList(userId: String): List<Int> = Db.transact {
        select { GroupChatUsers.userId eq userId }.map { it[groupChatId] }
    }
}