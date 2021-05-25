package com.neelkamath.omniChatBackend.graphql.routing

import com.neelkamath.omniChatBackend.db.tables.Users

typealias Cursor = Int

object Placeholder

/**
 * An [IllegalArgumentException] will be thrown if the following criteria aren't met:
 * - Must be 1-30 characters.
 * - Must contain only lowercase English letters (a-z), English numbers (0-9), periods, and underscores.
 */
@JvmInline
value class Username(val value: String) {
    init {
        require(!value.contains(Regex("""[^a-z0-9._]"""))) {
            """
            The username ("$value") must only contain lowercase English letters, English numbers, periods, and 
            underscores.
            """
        }
        require(value.length in 1..30) { "The username ($value) must be 1-${Users.MAX_NAME_LENGTH} characters long." }
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [adminIdList] is empty, or the [adminIdList] isn't a subset of
 * the [userIdList].
 */
data class GroupChatInput(
    val title: GroupChatTitle,
    val description: GroupChatDescription = GroupChatDescription(""),
    val userIdList: List<Int>,
    val adminIdList: List<Int>,
    val isBroadcast: Boolean,
    val publicity: GroupChatPublicity,
) {
    init {
        require(adminIdList.isNotEmpty()) { "There must be at least one admin." }
        require(userIdList.containsAll(adminIdList)) {
            "The admin ID list ($adminIdList) must be a subset of the user ID list ($userIdList)."
        }
    }
}

/**
 * An [IllegalArgumentException] will be thrown if it contains whitespace, or exceeds [Users.MAX_NAME_LENGTH]
 * characters.
 */
@JvmInline
value class Name(val value: String) {
    init {
        require(!value.contains(Regex("""\s"""))) { """The name ("$value") cannot contain whitespace.""" }
        require(value.length <= Users.MAX_NAME_LENGTH) {
            "The name ($value) mustn't exceed ${Users.MAX_NAME_LENGTH} characters."
        }
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] exceeds [Bio.MAX_LENGTH], contains leading whitespace, or
 * contains trailing whitespace.
 */
@JvmInline
value class Bio(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { "The value ($value) cannot exceed $MAX_LENGTH characters." }
        require(value.trim() == value) { "The value ($value) can neither contain leading nor trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 2500
    }
}

/** An [IllegalArgumentException] will be thrown if the [value] doesn't contain non-whitespace characters. */
@JvmInline
value class Password(val value: String) {
    init {
        require(value.trim().isNotEmpty()) { """The password ("$value") mustn't be empty.""" }
    }
}

/**
 * An [IllegalArgumentException] will be thrown in the following cases:
 * - The [value] isn't 1-[GroupChatTitle.MAX_LENGTH] characters, of which at least one isn't whitespace.
 * - The [value] contains leading or trailing whitespace.
 */
@JvmInline
value class GroupChatTitle(val value: String) {
    init {
        require(value.trim().isNotEmpty() && value.length <= MAX_LENGTH) {
            """The title ("$value") must be 1-$MAX_LENGTH characters, with at least one non-whitespace character."""
        }
        require(value.trim() == value) { "The value ($value) cannot contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 70
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't at most [GroupChatDescription.MAX_LENGTH]
 * characters, or it contains leading/trailing whitespace.
 */
@JvmInline
value class GroupChatDescription(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { """The description ("$value") must be at most $MAX_LENGTH characters""" }
        require(value.trim() == value) { "The value ($value) mustn't contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[MessageText.MAX_LENGTH] characters with at least
 * one non-whitespace character, or it contains leading/trailing whitespace.
 */
@JvmInline
value class MessageText(val value: String) {
    init {
        require(value.trim().isNotEmpty() && value.length <= MAX_LENGTH) {
            "The text must be 1-$MAX_LENGTH characters, with at least one non-whitespace."
        }
        require(value.trim() == value) { "The value ($value) mustn't contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 10_000
    }
}

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class PollInput(val title: MessageText, val options: List<MessageText>) {
    init {
        require(options.size > 1) { "There must be at least two options ($options)." }
        require(options.toSet().size == options.size) { "Options ($options) must be unique." }
    }
}

/** An [IllegalArgumentException] is thrown if there isn't at least one [actions], or the [actions] aren't unique. */
data class ActionMessageInput(val text: MessageText, val actions: List<MessageText>) {
    init {
        require(actions.isNotEmpty()) { "There must be at least one action." }
        require(actions.toSet().size == actions.size) { "Actions ($actions) must be unique." }
    }
}

data class AccountInput(
    val username: Username,
    val password: Password,
    val emailAddress: String,
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val bio: Bio = Bio(""),
)

data class Login(val username: Username, val password: Password)

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: Name? = null,
    val lastName: Name? = null,
    val bio: Bio? = null,
)

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any?>? = null,
    val operationName: String? = null,
)

data class GraphQlResponse(val data: Map<String, Any?>? = null, val errors: List<GraphQlResponseError>? = null)

data class GraphQlResponseError(val message: String)

enum class GroupChatPublicity { NOT_INVITABLE, INVITABLE, PUBLIC }

enum class MessageState { SENT, DELIVERED, READ }

enum class MessageStatus { DELIVERED, READ }
