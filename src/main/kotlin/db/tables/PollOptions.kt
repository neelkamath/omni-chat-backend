package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.PollOption
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [PollMessages] */
object PollOptions : IntIdTable() {
    override val tableName = "poll_options"
    private val pollId: Column<Int> = integer("poll_id").references(PollMessages.id)
    private val option: Column<String> = varchar("option", MessageText.MAX_LENGTH)

    fun create(pollId: Int, options: List<MessageText>): Unit = transaction {
        batchInsert(options) {
            this[PollOptions.pollId] = pollId
            this[option] = it.value
        }
    }

    fun read(pollId: Int): List<PollOption> = transaction {
        select { PollOptions.pollId eq pollId }.map {
            val votes = PollVotes.read(it[PollOptions.id].value)
            PollOption(MessageText(it[option]), votes)
        }
    }

    /** Returns the option ID of the [pollId]'s [option]. */
    fun readId(pollId: Int, option: MessageText): Int = transaction {
        select { (PollOptions.pollId eq pollId) and (PollOptions.option eq option.value) }.first()[PollOptions.id].value
    }

    /** Returns the IDs of all [PollOptions] every poll in the [pollIdList] has. */
    private fun readIdList(pollIdList: List<Int>): List<Int> = transaction {
        select { pollId inList pollIdList }.map { it[PollOptions.id].value }
    }

    fun delete(pollIdList: List<Int>) {
        PollVotes.deleteVotes(readIdList(pollIdList))
        transaction {
            deleteWhere { pollId inList pollIdList }
        }
    }
}