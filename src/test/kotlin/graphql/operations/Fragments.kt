package graphql.operations

const val ACCOUNT_FRAGMENT = """
    ... on Account {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

const val PAGE_INFO_FRAGMENT = """
    ... on PageInfo {
        hasNextPage
        hasPreviousPage
        startCursor
        endCursor
    }
"""

const val ACCOUNTS_CONNECTION_FRAGMENT = """
    ... on AccountsConnection {
        edges {
            node {
                $ACCOUNT_FRAGMENT
            }
            cursor
        }
        pageInfo {
            $PAGE_INFO_FRAGMENT
        }
    }
"""

const val MESSAGE_DATE_TIME_STATUS_FRAGMENT = """
    ... on MessageDateTimeStatus {
        user {
            $ACCOUNT_FRAGMENT
        }
        dateTime
        status
    }
"""

const val MESSAGE_DATE_TIMES_FRAGMENT = """
    ... on MessageDateTimes {
        sent
        statuses {
            $MESSAGE_DATE_TIME_STATUS_FRAGMENT
        }
    }
"""

const val MESSAGE_FRAGMENT = """
    ... on Message {
        id
        sender {
            $ACCOUNT_FRAGMENT
        }
        text
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
    }
"""

const val MESSAGE_EDGE_FRAGMENT = """
    ... on MessageEdge {
        node {
            $MESSAGE_FRAGMENT
        }
        cursor
    }
"""

const val MESSAGES_CONNECTION_FRAGMENT = """
    ... on MessagesConnection {
        edges {
            $MESSAGE_EDGE_FRAGMENT
        }
        pageInfo {
            $PAGE_INFO_FRAGMENT
        }
    }
"""

const val DELETION_OF_EVERY_MESSAGE_FRAGMENT = """
    ... on DeletionOfEveryMessage {
        placeholder
    }
"""

const val USER_CHAT_MESSAGES_REMOVAL_FRAGMENT = """
    ... on UserChatMessagesRemoval {
        userId
    }
"""

const val MESSAGE_DELETION_POINT_FRAGMENT = """
    ... on MessageDeletionPoint {
        until
    }
"""

const val CREATED_SUBSCRIPTION_FRAGMENT = """
    ... on CreatedSubscription {
        placeholder
    }
"""


const val DELETED_MESSAGE_FRAGMENT = """
    ... on DeletedMessage {
        id
    }
"""

const val GROUP_CHAT_FRAGMENT = """
    ... on GroupChat {
        id
        title
        description
        adminId
        users(first: ${"$"}groupChat_users_first, after: ${"$"}groupChat_users_after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
        messages(last: ${"$"}groupChat_messages_last, before: ${"$"}groupChat_messages_before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

const val PRIVATE_CHAT_FRAGMENT = """
    ... on PrivateChat {
        id
        user {
            $ACCOUNT_FRAGMENT
        }
        messages(last: ${"$"}privateChat_messages_last, before: ${"$"}privateChat_messages_before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

const val CHAT_MESSAGES_FRAGMENT = """
    ... on ChatMessages {
        chat {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
        messages(last: ${"$"}chatMessages_messages_last, before: ${"$"}chatMessages_messages_before) {
            $MESSAGE_EDGE_FRAGMENT
        }
    }
"""

const val TOKEN_SET_FRAGMENT = """
    ... on TokenSet {
        accessToken
        refreshToken
    }
"""

const val NEW_CONTACT_FRAGMENT = """
    ... on NewContact {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

const val UPDATED_CONTACT_FRAGMENT = """
        ... on UpdatedContact {
        id
        username
        emailAddress
        firstName
        lastName
    }
"""

const val DELETED_CONTACT_FRAGMENT = """
    ... on DeletedContact {
        id
    }
"""