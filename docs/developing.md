# Developing

## Flow

Here's what a standard project iteration looks like.

1. Pick a feature to work on using the [project board](https://github.com/neelkamath/omni-chat/projects/1).
1. Update the [GraphQL schema](../src/main/resources/schema.graphqls).
1. Create [JSON models](../src/main/kotlin/Json.kt). Here's how to create Kotlin types from GraphQL types:
    - GraphQL `interface`s must become Kotlin `interface`s. For example, if the GraphQL schema defines `interface Chat`, the Kotlin types `com.neelkamath.omniChat.Chat` and `com.neelkamath.omniChat.graohql.ChatDto` must be `interface`s.
    - GraphQL `union`s must become Kotlin `sealed class`es sans body.
1. When updating the DB, keep in mind the following.
    - This data may need to be wiped when an account gets deleted. For example, the user's messages must be deleted when they delete their account.
    - Subscribers may need to be notified whenever this data gets created. For example, deleting a chat requires notifying subscribers of such.
1. Write tests.

    If you're writing tests for the GraphQL API, perform the following steps (see [`RefreshTokenSetTest.kt`](../src/test/kotlin/graphql/operations/queries/RefreshTokenSetTest.kt) for an example on queries and mutations, and [`MessageUpdatesTest.kt`](../src/test/kotlin/graphql/operations/subscriptions/MessageUpdatesTest.kt) for an example on subscriptions):
    1. Create inline fragments in [`Fragments.kt`](../src/test/kotlin/graphql/operations/Fragments.kt) named using the format `<TYPE>_FRAGMENT` (e.g., `PRIVATE_CHAT_DELETION_FRAGMENT`). Use the format `<FRAGMENT>_<FIELD>_<ARGUMENT>` when naming variables in fragments. For example, an argument `last` to a field `messages` in a fragment `ChatMessages` would be named `chatMessages_messages_last`.
    1. Create a file named using the format `<OPERATION>Test.kt` (e.g., `DeleteAccountTest.kt`).
    1. Create a constant `String` value for the GraphQL document's query named using the format `<OPERATION>_QUERY` (e.g., `MESSAGE_UPDATES_QUERY`). The query should be named using the operation (e.g., the operation `searchMessages` would have its query named `SearchMessages`). Variables for arguments to the operation name should be named the same as the argument itself. For example, if an operation `Query.readChat` takes an argument `id`, the argument's corresponding variable should be named `id`.
    1. Follow these steps for `Query`s and `Mutation`s. If the function takes an access token, it must be the first parameter.
        1. Create a private function which deals with the HTTP request (the "operator function"). Name it using the format `operate<OPERATION>` (e.g., `operateReadChats`), and have it return a `com.neelkamath.omniChat.graphql.GraphQlResponse`.
        1. Create a function which deals with the HTTP response's db. Name it the GraphQL operation (e.g., `readChats`). It must return the operator function's `com.neelkamath.omniChat.graphql.GraphQlResponse.db` mapped to a Kotlin type (e.g., `List<Chat>`).
        1. create a function which deals with the HTTP response's `ClientException` (e.g., `"INVALID_CHAT_ID"`). Name it using the format `err<OPERATION>` (e.g., `errCreateAccount`). It must return the operator function's `com.neelkamath.omniChat.graphql.GraphQlResponse.errors[0].message`.
    1. Follow these steps for `Subscription`s. If the function takes an access token, it must be the first parameter.
        1. Create a private function which deals with opening the WebSocket connection (the "operator function"). Name it using the format `operate<OPERAION>` (e.g., `operateMessageUpdates`).
        1. Create a function which deals with the `CreatedSubscription`, and receives the events. Name the function `receive<OPERATION>` (e.g., `receiveMessageUpdates`).
        1. Create a function which asserts the `ClientException` received (e.g., `"INVALID_CHAT_ID"`), and asserts that the connection gets closed. Name it using the format `err<OPERATION>` (e.g., `errMessageUpdates`).
    1. Create a class named using the format `<OPERATION>Test` (e.g., `ReadChatsTest`).
    1. Write load tests if required. For example, you might want to test that searching messages from a populated DB is fast.
1. Implement the feature. For `Query`s and `Mutation`s, name the resolver the same as the GraphQL operation (e.g., `readChats`). For `Subscription`s, name the resolver named using the format `operate<OPERATION>` (e.g., `routeMessageUpdates`).
1. Mark the feature as completed in the [spec](spec.md).
1. Create a new release.
    1. Update the version in the [build file](../build.gradle.kts).
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created.

## Testing

- We need to test both the HTTP/WebSocket interface, and the GraphQL operations. The HTTP/WebSocket interface provides a wrapper for the GraphQL operations which is light enough to be easily used as a substitute for the GraphQL engine (i.e., `graphql.GraphQL`) while still providing the same functionality we require from the engine. Hence, we only test GraphQL operations through the HTTP/WebSocket interface so that we needn't write the same test twice (i.e., a unit test for the GraphQL engine, and an integration test for the HTTP/WebSocket interface). For example, if you are testing `Subscription.messageUpdates`, and need to use `Mutation.createMessage` in the test, use the HTTP interface for `Mutation.createMessage` instead of the GraphQL engine directly.
- Unit tests must never use the HTTP/WebSocket interface.
- Integration tests (i.e., tests in `com.neelkamath.omniChat.graphql`) must only use the HTTP/WebSocket interface. If required, assertions may directly use the under-the-hood functions (i.e., APIs in `com.neelkamath.omniChat.db`, `com.neelkamath.omniChat.db`, etc.).
- These test cases which must be implemented when testing [forward](ForwardPaginationTest.kt) and [backward](BackwardPaginationTest.kt) pagination.
- The test source set should mirror the main source set's directory structure. For example, a file in the main source set named `directory/Filename.kt` should have its test in `directory/FilenameTest.kt`. If it's impractical to fit the tests in a single file, create a folder by the name of the file. For example, [`graphql/operations/Mutations.kt`](../src/main/kotlin/graphql/operations/Mutations.kt)'s tests are in [`graphql/operations/mutations/`](../src/test/kotlin/graphql/operations/mutations). Create a `io.kotest.core.spec.style.FunSpec` for each `class` getting tested, and one with the name of the test file for top-level functions.
- Each function tested should have its test cases placed in a `io.kotest.core.spec.style.FunSpecDsl.context` (`context`). The argument to the `context` must be the function's name and variables. For example, the argument to `context` for the function `infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"` would be `"Expression<String>.iLike(String)"`. If you're testing a private function through its public interface, the signature in the `context` must be the private function's signature so that you can easily find where its functionality gets tested. The only time `context`s aren't used is when the class containing the test cases gets named after the function tested (e.g., [`ReadAccountTest.kt`](../src/test/kotlin/graphql/operations/queries/ReadAccountTest.kt)).

## DB

- Always use the abstraction layer ([`src/main/kotlin/db/`](../src/main/kotlin/db) (e.g., [`Messages.kt`](../src/main/kotlin/db/messages/Messages.kt)) and its mirror files in [`src/test/kotlin/db/`](../src/test/kotlin/db)) (e.g., [`MessagesUtil.kt`](../src/test/kotlin/db/messages/MessagesUtil.kt)) instead of directly operating on the DB.
- The abstraction layer doesn't check whether the user IDs it's asked to store exist in the auth system because the DB couldn't know when the user deletes their account. Therefore, the validation of user IDs getting stored is done in the wrapping level (i.e., the GraphQL API level).
- It's possible that the nanosecond a user deletes a private chat, the other user sends a message. Thus, chat deletions delete messages _until_, and not _up to_, the time of deletion.

## Auth

- Always use the [`src/main/kotlin/Auth.kt`](../src/main/kotlin/Auth.kt) and [`src/test/kotlin/AuthUtil.kt`](../src/test/kotlin/AuthUtil.kt) abstraction layers instead of directly using Keycloak's API.
- Usernames must be lowercase because the auth service saves them as such even though it accepts uppercase letters.
- The auth system disallows two users from having the same registered email address.
- The auth system strips leading and trailing whitespace for usernames, first names, and last names.
- There's an [admin panel](auth_admin_panel.md).

## Server

### Development

1. Run the server on http://localhost:80 with autoreload enabled:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        -p dev \
        --project-directory . \
        up
    ```
1. [Set up auth](auth_setup.md).

### Testing

1. It takes a minute to start the services. You should spin them up once by running:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        up --scale chat=0 -d
    ```
1. Since the testing environment disables the sending of emails, the [`.example.env`](.example.env) file will suffice. You should enter the shell to run tests. It takes a negligible amount of time to enter the shell if the services have started. You can enter the shell any number of times you want by running:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        run --rm --service-ports chat bash
    ```
1. Reports save to `build/reports/tests/test/`. Update the code and run tests any number of times: 
    1. `gradle test`
    1. Optionally, debug:
        1. Wait for `Listening for transport dt_socket at address: 5005` to be printed.
        1. Run `jdb -attach 5005` in another terminal.
        1. Run `exit` in the debugger's terminal once you're done. 
1. Once you're done with the shell, run `exit` to shut it down.
1. Once you are finished testing, spin down the services:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        down
    ```

### [Production](production.md)

## [Spec](spec.md)