package com.neelkamath.omniChat.graphql.api

const val ACCOUNT_INFO_FRAGMENT: String = """
    ... on AccountInfo {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

const val MESSAGE_DATE_TIME_STATUS_FRAGMENT: String = """
    ... on MessageDateTimeStatus {
        user {
            $ACCOUNT_INFO_FRAGMENT
        }
        dateTime
        status
    }
"""

const val MESSAGE_DATE_TIMES_FRAGMENT: String = """
    ... on MessageDateTimes {
        sent
        statuses {
            $MESSAGE_DATE_TIME_STATUS_FRAGMENT
        }
    }
"""

const val MESSAGE_FRAGMENT: String = """
    ... on Message {
        id
        sender {
            $ACCOUNT_INFO_FRAGMENT
        }
        text
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
    }
"""

const val MESSAGE_EDGE_FRAGMENT: String = """
    ... on MessageEdge {
        node {
            $MESSAGE_FRAGMENT
        }
        cursor
    }
"""

const val PAGE_INFO_FRAGMENT: String = """
    ... on PageInfo {
        hasNextPage
        hasPreviousPage
        startCursor
        endCursor
    }
"""

const val MESSAGES_CONNECTION_FRAGMENT: String = """
    ... on MessagesConnection {
        edges {
            $MESSAGE_EDGE_FRAGMENT
        }
        pageInfo {
            $PAGE_INFO_FRAGMENT
        }
    }
"""

const val DELETION_OF_EVERY_MESSAGE_FRAGMENT: String = """
    ... on DeletionOfEveryMessage {
        isDeleted
    }
"""

const val USER_CHAT_MESSAGES_REMOVAL_FRAGMENT: String = """
    ... on UserChatMessagesRemoval {
        userId
    }
"""

const val MESSAGE_DELETION_POINT_FRAGMENT: String = """
    ... on MessageDeletionPoint {
        until
    }
"""

const val CREATED_SUBSCRIPTION_FRAGMENT: String = """
    ... on CreatedSubscription {
        isCreated
    }
"""

const val DELETED_MESSAGE_FRAGMENT: String = """
    ... on DeletedMessage {
        id
    }
"""

const val GROUP_CHAT_FRAGMENT: String = """
    ... on GroupChat {
        id
        title
        description
        adminId
        users {
            $ACCOUNT_INFO_FRAGMENT
        }
        messages(last: ${"$"}last, before: ${"$"}before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

const val PRIVATE_CHAT_FRAGMENT: String = """
    ... on PrivateChat {
        id
        user {
            $ACCOUNT_INFO_FRAGMENT
        }
        messages(last: ${"$"}last, before: ${"$"}before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

const val CHAT_MESSAGES_FRAGMENT: String = """
    ... on ChatMessages {
        chat {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
        messages(last: ${"$"}last, before: ${"$"}before) {
            $MESSAGE_EDGE_FRAGMENT
        }
    }
"""

const val TOKEN_SET_FRAGMENT: String = """
    ... on TokenSet {
        accessToken
        refreshToken
    }
"""