# Developing

## [Spec](spec.md)

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

## OpenAPI Spec

[`openapi.yaml`](openapi.yaml) is the spec for the REST API.

### Development

```
npx redoc-cli serve docs/openapi.yaml -w
```

Open http://127.0.0.1:8080 in your browser. The documentation will automatically rebuild whenever you save a change to the spec. Refresh the page whenever you want to view the updated documentation.

### Testing

```
npx @stoplight/spectral lint docs/openapi.yaml
```

## GraphQL

- An operation which is a subscription must be named using the format `subscribeTo<UPDATE>` (e.g., `subscribeToMessages`).
- An operation which is a subscription must return a `union` whose name's suffixed with `Subscription` which includes the type `CreatedSubscription` (e.g., `union MessagesSubscription = CreatedSubscription | NewMessage`).
- An `input` for updating a resource must have its name suffixed with `Update` (e.g., `AccountUpdate`).
- A `type` representing an updated resource, such as one returned via a subscription, must have its name prefixed with `Updated` (e.g., `UpdatedAccount`).

Here's how to create Kotlin [models](../src/main/kotlin/Models.kt) for GraphQL types:

|GraphQL|Kotlin|
|---|---|
|`type`|A `data class` or `object`.|
|`input`|`data class`|
|`interface`|`interface`|
|`union`|Create an `interface` sans body, and have the `union`'s types inherit from it.|
|`enum`|`enum class`|
|`scalar`|A `data class`, `typealias`, predefined class (e.g., `String`, `LocalDateTime`), or `object`.|

## DB

When updating [`db/Brokers.kt`](../src/main/kotlin/db/Brokers.kt), name the instance of `com.neelkamath.omniChat.Broker` using the format `<SUBSCRIPTION>Broker`, and name the `data class` used as the `T` supplied to `com.neelkamath.omniChat.Broker` using the format `<SUBSCRIPTION>Asset`, where `<SUBSCRIPTION>` is the operation's return type without the `Subscription` suffix. For example, if a GraphQL operation `Subscription.subscribeToUpdatedChats` returns an `UpdatedChatsSubscription`, its broker and `data class` are named `updatedChatsBroker` and `UpdatedChatsAsset` respectively.

## Testing

- Inline fragments in [`Fragments.kt`](../src/test/kotlin/graphql/operations/Fragments.kt) are named using the format `<TYPE>_FRAGMENT` (e.g., `PRIVATE_CHAT_DELETION_FRAGMENT`). Use the format `<FRAGMENT>_<FIELD>_<ARGUMENT>` when naming variables in fragments. For example, an argument `last` to a field `messages` in a fragment `ChatMessages` would be named `chatMessages_messages_last`.
- Should you require extra functionality for a file, create a mirror utility file in the test source set. For example, [`GroupChatsUtil.kt`](../src/test/kotlin/db/tables/GroupChatsUtil.kt) for [`GroupChats.kt`](../src/main/kotlin/db/tables/GroupChats.kt).
- These test cases which must be implemented when testing [forward](ForwardPaginationTest.kt) and [backward](BackwardPaginationTest.kt) pagination.
- The test source set should mirror the main source set's directory structure. For example, a file in the main source set named `directory/Filename.kt` should have its test in `directory/FilenameTest.kt`. Create a `io.kotest.core.spec.style.FunSpec` (`FunSpec`) for each `class` getting tested (e.g., a class `MyClass` would have a corresponding `MyClassTest` `FunSpec`). Keep tests for top-level functions in a `FunSpec` named after the file (e.g., a top-level function `myFun` in a file `MyFile.kt` would have its test placed in a `MyFileTest` `FunSpec`).
- Each function tested should have its test cases placed in a `io.kotest.core.spec.style.FunSpecDsl.context` (`context`). The argument to the `context` must be the function's name and variables. For example, the argument to `context` for the function `infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"` would be `"Expression<String>.iLike(String)"`. Name the `context` `"init"` when testing a `class`'s `init`. If you're testing a private function through its public interface, the signature in the `context` must be the private function's signature so that you can easily find where its functionality gets tested. If you're testing a function through another function (e.g., a private function, a function which converts `vararg` arguments to a `List` for another function), place the tests in the `context` of the function which is getting tested (i.e., the private function's `context`, etc.). 

## [Auth](auth_admin_panel.md)

## Releasing

1. Update the version in the [build file](../build.gradle.kts).
1. Update the version in the [OpenAPI spec](openapi.yaml).
1. Add an entry to the [changelog](CHANGELOG.md).
1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created.