package com.neelkamath.omniChat.test.graphql.api

const val DELETION_OF_EVERY_MESSAGE_FRAGMENT: String = """
    ... on DeletionOfEveryMessage {
        isDeleted
    }
"""

const val MESSAGE_DATE_TIME_STATUS_FRAGMENT: String = """
    ... on MessageDateTimeStatus {
        userId
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

const val ACCOUNT_INFO_FRAGMENT: String = """
    ... on AccountInfo {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

const val MESSAGE_FRAGMENT: String = """
    ... on Message {
        id
        senderId
        text
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
    }
"""

const val GROUP_CHAT_FRAGMENT: String = """
    ... on GroupChat {
        id
        title
        description
        adminId
        userIdList
        messages {
            $MESSAGE_FRAGMENT
        }
    }
"""

const val PRIVATE_CHAT_FRAGMENT: String = """
    ... on PrivateChat {
        id
        userId
        messages {
            $MESSAGE_FRAGMENT
        }
    }
"""

const val TOKEN_SET_FRAGMENT: String = """
    ... on TokenSet {
        accessToken
        refreshToken
    }
"""