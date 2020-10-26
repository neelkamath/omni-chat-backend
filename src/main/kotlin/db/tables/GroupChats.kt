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
    private val publicity: Column<GroupChatPublicity> = customEnumeration(
            name = "publicity",
            sql = "group_chat_publicity",
            fromDb = { GroupChatPublicity.valueOf((it as String).toUpperCase()) },
            toDb = { PostgresEnum("group_chat_publicity", it) }
    )
    private val inviteCode: Column<UUID> =
            uuid("invite_code").uniqueIndex().defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))

    /**
     * Returns the [chat]'s ID after creating it.
     *
     * Notifies the [GroupChatInput.userIdList] of the [GroupChatId] via [groupChatsNotifier].
     */
    fun create(chat: GroupChatInput): Int {
        val chatId = transaction {
            insert {
                it[id] = Chats.create()
                it[title] = chat.title.value
                it[description] = chat.description.value
                it[isBroadcast] = chat.isBroadcast
                it[publicity] = chat.publicity
            }[GroupChats.id]
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList)
        GroupChatUsers.makeAdmins(chatId, chat.adminIdList, shouldNotify = false)
        return chatId
    }

    fun exists(chatId: Int): Boolean = transaction {
        select { GroupChats.id eq chatId }.empty().not()
    }

    /** Whether the [chatId] exists, and it's public. */
    fun isExistentPublicChat(chatId: Int): Boolean = exists(chatId) &&
            readChatInfo(chatId, usersPagination = ForwardPagination(first = 0)).publicity == GroupChatPublicity.PUBLIC

    /**
     * Returns the [chatId] for the [userId], or for an anonymous user if there's no [userId].
     *
     * @see [readChatInfo]
     */
    fun readChat(
            chatId: Int,
            usersPagination: ForwardPagination? = null,
            messagesPagination: BackwardPagination? = null,
            userId: Int? = null
    ): GroupChat {
        val row = transaction {
            select { GroupChats.id eq chatId }.first()
        }
        return buildGroupChat(row, usersPagination, messagesPagination, userId)
    }

    /** @see [readChat] */
    fun readChatInfo(inviteCode: UUID, usersPagination: ForwardPagination? = null): GroupChatInfo {
        val row = transaction {
            select { GroupChats.inviteCode eq inviteCode }.first()
        }
        return buildChatInfo(row, usersPagination)
    }

    private fun readChatInfo(chatId: Int, usersPagination: ForwardPagination? = null): GroupChatInfo {
        val row = transaction {
            select { GroupChats.id eq chatId }.first()
        }
        return buildChatInfo(row, usersPagination)
    }

    private fun buildChatInfo(row: ResultRow, usersPagination: ForwardPagination? = null): GroupChatInfo =
            GroupChatInfo(
                    GroupChatUsers.readAdminIdList(row[id]),
                    GroupChatUsers.readUsers(row[id], usersPagination),
                    row[title].let(::GroupChatTitle),
                    row[description].let(::GroupChatDescription),
                    row[isBroadcast],
                    row[publicity]
            )

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
        GroupChatUsers.readChatIdList(userId).map { readChat(it, usersPagination, messagesPagination, userId) }
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun updateTitle(chatId: Int, title: GroupChatTitle) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.title] = title.value }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::GroupChatsAsset)
        groupChatsNotifier.publish(UpdatedGroupChat(chatId, title), subscribers)
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun updateDescription(chatId: Int, description: GroupChatDescription) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.description] = description.value }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::GroupChatsAsset)
        groupChatsNotifier.publish(UpdatedGroupChat(chatId, description = description), subscribers)
    }

    /**
     * Deletes the [pic] if it's `null`. Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier].
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
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::GroupChatsAsset)
        groupChatsNotifier.publish(UpdatedGroupChat(chatId), subscribers)
    }

    fun readPic(chatId: Int): Pic? = transaction {
        select { GroupChats.id eq chatId }.first()[picId]
    }?.let(Pics::read)

    /**
     * Deletes the [chatId] from [Chats], [GroupChats], [TypingStatuses], [Messages], and [MessageStatuses]. Clients who
     * have [Notifier.subscribe]d to [MessagesSubscription]s via [messagesNotifier] will receive a
     * [DeletionOfEveryMessage].
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
                .map { buildGroupChat(it, usersPagination, messagesPagination, userId) }
    }

    /** Case-insensitively [query]s public chats. */
    fun searchPublicChats(
            query: String,
            usersPagination: ForwardPagination? = null,
            messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transaction {
        select { (publicity eq GroupChatPublicity.PUBLIC) and (title iLike query) }
                .map { buildGroupChat(it, usersPagination, messagesPagination) }
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun setBroadcastStatus(chatId: Int, isBroadcast: Boolean) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.isBroadcast] = isBroadcast }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::GroupChatsAsset)
        groupChatsNotifier.publish(UpdatedGroupChat(chatId, isBroadcast = isBroadcast), subscribers)
    }

    /**
     * Throws an [IllegalArgumentException] if the [chatId] is public. Subscribers are notified of the
     * [UpdatedGroupChat] via [groupChatsNotifier].
     */
    fun setInvitability(chatId: Int, isInvitable: Boolean) {
        if (isExistentPublicChat(chatId))
            throw IllegalArgumentException("A public chat's invitability cannot be updated.")
        val publicity = if (isInvitable) GroupChatPublicity.INVITABLE else GroupChatPublicity.NOT_INVITABLE
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.publicity] = publicity }
        }
        val subscribers = GroupChatUsers.readUserIdList(chatId).map(::GroupChatsAsset)
        groupChatsNotifier.publish(UpdatedGroupChat(chatId, publicity = publicity), subscribers)
    }

    /** Builds the chat from the [row] for the [userId], or an anonymous user if there's no [userId]. */
    private fun buildGroupChat(
            row: ResultRow,
            usersPagination: ForwardPagination? = null,
            messagesPagination: BackwardPagination? = null,
            userId: Int? = null
    ): GroupChat = GroupChat(
            row[id],
            GroupChatUsers.readAdminIdList(row[id]),
            GroupChatUsers.readUsers(row[id], usersPagination),
            GroupChatTitle(row[title]),
            GroupChatDescription(row[description]),
            Messages.readGroupChatConnection(row[id], messagesPagination, userId),
            row[isBroadcast],
            row[publicity],
            row[inviteCode].takeIf { row[publicity] != GroupChatPublicity.NOT_INVITABLE }
    )

    /** Returns `false` if the [chatId] doesn't exist, or isn't invitable. */
    fun isInvitable(chatId: Int): Boolean = transaction {
        val publicity = select { GroupChats.id eq chatId }.firstOrNull()?.get(publicity) ?: return@transaction false
        publicity != GroupChatPublicity.NOT_INVITABLE
    }

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
            .associateWith { Messages.searchGroupChat(it, query, userId = userId) }
            .filter { (_, edges) -> edges.isNotEmpty() }
            .map { (chatId, edges) -> ChatEdges(chatId, edges) }
}
