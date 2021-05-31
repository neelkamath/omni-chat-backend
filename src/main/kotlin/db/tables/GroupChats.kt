package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.GroupChatId
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChatPic
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * The chat's pic cannot exceed [Pic.ORIGINAL_MAX_BYTES].
 *
 * @see GroupChatUsers
 * @see Messages
 */
object GroupChats : Table() {
    override val tableName = "group_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val title: Column<String> = varchar("title", GroupChatTitle.MAX_LENGTH)
    private val description: Column<String> = varchar("description", GroupChatDescription.MAX_LENGTH)
    private val picId: Column<Int?> = integer("pic_id").references(Pics.id).nullable()
    private val isBroadcast: Column<Boolean> = bool("is_broadcast")
    private val publicity: Column<GroupChatPublicity> = customEnumeration(
        name = "publicity",
        sql = "group_chat_publicity",
        fromDb = { GroupChatPublicity.valueOf((it as String).uppercase()) },
        toDb = { PostgresEnum("group_chat_publicity", it) },
    )
    private val inviteCode: Column<UUID> =
        uuid("invite_code").uniqueIndex().defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))

    /**
     * Returns the newly created [chat]'s ID.
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

    fun readPublicity(chatId: Int): GroupChatPublicity =
        transaction { select(GroupChats.id eq chatId).first()[publicity] }

    fun isBroadcastChat(chatId: Int): Boolean = transaction { select(GroupChats.id eq chatId).first()[isBroadcast] }

    fun isExisting(chatId: Int): Boolean = transaction { select(GroupChats.id eq chatId).empty().not() }

    fun isExistingPublicChat(chatId: Int): Boolean = transaction {
        select((GroupChats.id eq chatId) and (publicity eq GroupChatPublicity.PUBLIC)).empty().not()
    }

    fun readTitle(chatId: Int): GroupChatTitle =
        transaction { select(GroupChats.id eq chatId).first()[title].let(::GroupChatTitle) }

    fun readDescription(chatId: Int): GroupChatDescription =
        transaction { select(GroupChats.id eq chatId).first()[description].let(::GroupChatDescription) }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun updateTitle(chatId: Int, title: GroupChatTitle) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.title] = title.value }
        }
        groupChatsNotifier.publish(UpdatedGroupChat(chatId, title), GroupChatUsers.readUserIdList(chatId).map(::UserId))
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun updateDescription(chatId: Int, description: GroupChatDescription) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.description] = description.value }
        }
        groupChatsNotifier.publish(
            UpdatedGroupChat(chatId, description = description),
            GroupChatUsers.readUserIdList(chatId).map(::UserId),
        )
    }

    /** Deletes the [pic] if it's `null`. Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun updatePic(chatId: Int, pic: Pic?) {
        transaction {
            // If the pic is to be deleted, its reference must be cleared before we can delete the referenced data.
            if (pic == null) update({ GroupChats.id eq chatId }) { it[picId] = null }
            val picId = select(GroupChats.id eq chatId).first()[picId]
            update({ GroupChats.id eq chatId }) { it[this.picId] = Pics.update(picId, pic) }
        }
        groupChatsNotifier.publish(UpdatedGroupChatPic(chatId), GroupChatUsers.readUserIdList(chatId).map(::UserId))
    }

    /** Returns the group chat's pic (`null` if there's no pic). */
    fun readPic(chatId: Int, type: PicType): ByteArray? {
        val picId = transaction { select(GroupChats.id eq chatId).first()[picId] } ?: return null
        return Pics.read(picId, type)
    }

    /**
     * Deletes the [chatId] from [Chats], [GroupChats], [TypingStatuses], [Messages], and [MessageStatuses].
     *
     * An [IllegalArgumentException] will be thrown if the [chatId] has users in it.
     *
     * @see GroupChatUsers.removeUsers
     */
    fun delete(chatId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        require(userIdList.isEmpty()) { "The chat (ID: $chatId) is not empty (user IDs: $userIdList)." }
        TypingStatuses.deleteChat(chatId)
        Messages.deleteChat(chatId)
        transaction {
            deleteWhere { GroupChats.id eq chatId }
        }
        Chats.delete(chatId)
    }

    /**
     * Case-insensitively [query]s the title of every chat the [userId] is in, and returns the IDs of matching chats.
     * The returned chat IDs are sorted in ascending order.
     */
    fun search(userId: Int, query: String): LinkedHashSet<Int> {
        val chatIdList = GroupChatUsers.readChatIdList(userId)
        return transaction {
            select((GroupChats.id inList chatIdList) and (title iLike query))
                .orderBy(GroupChats.id)
                .map { it[GroupChats.id] }
                .toLinkedHashSet()
        }
    }

    /**
     * Case-insensitively [query]s public chats by their title, and returns the IDs of matching chats in ascending
     * order.
     */
    fun searchPublicChats(query: String, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = (publicity eq GroupChatPublicity.PUBLIC) and (title iLike query)
        pagination?.after?.let { op = op and (id greater pagination.after) }
        return transaction {
            select(op)
                .orderBy(GroupChats.id)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[GroupChats.id] }
                .toLinkedHashSet()
        }
    }

    /**
     * Case-insensitively [query]s public chats by their title. Returns the [type] of [Cursor] from the searched chats.
     */
    fun readPublicChatsCursor(query: String, type: CursorType): Cursor? {
        val order = when (type) {
            CursorType.END -> SortOrder.DESC
            CursorType.START -> SortOrder.ASC
        }
        return transaction {
            select((publicity eq GroupChatPublicity.PUBLIC) and (title iLike query))
                .orderBy(GroupChats.id, order)
                .limit(1)
                .firstOrNull()
                ?.get(GroupChats.id)
        }
    }

    /** Notifies subscribers of the [UpdatedGroupChat] via [groupChatsNotifier]. */
    fun setBroadcastStatus(chatId: Int, isBroadcast: Boolean) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.isBroadcast] = isBroadcast }
        }
        groupChatsNotifier.publish(
            UpdatedGroupChat(chatId, isBroadcast = isBroadcast),
            GroupChatUsers.readUserIdList(chatId).map(::UserId),
        )
    }

    /**
     * Throws an [IllegalArgumentException] if the [chatId] is public. Subscribers are notified of the
     * [UpdatedGroupChat] via [groupChatsNotifier].
     */
    fun setInvitability(chatId: Int, isInvitable: Boolean) {
        require(!isExistingPublicChat(chatId)) { "A public chat's invitability cannot be updated." }
        val publicity = if (isInvitable) GroupChatPublicity.INVITABLE else GroupChatPublicity.NOT_INVITABLE
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.publicity] = publicity }
        }
        groupChatsNotifier.publish(
            UpdatedGroupChat(chatId, publicity = publicity),
            GroupChatUsers.readUserIdList(chatId).map(::UserId),
        )
    }

    /** Returns `false` if the [chatId] either doesn't exist or isn't invitable. */
    fun isInvitable(chatId: Int): Boolean = transaction {
        val publicity = select(GroupChats.id eq chatId).firstOrNull()?.get(publicity) ?: return@transaction false
        publicity != GroupChatPublicity.NOT_INVITABLE
    }

    /**
     * Returns `true` if there's an invitable chat (i.e., not [GroupChatPublicity.NOT_INVITABLE]) having the
     * [inviteCode], and `false` otherwise.
     */
    fun isExistingInviteCode(inviteCode: UUID): Boolean = transaction {
        select((GroupChats.inviteCode eq inviteCode) and (publicity neq GroupChatPublicity.NOT_INVITABLE))
            .empty()
            .not()
    }

    /**
     * Returns the [chatId]'s invite code, or `null` if the chat isn't invitable.
     *
     * @see isExistingInviteCode
     */
    fun readInviteCode(chatId: Int): UUID? = transaction {
        select((GroupChats.id eq chatId) and (publicity neq GroupChatPublicity.NOT_INVITABLE))
            .firstOrNull()
            ?.get(inviteCode)
    }

    /** Returns the ID of the chat having the [inviteCode], or `null` if there's no such invitable chat. */
    fun readChatIdFromInviteCode(inviteCode: UUID): Int? = transaction {
        select((GroupChats.inviteCode eq inviteCode) and (publicity neq GroupChatPublicity.NOT_INVITABLE))
            .firstOrNull()
            ?.get(GroupChats.id)
    }

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in.
     *
     * Only [ChatEdges.chatId]s having messages matching the [query] will be returned, and they'll only contain the
     * matched [ChatEdges.messageIdList]. The returned [ChatEdges] are sorted in ascending order of their
     * [ChatEdges.chatId].
     */
    fun queryUserChatEdges(userId: Int, query: String): LinkedHashSet<ChatEdges> = GroupChatUsers
        .readChatIdList(userId)
        .associateWith { Messages.searchGroupChat(it, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }
        .toLinkedHashSet()
}
