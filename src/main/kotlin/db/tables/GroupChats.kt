package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * The chat's pic cannot exceed [Pic.MAX_BYTES].
 *
 * @see [GroupChatUsers]
 * @see [Messages]
 */
object GroupChats : Table() {
    override val tableName get() = "group_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val title: Column<String> = varchar("title", GroupChatTitle.MAX_LENGTH)
    private val description: Column<String> = varchar("description", GroupChatDescription.MAX_LENGTH)
    private val picId: Column<Int?> = integer("pic_id").references(Pics.id).nullable()
    private val isBroadcast: Column<Boolean> = bool("is_broadcast")
    private val inviteCode: Column<UUID> =
        uuid("invite_code").uniqueIndex().defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))

    /**
     * Returns the [chat]'s ID after creating it.
     *
     * Notifies the [GroupChatInput.userIdList] of the [GroupChatId] via [newGroupChatsNotifier].
     */
    fun create(chat: GroupChatInput): Int {
        val chatId = transaction {
            insert {
                it[id] = Chats.create()
                it[title] = chat.title.value
                it[description] = chat.description.value
                it[isBroadcast] = chat.isBroadcast
            }[GroupChats.id]
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList)
        GroupChatUsers.makeAdmins(chatId, chat.adminIdList, shouldNotify = false)
        return chatId
    }

    /**
     * Returns the [chatId] for the [userId].
     *
     * @see [readChatInfo]
     */
    fun readChat(
        userId: Int,
        chatId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat {
        val row = transaction {
            select { GroupChats.id eq chatId }.first()
        }
        return buildGroupChat(row, userId, chatId, usersPagination, messagesPagination)
    }

    /** @see [readChat] */
    fun readChatInfo(inviteCode: UUID, usersPagination: ForwardPagination? = null): GroupChatInfo {
        val row = transaction {
            select { GroupChats.inviteCode eq inviteCode }.first()
        }
        return GroupChatInfo(
            GroupChatUsers.readAdminIdList(row[id]),
            GroupChatUsers.readUsers(row[id], usersPagination),
            row[title].let(::GroupChatTitle),
            row[description].let(::GroupChatDescription),
            row[isBroadcast]
        )
    }

    /**
     * Returns the [userId]'s chats.
     *
     * @see [GroupChatUsers.readChatIdList]
     */
    fun readUserChats(
        userId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transaction {
        GroupChatUsers.readChatIdList(userId).map { readChat(userId, it, usersPagination, messagesPagination) }
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [updatedChatsNotifier]. */
    fun updateTitle(chatId: Int, title: GroupChatTitle) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.title] = title.value }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::UpdatedChatsAsset)
        updatedChatsNotifier.publish(UpdatedGroupChat(chatId, title), subscribers)
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [updatedChatsNotifier]. */
    fun updateDescription(chatId: Int, description: GroupChatDescription) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.description] = description.value }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::UpdatedChatsAsset)
        updatedChatsNotifier.publish(UpdatedGroupChat(chatId, description = description), subscribers)
    }

    /**
     * Deletes the [pic] if it's `null`. Notifies subscribers of the [UpdatedGroupChat] via [updatedChatsNotifier].
     *
     * @see [update]
     */
    fun updatePic(chatId: Int, pic: Pic?) {
        transaction {
            val op = GroupChats.id eq chatId
            update({ op }) { it[this.picId] = null }
            val picId = select(op).first()[picId]
            update({ op }) { it[this.picId] = Pics.update(picId, pic) }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::UpdatedChatsAsset)
        updatedChatsNotifier.publish(UpdatedGroupChat(chatId), subscribers)
    }

    fun readPic(chatId: Int): Pic? = transaction {
        select { GroupChats.id eq chatId }.first()[picId]
    }?.let(Pics::read)

    /**
     * Deletes the [chatId] from [Chats], [GroupChats], [TypingStatuses], [Messages], and [MessageStatuses]. Clients who
     * have [Notifier.subscribe]d to [MessagesSubscription]s via [messagesNotifier] will receive a [DeletionOfEveryMessage].
     *
     * An [IllegalArgumentException] will be thrown if the [chatId] has users in it.
     */
    fun delete(chatId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userIdList.isNotEmpty())
            throw IllegalArgumentException("The chat (ID: $chatId) is not empty (users: $userIdList).")
        TypingStatuses.deleteChat(chatId)
        Messages.deleteChat(chatId)
        transaction {
            deleteWhere { GroupChats.id eq chatId }
        }
        Chats.delete(chatId)
    }

    /** Case-insensitively [query]s the title of every chat the [userId] is in. */
    fun search(
        userId: Int,
        query: String,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transaction {
        select { (GroupChats.id inList GroupChatUsers.readChatIdList(userId)) and (title iLike query) }
            .map { buildGroupChat(it, userId, it[GroupChats.id], usersPagination, messagesPagination) }
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [updatedChatsNotifier]. */
    fun setBroadcastStatus(chatId: Int, isBroadcast: Boolean) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.isBroadcast] = isBroadcast }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::UpdatedChatsAsset)
        updatedChatsNotifier.publish(UpdatedGroupChat(chatId, isBroadcast = isBroadcast), subscribers)
    }

    /** Builds the [chatId] from the [row] for the [userId]. */
    private fun buildGroupChat(
        row: ResultRow,
        userId: Int,
        chatId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat = GroupChat(
        chatId,
        GroupChatUsers.readAdminIdList(chatId),
        GroupChatUsers.readUsers(chatId, usersPagination),
        GroupChatTitle(row[title]),
        GroupChatDescription(row[description]),
        Messages.readGroupChatConnection(userId, chatId, messagesPagination),
        row[isBroadcast],
        row[inviteCode].takeIf { GroupChatUsers.isAdmin(userId, chatId) }
    )

    fun isExistentInviteCode(inviteCode: UUID): Boolean = transaction {
        select { GroupChats.inviteCode eq inviteCode }.empty().not()
    }

    /** @see [isExistentInviteCode] */
    fun readInviteCode(chatId: Int): UUID = transaction {
        select { GroupChats.id eq chatId }.first()[inviteCode]
    }

    /** Returns the ID of the chat having the [inviteCode]. */
    fun readChatFromInvite(inviteCode: UUID): Int = transaction {
        select { GroupChats.inviteCode eq inviteCode }.first()[GroupChats.id]
    }

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in. Only chats having messages matching the
     * [query] will be returned. Only the matched message [ChatEdges.edges] will be returned.
     */
    fun queryUserChatEdges(userId: Int, query: String): List<ChatEdges> = GroupChatUsers.readChatIdList(userId)
        .associateWith { Messages.searchGroupChat(userId, it, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }
}