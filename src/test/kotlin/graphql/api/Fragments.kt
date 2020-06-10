package com.neelkamath.omniChat.test.graphql.api

typealias Cursor = String

fun buildAccountInfoFragment(): String = """
    ... on AccountInfo {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

fun buildMessageDateTimeStatusFragment(): String = """
    ... on MessageDateTimeStatus {
        user {
            ${buildAccountInfoFragment()}
        }
        dateTime
        status
    }
"""

fun buildMessageDateTimesFragment(): String = """
    ... on MessageDateTimes {
        sent
        statuses {
            ${buildMessageDateTimeStatusFragment()}
        }
    }
"""

fun buildMessageFragment(): String = """
    ... on Message {
        id
        sender {
            ${buildAccountInfoFragment()}
        }
        text
        dateTimes {
            ${buildMessageDateTimesFragment()}
        }
    }
"""

fun buildMessageEdgeFragment(): String = """
    ... on MessageEdge {
        node {
            ${buildMessageFragment()}
        }
        cursor
    }
"""

fun buildPageInfoFragment(): String = """
    ... on PageInfo {
        hasNextPage
        hasPreviousPage
        startCursor
        endCursor
    }
"""

fun buildMessagesConnectionFragment(): String = """
    ... on MessagesConnection {
        edges {
            ${buildMessageEdgeFragment()}
        }
        pageInfo {
            ${buildPageInfoFragment()}
        }
    }
"""

fun buildDeletionOfEveryMessageFragment(): String = """
    ... on DeletionOfEveryMessage {
        isDeleted
    }
"""

fun buildUserChatMessagesRemovalFragment(): String = """
    ... on UserChatMessagesRemoval {
        userId
    }
"""

fun buildMessageDeletionPointFragment(): String = """
    ... on MessageDeletionPoint {
        until
    }
"""

fun buildCreatedSubscriptionFragment(): String = """
    ... on CreatedSubscription {
        isCreated
    }
"""

fun buildDeletedMessageFragment(): String = """
    ... on DeletedMessage {
        id
    }
"""

fun buildGroupChatFragment(last: Int?, before: Cursor?): String = """
    ... on GroupChat {
        id
        title
        description
        adminId
        users {
            ${buildAccountInfoFragment()}
        }
        messages(last: $last, before: $before) {
            ${buildMessagesConnectionFragment()}
        }
    }
"""

fun buildPrivateChatFragment(last: Int?, before: Cursor?): String = """
    ... on PrivateChat {
        id
        user {
            ${buildAccountInfoFragment()}
        }
        messages(last: $last, before: $before) {
            ${buildMessagesConnectionFragment()}
        }
    }
"""

fun buildChatMessagesFragment(last: Int?, before: Cursor?): String = """
    ... on ChatMessages {
        chat {
            ${buildPrivateChatFragment(last, before)}
            ${buildGroupChatFragment(last, before)}
        }
        messages(last: $last, before: $before) {
            ${buildMessageEdgeFragment()}
        }
    }
"""

fun buildTokenSetFragment(): String = """
    ... on TokenSet {
        accessToken
        refreshToken
    }
"""