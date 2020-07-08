# Developing

## Flow

Here's what a standard project iteration looks like.

1. Pick a feature to work on using the [project board](https://github.com/neelkamath/omni-chat/projects/1). Remember that resources might need to be created, read, updated, deleted, searched, and notified. For example, a messaging feature should allow users to create a message, read a message, update a message (i.e., display which users have read the message), delete a message, search for the message, and be notified of a created message.
1. Update the [GraphQL schema](../src/main/resources/schema.graphqls).
    - An operation which is a subscription must be named using the format `subscribeTo<UPDATE>` (e.g., `subscribeToMessages`).
    - An operation which is a subscription must return a `union` whose name's suffixed with `Subscription` which includes the type `CreatedSubscription` (e.g., `union MessagesSubscription = CreatedSubscription | NewMessage`).
    - An `input` for updating a resource must have its name suffixed with `Update` (e.g., `AccountUpdate`).
    - A `type` representing an updated resource, such as one returned via a subscription, must have its name prefixed with `Updated` (e.g., `UpdatedAccount`).
1. Create Kotlin [models](../src/main/kotlin/Models.kt) without duplicating documentation for the GraphQL types:

    |GraphQL|Kotlin|
    |---|---|
    |`type`|A `data class` or `object`.|
    |`input`|`data class`|
    |`interface`|`interface`|
    |`union`|Create an `interface` sans body, and have the `union`'s types inherit from it.|
    |`enum`|`enum class`|
    |`scalar`|A `data class`, `typealias`, predefined class (e.g., `String`, `LocalDateTime`), or `object`.|
    
    Since GraphQL only has lists, you mustn't use `Set`s when converting between GraphQL and Kotlin types. Instead, use something like an `init` block to assert uniqueness. 
1. Write tests.

    If you're writing tests for the GraphQL API, perform the following steps (see [`RefreshTokenSetTest.kt`](../src/test/kotlin/graphql/operations/queries/RefreshTokenSetTest.kt) for an example on queries and mutations, and [`SubscribeToMessagesTest.kt`](../src/test/kotlin/graphql/operations/subscriptions/SubscribeToMessagesTest.kt) for an example on subscriptions):
    1. Create inline fragments in [`Fragments.kt`](../src/test/kotlin/graphql/operations/Fragments.kt) named using the format `<TYPE>_FRAGMENT` (e.g., `PRIVATE_CHAT_DELETION_FRAGMENT`). Use the format `<FRAGMENT>_<FIELD>_<ARGUMENT>` when naming variables in fragments. For example, an argument `last` to a field `messages` in a fragment `ChatMessages` would be named `chatMessages_messages_last`.
    1. Create a file named using the format `<OPERATION>Test.kt` (e.g., `DeleteAccountTest.kt`).
    1. Create a constant `String` value for the GraphQL document's query named using the format `<OPERATION>_QUERY` (e.g., `MESSAGE_UPDATES_QUERY`). The query should be named using the operation (e.g., the operation `searchMessages` would have its query named `SearchMessages`). Variables for arguments to the operation name should be named the same as the argument itself. For example, if an operation `Query.readChat` takes an argument `id`, the argument's corresponding variable should be named `id`.
    1. Follow these steps for `Query`s and `Mutation`s. If the function takes an access token, it must be the first parameter.
        1. Create a private function which deals with the HTTP request (the "operator function"). Name it using the format `operate<OPERATION>` (e.g., `operateReadChats`), and have it return a `com.neelkamath.omniChat.graphql.GraphQlResponse`.
        1. Create a function which deals with the HTTP response's db. Name it the GraphQL operation (e.g., `readChats`). It must return the operator function's `com.neelkamath.omniChat.graphql.GraphQlResponse.db` mapped to a Kotlin type (e.g., `List<Chat>`).
        1. Create a function which deals with the HTTP response's `ClientException` (e.g., `"INVALID_CHAT_ID"`). Name it using the format `err<OPERATION>` (e.g., `errCreateAccount`). It must return the operator function's `com.neelkamath.omniChat.graphql.GraphQlResponse.errors[0].message`.
    1. Follow these steps for `Subscription`s. If the function takes an access token, it must be the first parameter.
        1. Create a private function which deals with opening the WebSocket connection (the "operator function"). Name it using the format `operate<OPERAION>` (e.g., `operateSubscribeToMessages`).
        1. Create a function which deals with the `CreatedSubscription`, and receives events. Name it the GraphQL operation (e.g., `subscribeToMessages`).
        1. Create a function which asserts the `ClientException` received (e.g., `"INVALID_CHAT_ID"`), and asserts that the connection gets closed. Name it using the format `err<OPERATION>` (e.g., `errSubscribeToMessages`).
    1. Create a class named using the format `<OPERATION>Test` (e.g., `ReadChatsTest`).
    1. Write load tests if required. For example, you might want to test that searching messages from a populated DB is fast.
1. Implement the feature. Name the data fetcher the same as the GraphQL operation (e.g., `subscribeToMessages`). When adding a route for a subscription in `com.neelkamath.omniChat.graphql.routing.routeGraphQlSubscriptions`, name the function using the format `route<SUBSCRIPTION>` where `<SUBSCRIPTION>` is the operation's return type (e.g., the operation `Subscription.subscribeToMessages` has its router named `routeMessagesSubscription()` because it returns a `MessagesSubscription`).
1. When updating [`db/Brokers.kt`](../src/main/kotlin/db/Brokers.kt), name the instance of `com.neelkamath.omniChat.Broker` using the format `<SUBSCRIPTION>Broker`, and name the `data class` used as the `T` supplied to `com.neelkamath.omniChat.Broker` using the format `<SUBSCRIPTION>Asset`, where `<SUBSCRIPTION>` is the operation's return type without the `Subscription` suffix. For example, if a GraphQL operation `Subscription.subscribeToUpdatedChats` returns an `UpdatedChatsSubscription`, its broker and `data class` are named `updatedChatsBroker` and `UpdatedChatsAsset` respectively.
1. Write DB migrations if you've made a backwards-incompatible change.
1. Mark the feature as completed in the [spec](spec.md).
1. Create a new release.
    1. Update the version in the [build file](../build.gradle.kts).
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created.

## Testing

- Should you require extra functionality for a file, create a mirror utility file in the test source set. For example, [`Auth.kt`](../src/main/kotlin/Auth.kt) and [`AuthUtil.kt`](../src/test/kotlin/AuthUtil.kt), [`GroupChats.kt`](../src/main/kotlin/db/tables/GroupChats.kt) and [`GroupChatsUtil.kt`](../src/test/kotlin/db/tables/GroupChatsUtil.kt).
- These test cases which must be implemented when testing [forward](ForwardPaginationTest.kt) and [backward](BackwardPaginationTest.kt) pagination.
- The test source set should mirror the main source set's directory structure. For example, a file in the main source set named `directory/Filename.kt` should have its test in `directory/FilenameTest.kt`. If it's impractical to fit the tests in a single file, create a folder by the name of the file. For example, [`graphql/operations/Mutations.kt`](../src/main/kotlin/graphql/operations/Mutations.kt)'s tests are in [`graphql/operations/mutations/`](../src/test/kotlin/graphql/operations/mutations). Create a `io.kotest.core.spec.style.FunSpec` for each `class` getting tested, and one with the name of the test file for top-level functions.
- Each function tested should have its test cases placed in a `io.kotest.core.spec.style.FunSpecDsl.context` (`context`). The argument to the `context` must be the function's name and variables. For example, the argument to `context` for the function `infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"` would be `"Expression<String>.iLike(String)"`. Name the `context` `"init"` when testing a `class`'s `init`. If you're testing a private function through its public interface, the signature in the `context` must be the private function's signature so that you can easily find where its functionality gets tested. If you're testing a function through another function (e.g., a private function, a function which converts `vararg` arguments to a `List` for another function), place the tests in the `context` of the function which is getting tested (i.e., the private function's `context`, etc.). The only time `context`s aren't used is when the class containing the test cases gets named after the function tested (e.g., [`ReadAccountTest.kt`](../src/test/kotlin/graphql/operations/queries/ReadAccountTest.kt)).

## Auth

There's an [admin panel](auth_admin_panel.md).

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