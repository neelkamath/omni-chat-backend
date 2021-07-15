# API

## Flow

Here's the usual flow for using the API if you're building a frontend UI:

1. Have the user sign up for an account. Pass the info they give you to `Mutation.createAccount`.
1. Have the user verify their email address using `Mutation.verifyEmailAddress`.
1. Have the user log in. Pass the credentials they give you to `Query.requestTokenSet`. This will give you an access token to authenticate their future actions.
1. Periodically refresh the token set using `Query.refreshTokenSet`.
1. Use the access token to authorize requests on behalf of the user (e.g., `Query.readChats`).

Here's the usual flow for using the API if you're building a bot:

1. Create an account for the bot, and verify the account's email address.
1. Store the created account's username and password in the bot's source code.
1. Program the bot to call `Query.requestTokenSet` when it first starts, and to use `Query.refreshTokenSet` periodically.
1. Program the bot to use the API (e.g., `Mutation.createTextMessage`).

## Notes

- If Omni Chat is running using [Docker Compose](docker-compose.md), the base URL is http://localhost by default. If Omni Chat is being run [in the cloud](cloud.md), the base URL is whatever the server admin is running it on (e.g., `https://example.com`).
- Message IDs are unique across chats.
- Though the server knows which users have been blocked, it doesn't treat blocked users differently. The client chooses what to do with messages, etc. from a blocked user. For example, if a blocked user creates a chat with the user, the client could hide it in a page away from the main chats where the user would have to explicitly navigate to should they want to (e.g., the way Gmail handles spam email).

## Security

[JWTs](https://jwt.io/) are used for auth. Access and refresh tokens expire in one hour and one week respectively. The `sub` and `exp` claims are present in each token's payload. The `sub` claim is the user's ID in a `string`.

HTTP requests requiring auth (e.g., the `/query-or-mutation` endpoint for `Query.updateAccount`) must have the access token passed using the Bearer schema. The user is unauthorized when calling an operation requiring an access token if they've failed to provide one, provided an invalid one (e.g., an expired token), or lack the required permission level (e.g., the user isn't allowed to perform the requested action).

## GraphQL API

### Documents

GraphQL documents are in JSON. The query, variables, and operation name you send is a "GraphQL document". The data and errors the server responds with is a "GraphQL document". Use the following format when sending GraphQL documents.

|Key|Explanation|Optional|
|---|---|---|
|`"query"`|GraphQL query to execute.|No|
|`"variables"`|The runtime values to use for any GraphQL query variables as a JSON object.|Yes|
|`"operationName"`|If the provided query contains multiple named operations, this specifies which operation should be executed. If this isn't provided, and the query contains multiple named operations, an error will be returned. For `Query`s and `Mutation`s, an HTTP status code of 400 will be returned. For `Subscription`s, a status code of 1008 will be returned, and the connection will be closed.|Yes|

Here's an example:

```json
{
  "query": "mutation UpdateAccount($update: AccountUpdate!) { updateAccount(update: $update) }",
  "variables": {
    "update": {
      "username": "john_doe",
      "password": "pass"
    }
  }
}
```

### Pagination

[Pagination](https://graphql.org/learn/pagination/) follows [Relay](https://relay.dev)'s [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm) with the exception that fields are nullable based on what's more logical. The following explanation clarifies the parts Relay's spec isn't clear on.

It's possible that a previously valid cursor no longer is. For example, you might read five messages, and then attempt to read another five by passing the cursor of the oldest message; but it happens that the oldest message just got deleted. In such cases, the expected messages will still be returned (i.e., it will seem as if the cursor is valid).

Here's how backward pagination (i.e., pagination using the `last` and `before` arguments) works. Forward pagination (i.e., pagination using the `first` and `after` arguments) behaves similarly. The `last` and `before` arguments indicate the number of items to be returned before the cursor. `last` indicates the maximum number of items to retrieve (e.g., if there are two items, and five items get requested, only two will be returned). `before` is the cursor (i.e., only items before this will be returned). Here's the algorithm:

- If neither `last` nor `before` are `null`, then at most `last` items will be returned from before the cursor.
- If `last` isn't null but `before` is, then at most `last` items will be returned from the end.
- If `last` is `null` but `before` isn't, then every item before the cursor will be returned.
- If both `last` and `before` are `null`, then every item will be returned.

### `Query`s and `Mutation`s

Send the GraphQL query in an HTTP POST request to the `/query-or-mutation` endpoint.

Here's an example request for `Query.updateAccount`:

```http request
POST http://localhost/query-or-mutation HTTP/1.1
Content-Type: application/json
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YzI5MjQ3My0yMmQ1LTQ4MjUtOGYzNS0xYWNhNDZjMGNmNTYiLCJhdWQiOiJvbW5pLWNoYXQiLCJpc3MiOiJodHRwOi8vYXV0aDo4MDgwIiwiZXhwIjoxNTg3NzA5OTQ4fQ.w_t9fGjYj_Nw569xG92NCEjmzZC95NP-t0VXCCXuizM

{
  "query": "mutation UpdateAccount($update: AccountUpdate!) { updateAccount(update: $update) }",
  "variables": {
    "update": {
      "username": "john_doe",
      "password": "pass"
    }
  }
}

```

If the user is unauthorized, the server will respond with an HTTP status code of 401.

### `Subscription`s

Each `Subscription` has its own endpoint. The endpoint is the operation's return type styled using kebab-case (e.g., the endpoint for `Subscription.subscribeToMessages` is `/messages-subscription` because it returns a `MessagesSubscription`). `Subscription`s use WebSockets with a ping period of one minute, and a timeout of 15 seconds. Since WebSockets can't transfer JSON directly, the GraphQL documents, which are in JSON, are serialized as text when being sent or received.

It takes a small amount of time for the WebSocket connection to be created. After the connection has been created, it takes a small amount of time for the `Subscription` to get created. Although these delays may be imperceptible to humans,
it's possible that an event, such as a newly created chat message, was sent during one of these delays. For example, if you were opening a user's chat, you might be tempted to first `Query` the previous messages, and then create a `Subscription` to receive new messages. However, this might cause a message another user sent in the chat to get lost during one of the aforementioned delays. Therefore, you should first create the `Subscription`, await the `CreatedSubscription` event, and then `Query` for older data if required.

The server only accepts the first one or two events you send it (i.e., the GraphQL document you send when you first open the connection, and an access token if required). Any further events you send to the server will be ignored.

Here's an example of a `Subscription` using `Subscription.subscribeToChatMessages`:

1. Open a WebSocket connection on the path `/chat-messages-subscription` (e.g., `http://localhost/chat-messages-subscription`).
1. Skip this step if the operation doesn't require an access token. Pass the access token you get from `Query.requestTokenSet` as a text event (
   e.g., `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YzI5MjQ3My0yMmQ1LTQ4MjUtOGYzNS0xYWNhNDZjMGNmNTYiLCJhdWQiOiJvbW5pLWNoYXQiLCJpc3MiOiJodHRwOi8vYXV0aDo4MDgwIiwiZXhwIjoxNTg3NzA5OTQ4fQ.w_t9fGjYj_Nw569xG92NCEjmzZC95NP-t0VXCCXuizM`). If the user is unauthorized, the connection will be closed with a status code of 1008.
1. Send the JSON GraphQL document as a text event. For example:
    ```json
    {
      "query": "subscription SubscribeToChatMessages($chatId: Int!) { subscribeToChatMessages(chatId: $chatId) { __typename ... on NewTextMessage { chatId, textMessage } } }"
    }
    ```
1. If the GraphQL document you sent was invalid, a text event will be received, and the connection will get closed. For example:
    ```json
    {
        "errors": [
            {
               "message": "Invalid type"
            }
        ]
    }
    ```
1. A `CreatedSubscription` text event will be received. For example:
    ```json
    {
        "data": {
            "subscribeToChatMessages": {
                "__typename": "CreatedSubscription"
            }
        }
    }
    ```
1. If the GraphQL document you sent contained invalid data, the result will be received as a text event, and the connection will get closed. For example:
    ```json
    {
        "data": {
            "subscribeToChatMessages": {
                "__typename": "InvalidChatId"
            }
        }
    }
    ```
1. You will receive updates as text events. For example:
    ```json
    {
      "data": {
        "subscribeToChatMessages": {
          "chatId": 3,
          "textMessage": "Hi!"
        }
      }
    }
    ```

## Operations

The application is primarily a [GraphQL](https://graphql.org/) API served over the HTTP and WS protocols. Here's the GraphQL API's [schema](../src/main/resources/schema.graphqls). There's also a REST API for tasks which aren't well suited for GraphQL such as image uploads. You can view the REST API documentation by opening the release asset you downloaded earlier, `rest-api.html`, in your browser.

Since there are many operations, we've categorized each of them below. The same operation may occur under different categories. You can use your browser or editor's "Find in page" feature to quickly find the relevant documentation once you've found the operation you want to use.

### User

- `Query`
    - `requestTokenSet`
    - `refreshTokenSet`
    - `readAccount`
- `Mutation`
    - `deleteAccount`
    - `verifyEmailAddress`
    - `resetPassword`
    - `setOnline`
    - `setTyping`
    - `deleteProfileImage`
    - `updateAccount`
    - `createAccount`
    - `emailEmailAddressVerification`
    - `emailPasswordResetCode`
- `Subscription`
    - `subscribeToAccounts`
- REST API
    - `/profile-image`

### Other Users

- `Query`
    - `readAccount`
    - `readOnlineStatus`
    - `readTypingUsers`
    - `searchUsers`
    - `searchGroupChatUsers`
- `Subscription`
    - `subscribeToOnlineStatuses`
    - `subscribeToChatOnlineStatuses`
    - `subscribeToTypingStatuses`
    - `subscribeToChatTypingStatuses`
    - `subscribeToAccounts`
    - `subscribeToChatAccounts`
- REST API
    - `/profile-image`

### Blocked Users

- `Query`
    - `readBlockedUsers`
    - `searchBlockedUsers`
- `Mutation`
    - `blockUser`
    - `unblockUser`
- `Subscription`
    - `subscribeToAccounts`
- REST API
    - `/profile-image`

### Contacts

- `Query`
    - `readContacts`
    - `searchContacts`
    - `readOnlineStatus`
- `Mutation`
    - `deleteContact`
    - `createContact`
- `Subscription`
    - `subscribeToOnlineStatuses`
    - `subscribeToAccounts`
- REST API
    - `/profile-image`

### Chats

- `Query`
    - `readChats`
    - `readChat`
    - `readGroupChat`
    - `searchChats`
    - `searchPublicChats`
    - `readOnlineStatus`
    - `searchGroupChatUsers`
- `Mutation`
    - `deleteGroupChatImage`
    - `updateGroupChatTitle`
    - `updateGroupChatDescription`
    - `addGroupChatUsers`
    - `removeGroupChatUsers`
    - `makeGroupChatAdmins`
    - `createGroupChat`
    - `setBroadcast`
    - `setPublicity`
    - `joinGroupChat`
    - `joinPublicChat`
    - `leaveGroupChat`
    - `deletePrivateChat`
    - `createPrivateChat`
- `Subscription`
    - `subscribeToChats`
    - `subscribeToGroupChatMetadata`
    - `subscribeToOnlineStatuses`
    - `subscribeToChatOnlineStatuses`
    - `subscribeToTypingStatuses`
    - `subscribeToChatTypingStatuses`
    - `subscribeToAccounts`
    - `subscribeToChatAccounts`
- REST API
    - `/group-chat-image`

### Messages

- `Query`
    - `readMessage`
    - `readBookmarks`
    - `searchChatMessages`
    - `searchMessages`
- `Mutation`
    - `createBookmark`
    - `deleteBookmark`
    - `createTextMessage`
    - `createActionMessage`
    - `createGroupChatInviteMessage`
    - `createPollMessage`
    - `forwardMessage`
    - `triggerAction`
    - `setPollVote`
    - `deleteMessage`
- `Subscription`
    - `subscribeToMessages`
    - `subscribeToChatMessages`
- REST API
    - `/image-message`
    - `/audio-message`
    - `/video-message`
    - `/doc-message`

### Miscellaneous

- Query
    - `readAllowedEmailAddressDomains`
- REST API
    - `/health-check`
