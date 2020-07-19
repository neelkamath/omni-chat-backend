# API

## Flow

Here's the usual flow for using the API:
1. Have the user sign up for an account. Pass the info they give you to `Mutatation.createAccount`.
1. Have the user verify their email.
1. Have the user log in. Pass the credentials they give you to `Query.requestTokenSet`. This will give you an access token to authenticate their future actions.
1. Use the access token to authorize requests on behalf of the user (e.g., `Query.readChats`).
1. Periodically request a new token set using `Query.refreshTokenSet`.

## Notes

- The base URL is http://localhost:80.
- The application is primarily a [GraphQL](https://graphql.org/) API served over the HTTP(S) and WS(S) protocols. There's also a REST API for tasks which aren't well suited for GraphQL, such as uploading images. You can view the REST API's docs by opening the release asset you downloaded earlier, `rest-api.html`, in your browser.
- Unless explicitly states, whitespace is never removed (e.g., a user's first name will keep trailing whitespace intact).
- IDs (e.g., message IDs) are strictly increasing. Therefore, they must be used for ordering items (e.g., messages). For example, if two messages get sent at the same nanosecond, order them by their ID.
- If the user creates a private chat, and doesn't send a message, it'll still exist the next time the chats get read. However, if the chat gets deleted, and then recreated, but no messages get sent after the recreation, it won't show up the next time the chats get read. Therefore, despite not receiving deleted private chats when reading every chat the user is in, it's still possible to read the particular chat's db when supplying its ID. Of course, none of the messages sent before the chat got deleted will be retrieved. This is neither a feature nor a bug. It simply doesn't matter.

## Security

[JWT](https://jwt.io/) is used for auth. Access and refresh tokens expire in one hour and one week respectively. Any operation requiring auth (e.g., the `/messages-subscription` endpoint for `Subscription.subscribeToMessages`, the `/query-or-mutation` endpoint for `Query.updateAccount`) must have the access token passed using the Bearer schema. The user is unauthorized when calling an operation requiring an access token if they've failed to provide one, provided an invalid one (e.g., an expired token), or lack the required permission level (e.g., the user isn't allowed to perform the requested action).

## GraphQL API

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

### Schema

Here's the current version's [schema](../src/main/resources/schema.graphqls).

The schema contains sentences similar to ```Returned `errors[0].message`s could be `"INVALID_CHAT_ID"`.```. `errors[0].message` refers to the `message` key of the first error returned. Such explicitly documented error messages mostly exist to help the client react to invalid operation states at runtime. For example, when the `"NONEXISTENT_USER"` error message gets returned, the client can politely notify the user that they're attempting to log in with an incorrect username, and that they should either fix a typo in it or sign up for a new account.

There are other types of error messages which could be returned which aren't explicitly documented in the schema because they would be repetitive and irrelevant. They are:
- Receiving the `"INTERNAL_SERVER_ERROR"` `errors[0].message` indicates a server-side bug. A client would be unable to do anything about this besides potentially telling the user something similar to "Something went wrong. Please try again.".
- Receiving the `"UNAUTHORIZED"` `errors[0].message` indicates that a mandatory access token wasn't supplied.
- If descriptive error messages get returned, then the GraphQL engine is explaining why the client's request was invalid. In this case, there's a bug in the client's code, and the programmer consuming the API must fix it. 

### Pagination

Pagination follows [Relay](https://relay.dev)'s [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm) with the exception that fields are nullable based on what's more logical.

The following points clarify the parts Relay's spec isn't clear on. Although only the `last` and `before` arguments get explained, the `first` and `after` arguments behave similarly.

It's possible that a cursor which was once valid no longer is. For example, you might read five messages, and later attempt to read another five by passing the cursor of the oldest message; but it happens that the oldest message got deleted just before you used its cursor. In such cases, the expected messages will be returned (i.e., it will seem as if the cursor is valid).

The `last` and `before` arguments indicate the number of items to be returned before the cursor. `last` indicates the maximum number of items to retrieve (e.g., if there are two items, and five gets requested, only two will be returned). `before` is the cursor (i.e., only items before this will be returned). Here's the algorithm:
- If neither `last` nor `before` are `null`, then at most `last` items will be returned from before the cursor.
- If `last` isn't null but `before` is, then at most `last` items will be returned from the end.
- If `last` is `null` but `before` isn't, then every item before the cursor will be returned.
- If both `last` and `before` are `null`, then every item will be returned.

### `Query`s and `Mutation`s

Send the GraphQL query in an HTTP POST request to the `/query-or-mutation` endpoint.

Here's an example request for `Query.updateAccount`:

```http request
POST http://localhost:80/query-or-mutation HTTP/1.1
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

It takes a small amount of time for the WebSocket connection to be created. After the connection has been created, it takes a small amount of time for the `Subscription` to be created. Although these delays may be imperceptible to humans, it's possible that an event, such as a newly created chat message, was sent during one of these delays. For example, if you were opening a user's chat, you might be tempted to first `Query` the previous messages, and then create a `Subscription` to receive new messages. However, this might cause a message another user sent in the chat to be lost during one of the aforementioned delays. Therefore, you should first create the `Subscription` (i.e., await the WebSocket connection to be created), await the `CreatedSubscription` event, and then `Query` for older data if required.

The server only accepts the first event you send it (i.e., the GraphQL document you send when you first open the connection). Any further events you send to the server will be ignored.

Here's an example of a `Subscription` using `Subscription.subscribeToMessages`:
1. Open the WebSocket connection. Note that if you supply an invalid access token, the connection will not be opened. Here's an example WebSocket handshake request:

    ```http request
    GET http://localhost:80/messages-subscription HTTP/1.1
    Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YjNhNzRhZi03Y2M4LTRjZTMtYTg2ZC05YzI4ZmNlZTAzODciLCJleHAiOjE1ODg3NTE0MjR9.JuVC92_Zz6Cnb5p2ZQ_lMKU_9lfIfAP7PcLkVVKnMkU
    Upgrade: websocket
    Connection: Upgrade
    
    ```
1. Send the GraphQL document in JSON serialized as text. Here's an example JSON string:

    ```json
    {
      "query": "subscription SubscribeToMessages { subscribeToMessages { ... on CreatedSubscription { placeholder } ... on NewMessage { senderId, text } } }"
    }
    ```
1. If the user is unauthorized, the connection will be closed with a status code of 1008.
1. If the user's authorized, but the GraphQL document you sent was invalid, the error will be returned, and then the connection will be closed. Here's an example of such a GraphQL document (a JSON string):

    ```json
    {
      "errors": [
        {
           "message": "Invalid type"  
        } 
      ]
    }
    ```
1. If the user's authorized, and the GraphQL document you sent was valid, you will receive events (GraphQL documents in JSON serialized as text). Here's an example of such an event (a JSON string):

    ```json
    {
      "data": {
        "subscribeToMessageUpdates": {
          "senderId": "586a42c6-1fd4-4bfa-9b78-5f32727042ca",
          "text": "Hi!"
        }
      }
    }
    ```