package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.GroupChatUpdate
import com.neelkamath.omniChat.db.GroupChats.adminUserId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update

data class GroupChatWithId(val id: Int, val chat: GroupChat)

/** The [GroupChatUsers] table contains the participants, including the [adminUserId], of a particular chat. */
object GroupChats : IntIdTable() {
    override val tableName get() = "group_chats"
    private val adminUserId = varchar("admin_user_id", Auth.userIdLength)
    const val maxTitleLength = 70
    val title = varchar("title", maxTitleLength)
    const val maxDescriptionLength = 1000
    val description = varchar("description", maxDescriptionLength).nullable()

    fun chatIdExists(id: Int): Boolean = Db.transact {
        !select { GroupChats.id eq id }.empty()
    }

    /** Whether the [userId] is the admin of [chatId] (assumed to exist). */
    fun isAdmin(userId: String, chatId: Int): Boolean = Db.transact {
        select { id eq chatId }.first()[adminUserId] == userId
    }

    /** Returns the chat ID after creating it. */
    fun create(adminUserId: String, chat: GroupChat): Int = Db.transact {
        val groupId = insertAndGetId {
            it[this.adminUserId] = adminUserId
            it[title] = chat.title
            it[description] = chat.description
        }.value
        GroupChatUsers.create(groupId, chat.userIdList + adminUserId)
        groupId
    }

    fun readCreated(adminUserId: String): List<GroupChatWithId> = Db.transact {
        GroupChats.select { GroupChats.adminUserId eq adminUserId }.map {
            val userIdList = GroupChatUsers.readUserIdList(it[id].value)
            GroupChatWithId(it[id].value, GroupChat(userIdList, it[title], it[description]))
        }
    }

    fun update(update: GroupChatUpdate): Unit = Db.transact {
        update({ id eq update.chatId }) { statement: UpdateStatement ->
            update.title?.let { statement[title] = it }
            update.description?.let { statement[description] = it }
        }
        update.newUserIdList?.let { GroupChatUsers.addUsers(update.chatId, it) }
        update.removedUserIdList?.let { GroupChatUsers.removeUsers(update.chatId, it) }
    }

    /**
     * Searches the chats the [userId]'s is in.
     *
     * Returns the chat ID list by searching for the [query] in every chat's title case-insensitively.
     */
    fun search(userId: String, query: String): List<Int> = Db.transact {
        selectAll()
            .filter { it[id].value in GroupChatUsers.getChatIdList(userId) }
            .filter { query.toLowerCase() in it[title].toLowerCase() }
            .map { it[id].value }
    }
}