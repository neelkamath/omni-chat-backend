package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

/** @see PollMessages */
object PollMessageOptions : IntIdTable() {
    override val tableName = "poll_message_options"
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)
    private val option: Column<String> = varchar("option", MessageText.MAX_LENGTH)

    /** The [options] can be read in the same order they were created in by using a function such as [readOptions]) */
    fun create(messageId: Int, options: LinkedHashSet<MessageText>): Unit = transaction {
        batchInsert(options) {
            this[PollMessageOptions.messageId] = messageId
            this[option] = it.value
        }
    }

    fun readOption(optionId: Int): MessageText =
        transaction { select(PollMessageOptions.id eq optionId).first()[option].let(::MessageText) }

    /**
     * Returns the ID of each option the [messageId] has sorted in order of their creation.
     *
     * @see readOptions
     */
    fun readOptionIdList(messageId: Int): LinkedHashSet<Int> = transaction {
        select(PollMessageOptions.messageId eq messageId)
            .orderBy(PollMessageOptions.id)
            .map { it[PollMessageOptions.id].value }
            .toLinkedHashSet()
    }

    /**
     * Returns the [messageId]'s options in order of their creation.
     *
     * @see readOptionIdList
     */
    fun readOptions(messageId: Int): LinkedHashSet<MessageText> = transaction {
        select(PollMessageOptions.messageId eq messageId)
            .orderBy(PollMessageOptions.id)
            .map { MessageText(it[option]) }
            .toLinkedHashSet()
    }

    /** Returns the option ID of the [messageId]'s [option]. */
    fun readOptionId(messageId: Int, option: MessageText): Int = transaction {
        select((PollMessageOptions.messageId eq messageId) and (PollMessageOptions.option eq option.value))
            .first()[PollMessageOptions.id]
            .value
    }

    /** Returns the ID of every [PollMessageOptions] each poll in the [messageIdList] has. */
    private fun readOptionIdList(messageIdList: Collection<Int>): Set<Int> = transaction {
        select(messageId inList messageIdList).map { it[PollMessageOptions.id].value }.toSet()
    }

    /** Whether the [messageId] has the [option]. */
    fun hasOption(messageId: Int, option: MessageText): Boolean = transaction {
        select((PollMessageOptions.messageId eq messageId) and (PollMessageOptions.option eq option.value))
            .empty()
            .not()
    }

    fun delete(messageIdList: Collection<Int>) {
        PollMessageVotes.deleteVotes(readOptionIdList(messageIdList))
        transaction {
            deleteWhere { messageId inList messageIdList }
        }
    }
}
