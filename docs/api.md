# API

## Flow

Here is the usual flow for using this service.
1. Have the user sign up for an account. Pass the info they give you to `Mutatation.createAccount`.
1. Have the user verify their email.
1. Have the user log in. Pass the credentials they give you to `Query.requestTokenSet`. This will give you an access token to authenticate their future actions.
1. Use the access token to authorize requests on behalf of the user (e.g., to use `Query.readChats`).
1. Periodically request a new token set using `Query.refreshTokenSet`.

## Notes

- IDs (e.g., message IDs) are strictly increasing. Therefore, they must be used for ordering items (e.g., messages). For example, if two messages were sent at the same nanosecond, order them by their ID.

## Security

[JWT](https://jwt.io/) is used for auth. Access and refresh tokens expire in one hour and one week respectively. Any operation requiring auth (e.g., the `/message-updates` endpoint for `Subscription.messageUpdates`, the `/graphql` endpoint for `Query.updateAccount`) must have the access token passed using the Bearer schema. If the access token sent is invalid, an HTTP response with the status code 401 will be returned.

## GraphQL

Here are the current version's [API docs](../src/main/resources/schema.graphqls).

The base URL is http://localhost:80.

GraphQL documents are in JSON. The query, variables, and operation name you send is a "GraphQL document". The data and errors the server responds with is a "GraphQL document". Use the following format when sending GraphQL documents.

|Key|Explanation|Optional|
|---|---|---|
|`"query"`|GraphQL query to execute.|No|
|`"variables"`|The runtime values to use for any GraphQL query variables as a JSON object.|Yes|
|`"operationName"`|If the provided query contains multiple named operations, this specifies which operation should be executed. If this is not provided, and the query contains multiple named operations, an HTTP status code of 400 will be returned.|Yes|

Here's an example.

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

### `Query`s and `Mutation`s

Send the GraphQL query in an HTTP POST request to the `/graphql` endpoint.

Here's an example request for `Query.updateAccount`.

```http request
POST http://localhost:80/graphql HTTP/1.1
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

### `Subscription`s

Each `Subscription` has its own endpoint. The endpoint is the operation's name styled using kebab-case. For example, the endpoint for `Subscription.messageUpdates` is `/message-updates`. `Subscription`s use WebSockets with a ping period of 60 seconds, and a timeout of 15 seconds. Since WebSockets can't transfer JSON directly, the GraphQL documents, which are in JSON, are serialized as text when being sent or received.

It takes a small amount of time for the WebSocket connection to be created. After the connection has been created, it takes a small amount of time for the `Subscription` to be created. Although these delays may be imperceptible to humans, it's possible that an event, such as a newly created chat message, was sent during one of these delays. For example, if you were opening a user's chat, you might be tempted to first `Query` the previous messages, and then create a `Subscription` to receive new messages. However, this might cause a message another user sent in the chat to be lost during one of the aforementioned delays. Therefore, you should first create the `Subscription` (i.e., await the WebSocket connection to be created), await the `CreatedSubscription` event, and then `Query` for older data if required.

An example of using a `Subscription` is shown below using `Subscription.messageUpdates`.

1. Open the WebSocket connection. Here's an example WebSocket handshake request.

    ```http request
    GET http://localhost:80/message-updates HTTP/1.1
    Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YjNhNzRhZi03Y2M4LTRjZTMtYTg2ZC05YzI4ZmNlZTAzODciLCJleHAiOjE1ODg3NTE0MjR9.JuVC92_Zz6Cnb5p2ZQ_lMKU_9lfIfAP7PcLkVVKnMkU
    Upgrade: websocket
    Connection: Upgrade
    
    ```

1. Send the GraphQL document in JSON serialized as text. Here's an example JSON string.

    ```json
    {
      "query": "subscription MessageUpdates($chatId: Int!) { messageUpdates(chatId: $chatId) { ... on DeletedMessage { id } ... on Message { id, senderId, text } } }",
      "variables": {
        "chatId": 3
      }
    }
    ```

1. If the `"query"` in the document you sent was invalid, the error will be returned, and then the connection will be closed. Here's an example of the received GraphQL document (a JSON string).

    ```json
    {
      "errors": [
        {
           "message": "INVALID_CHAT_ID"  
        } 
      ]
    }
    ```
   
1. If there was no error in the document you sent, you will receive events (GraphQL documents in JSON serialized as text). Here's an example event (a JSON string).

    ```json
    {
      "data": {
        "messageUpdates": {
          "id": 7,
          "senderId": "586a42c6-1fd4-4bfa-9b78-5f32727042ca",
          "text": "Hi!"
        }
      }
    }
    ```
   
## Health Check

There is an HTTP API endpoint `/health_check` which accepts the HTTP GET verb. It responds with the HTTP status code of 204 only if the server is "healthy". An example use case of this is a backend developer having the server automatically restart whenever it becomes "unhealthy".