package com.neelkamath.omniChatBackend.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see PollMessageOptions */
object PollMessageVotes : Table() {
    override val tableName = "poll_message_votes"
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val optionId: Column<Int> = integer("option_id").references(PollMessageOptions.id)

    /**
     * Creates a vote for the [userId] on the [optionId] if they haven't already.
     *
     * @see PollMessages.setVote
     */
    fun create(userId: Int, optionId: Int) {
        if (!isExisting(userId, optionId))
            transaction {
                insert {
                    it[this.userId] = userId
                    it[this.optionId] = optionId
                }
            }
    }

    private fun isExisting(userId: Int, optionId: Int): Boolean = transaction {
        select((PollMessageVotes.userId eq userId) and (PollMessageVotes.optionId eq optionId)).empty().not()
    }

    /** Returns the IDs of users who voted for the [optionId]. */
    fun read(optionId: Int): Set<Int> =
        transaction { select(PollMessageVotes.optionId eq optionId).map { it[userId] }.toSet() }

    /** Deletes the [userId]'s vote on the [optionId] if it exists. */
    fun deleteVote(userId: Int, optionId: Int): Unit = transaction {
        deleteWhere { (PollMessageVotes.userId eq userId) and (PollMessageVotes.optionId eq optionId) }
    }

    fun deleteVotes(optionIdList: Collection<Int>): Unit = transaction {
        deleteWhere { optionId inList optionIdList }
    }
}
