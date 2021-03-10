package com.neelkamath.omniChat.graphql.operations

const val POLL_OPTION_FRAGMENT = """
    ... on PollOption {
        __typename
        option
        votes
    }
"""

const val POLL_FRAGMENT = """
    ... on Poll {
        __typename
        title
        options {
            $POLL_OPTION_FRAGMENT
        }
    }
"""

const val MESSAGE_CONTEXT_FRAGMENT = """
    ... on MessageContext {
        __typename
        hasContext
        id
    }
"""

const val ONLINE_STATUS_FRAGMENT = """
    ... on OnlineStatus {
        __typename
        userId
        isOnline
        lastOnline
    }
"""

const val ACCOUNT_FRAGMENT = """
    ... on Account {
        __typename
        id
        username
        emailAddress
        firstName
        lastName
        bio
    }
"""

const val PAGE_INFO_FRAGMENT = """
    ... on PageInfo {
        __typename
        hasNextPage
        hasPreviousPage
        startCursor
        endCursor
    }
"""

const val ACCOUNT_EDGE_FRAGMENT = """
    ... on AccountEdge {
        __typename
        node {
            $ACCOUNT_FRAGMENT
        }
        cursor
    }
"""

const val ACCOUNTS_CONNECTION_FRAGMENT = """
    ... on AccountsConnection {
        __typename
        edges {
            $ACCOUNT_EDGE_FRAGMENT
        }
        pageInfo {
            $PAGE_INFO_FRAGMENT
        }
    }
"""

const val MESSAGE_DATE_TIME_STATUS_FRAGMENT = """
    ... on MessageDateTimeStatus {
        __typename
        user {
            $ACCOUNT_FRAGMENT
        }
        dateTime
        status
    }
"""

const val MESSAGE_DATE_TIMES_FRAGMENT = """
    ... on MessageDateTimes {
        __typename
        sent
        statuses {
            $MESSAGE_DATE_TIME_STATUS_FRAGMENT
        }
    }
"""

const val TEXT_MESSAGE_FRAGMENT = """
    ... on TextMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
        message
    }
"""

const val AUDIO_MESSAGE_FRAGMENT = """
    ... on AudioMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
    }
"""

const val GROUP_CHAT_INVITE_MESSAGE_FRAGMENT = """
    ... on GroupChatInviteMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
        inviteCode
    }
"""

const val GROUP_CHAT_INFO_FRAGMENT = """
    ... on GroupChatInfo {
        __typename
        title
        description
        adminIdList
        users(first: ${"$"}groupChatInfo_users_first, after: ${"$"}groupChatInfo_users_after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
        isBroadcast
        publicity
    }
"""

const val DOC_MESSAGE_FRAGMENT = """
    ... on DocMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
    }
"""

const val VIDEO_MESSAGE_FRAGMENT = """
    ... on VideoMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
    }
"""

const val PIC_MESSAGE_FRAGMENT = """
    ... on PicMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
        caption
    }
"""

const val POLL_MESSAGE_FRAGMENT = """
    ... on PollMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
        poll {
            $POLL_FRAGMENT
        }
    }
"""

const val ACTIONABLE_MESSAGE_FRAGMENT = """
    ... on ActionableMessage {
        __typename
        text
        actions
    }
"""

const val ACTION_MESSAGE_FRAGMENT = """
    ... on ActionMessage {
        __typename
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        hasStar
        message {
            $ACTIONABLE_MESSAGE_FRAGMENT
        }
    }
"""

const val MESSAGE_FRAGMENT = """
    $TEXT_MESSAGE_FRAGMENT
    $ACTION_MESSAGE_FRAGMENT
    $AUDIO_MESSAGE_FRAGMENT
    $GROUP_CHAT_INVITE_MESSAGE_FRAGMENT
    $DOC_MESSAGE_FRAGMENT
    $VIDEO_MESSAGE_FRAGMENT
    $PIC_MESSAGE_FRAGMENT
    $POLL_MESSAGE_FRAGMENT
"""

const val MESSAGE_EDGE_FRAGMENT = """
    ... on MessageEdge {
        __typename
        node {
            $MESSAGE_FRAGMENT
        }
        cursor
    }
"""

const val MESSAGES_CONNECTION_FRAGMENT = """
    ... on MessagesConnection {
        __typename
        edges {
            $MESSAGE_EDGE_FRAGMENT
        }
        pageInfo {
            $PAGE_INFO_FRAGMENT
        }
    }
"""

const val CREATED_SUBSCRIPTION_FRAGMENT = """
    ... on CreatedSubscription {
        __typename
        placeholder
    }
"""

const val TYPING_STATUS_FRAGMENT = """
    ... on TypingStatus {
        __typename
        chatId
        userId
        isTyping
    }
"""

const val GROUP_CHAT_FRAGMENT = """
    ... on GroupChat {
        __typename
        id
        title
        description
        adminIdList
        users(first: ${"$"}groupChat_users_first, after: ${"$"}groupChat_users_after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
        messages(last: ${"$"}groupChat_messages_last, before: ${"$"}groupChat_messages_before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
        isBroadcast
        publicity
        inviteCode
    }
"""

const val PRIVATE_CHAT_FRAGMENT = """
    ... on PrivateChat {
        __typename
        id
        messages(last: ${"$"}privateChat_messages_last, before: ${"$"}privateChat_messages_before) {
            $MESSAGES_CONNECTION_FRAGMENT
        }
        user {
            $ACCOUNT_FRAGMENT
        }
    }
"""

const val CHAT_MESSAGES_FRAGMENT = """
    ... on ChatMessages {
        __typename
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
        __typename
        accessToken
        refreshToken
    }
"""

const val NEW_CONTACT_FRAGMENT = """
    ... on NewContact {
        __typename
        id
        username
        emailAddress
        firstName
        lastName
        bio
    }
"""

const val UPDATED_ACCOUNT_FRAGMENT = """
    ... on UpdatedAccount {
        __typename
        id
        username
        emailAddress
        firstName
        lastName
        bio
    }
"""

const val DELETED_CONTACT_FRAGMENT = """
    ... on DeletedContact {
        __typename
        id
    }
"""

const val STARRED_TEXT_MESSAGE_FRAGMENT = """
    ... on StarredTextMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        message
    }
"""

const val STARRED_PIC_MESSAGE_FRAGMENT = """
    ... on StarredPicMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        caption
    }
"""

const val STARRED_AUDIO_MESSAGE_FRAGMENT = """
    ... on StarredAudioMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
    }
"""

const val STARRED_GROUP_CHAT_INVITE_MESSAGE_FRAGMENT = """
    ... on StarredGroupChatInviteMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        inviteCode
    }
"""

const val STARRED_DOC_MESSAGE_FRAGMENT = """
    ... on StarredDocMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
    }
"""

const val STARRED_VIDEO_MESSAGE_FRAGMENT = """
    ... on StarredVideoMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
    }
"""

const val STARRED_POLL_MESSAGE_FRAGMENT = """
    ... on StarredPollMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        poll {
            $POLL_FRAGMENT
        }
    }
"""

const val STARRED_ACTION_MESSAGE_FRAGMENT = """
    ... on StarredActionMessage {
        __typename
        chatId
        messageId
        sender {
            $ACCOUNT_FRAGMENT
        }
        dateTimes {
            $MESSAGE_DATE_TIMES_FRAGMENT
        }
        context {
            $MESSAGE_CONTEXT_FRAGMENT
        }
        isForwarded
        message {
            $ACTIONABLE_MESSAGE_FRAGMENT
        }
    }
"""

const val STARRED_MESSAGE_FRAGMENT = """
    $STARRED_TEXT_MESSAGE_FRAGMENT
    $STARRED_ACTION_MESSAGE_FRAGMENT
    $STARRED_PIC_MESSAGE_FRAGMENT
    $STARRED_AUDIO_MESSAGE_FRAGMENT
    $STARRED_GROUP_CHAT_INVITE_MESSAGE_FRAGMENT
    $STARRED_DOC_MESSAGE_FRAGMENT
    $STARRED_VIDEO_MESSAGE_FRAGMENT
    $STARRED_POLL_MESSAGE_FRAGMENT
"""

const val UPDATED_PROFILE_PIC_FRAGMENT = """
    ... on UpdatedProfilePic {
        __typename
        id
    }
"""

const val BLOCKED_ACCOUNT_FRAGMENT = """
    ... on BlockedAccount {
        id
        username
        emailAddress
        firstName
        lastName
        bio
    }
"""

const val UNBLOCKED_ACCOUNT_FRAGMENT = """
    ... on UnblockedAccount {
        id
    }
"""

const val ACCOUNTS_SUBSCRIPTION_FRAGMENT = """
    $CREATED_SUBSCRIPTION_FRAGMENT
    $NEW_CONTACT_FRAGMENT
    $UPDATED_ACCOUNT_FRAGMENT
    $UPDATED_PROFILE_PIC_FRAGMENT
    $DELETED_CONTACT_FRAGMENT
    $BLOCKED_ACCOUNT_FRAGMENT
    $UNBLOCKED_ACCOUNT_FRAGMENT
"""
