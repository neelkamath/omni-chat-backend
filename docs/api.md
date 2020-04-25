# API

You can see the latest HTTP API docs [here](https://neelkamath.github.io/omni-chat/redoc-static.html), and the current [GraphQL](http://graphql.org) API docs [here](../src/main/resources/schema.graphql).

## GraphQL API

Make an HTTP POST request to the http://localhost:80/graphql endpoint. The request body should contain the following key-value pairs.

|Key|Explanation|Optional|
|---|---|---|
|`"query"`|GraphQL document to execute.|No|
|`"variables"`|The runtime values to use for any GraphQL query variables as a JSON object.|Yes|
|`"operationName"`|If the provided query contains multiple named operations, this specifies which operation should be executed. If this is not provided, and the query contains multiple named operations, an HTTP status code of 400 will be returned.|Yes|

Here's an example request for the `Query.updateAccount` operation.

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

## Flow

Here is the usual flow for using this service.
1. Have the user sign up for an account. Pass the info they give you to create an account for them using the `Mutatation.createAccount` operation.
1. Have the user verify their email.
1. Have the user log in. Pass the credentials they give you while logging in to the `Query.requestJwt` operation. This will give you a [JWT](https://jwt.io/) to authenticate their
  future actions.
1. Use the JWT to authorize requests on behalf of the user (e.g., to use the `Query.readChats` operation).
1. Whenever required, refresh the JWT using the HTTP POST `/refresh_jwt` endpoint.

## How It Works

The same email address cannot be registered twice.

A message can have one of three states. A _sent_ message has been uploaded to the server. A _delivered_ message has
been downloaded by the other user. A _read_ message has been seen by the other user.

A _private chat_ is a chat between two users which cannot be converted into a group chat. When a private chat is
deleted by a user, the messages sent until then are no longer visible to them, and the chat is no longer retrieved
when requesting their chats. However, the user they were chatting with will still have the chat in the same state it
was in before the user deleted it. If the other user sends a message to the user, it will show up as the first
message in the user's chat.

If you are added to a group chat, or are added back to a group chat after leaving it, you will be able to see the
entire chat's history.