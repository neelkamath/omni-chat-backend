# Developing

## Server

### Testing

1. Spin up the services:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        --project-directory . \
        up --build --scale chat=0 -d
    ```
1. Enter the shell:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        --project-directory . \
        run --rm --service-ports chat sh -c 'flyway migrate && fish'
    ```
1. Reports save to `build/reports/tests/test/`. Update the code and run tests any number of times:
    1. `gradle test`
    1. Optionally, debug:
        1. Wait for `Listening for transport dt_socket at address: 5005` to be printed.
        1. Run `jdb -attach 5005` in another terminal.
        1. Run `exit` in the debugger's terminal once you're done.
1. Run `exit` to shut down the shell.
1. To shut down the services:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        --project-directory . \
        down
    ```

### Development

1. Run the server on http://localhost with autoreload enabled:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        --project-directory . \
        up --build -d
    ```
1. To shut down:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        --project-directory . \
        down
    ```

### Production

To test the production build:

1. Start the server on http://localhost:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        up --build -d
    ```
1. To shut down:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        down
    ```

## OpenAPI Spec

The REST API spec is [`openapi.yaml`](openapi.yaml).

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

- Since it's understood that the [mutations](../src/main/kotlin/graphql/operations/Mutations.kt), [models](../src/main/kotlin/graphql/routing/Models.kt), etc. are for the [schema](../src/main/resources/schema.graphqls), don't duplicate the schema doc comments in the Kotlin files.
- An operation which is a subscription must be named using the format `subscribeTo<UPDATE>` (e.g., `subscribeToMessages`).
- An operation which is a subscription must return a `union` named using the format `<UPDATE>Subscription`, and includes the type `CreatedSubscription` (e.g., `union MessagesSubscription = CreatedSubscription | NewMessage`).
- An `input` for updating a resource must have its name suffixed with `Update` (e.g., `AccountUpdate`).
- A `type` representing an updated resource, such as one returned via a subscription, must have its name prefixed with `Updated` (e.g., `UpdatedAccount`).
- A `union` returned by a `Query` or `Mutation` must be the operation's name suffixed with `Result` (e.g., the `union` returned by `Query.searchChatMessages` is named `SearchChatMessagesResult`).

Use functions instead of member variables when creating [DTOs](src/main/kotlin/graphql/dataTransferObjects) in order to prevent [overfetching](https://blog.logrocket.com/properly-designed-graphql-resolvers/), and for consistency. Wire each GraphQL scalar directly instead of using the GraphQL Java library's POJO feature because it ties the DB schema to the data fetchers too tightly (e.g., if a deeply nested GraphQL field were to accept an argument later on, it'd be a nightmare to refactor the code to use DTOs). For example, in the following code snippet, `startCursor` is incorrect because it isn't a function even though it fetches the scalar directly, `getEdges()` is incorrect even though it's a function because it doesn't fetch the scalar directly, `getData()` is correct because it's a function which fetches the scalars directly via further DTOs, and `getPageInfo()` is correct because it's a function which resolves the scalar directly:

```kotlin
class MyDto(
    private val messageIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    val startCursor = messageIdList.first()

    fun getEdges(): List<BookmarkedMessageEdge> = Bookmarks.read(messageIdList)

    fun getData(): List<DataDto> = messageIdList.map(::DataDto)

    fun getHasNextPage(env: DataFetchingEnvironment): boolean =
        Bookmarks.readPageInfo(env.userId!!, messageIdList.lastOrNull(), pagination).hasNextPage
}
```

Here's how to create Kotlin [models](../src/main/kotlin/graphql/routing/Models.kt) for GraphQL types:

|GraphQL|Kotlin|
|---|---|
|`type`|`class`|
|`input`|`data class`|
|`interface`|`sealed interface`|
|`union`|Create a `sealed interface` sans body, and have the `union`'s `type`s implement it.|
|`enum`|`enum class`|
|`scalar`|A `data class`, `typealias` for a predefined class (e.g., `String`, `LocalDateTime`), or `object`.|

## Naming Conventions

We use `create` (e.g., `createAccount`), `read` (e.g., `readAccount`), `update` (e.g., `updateAccount`), `delete` (e.g., `deleteAccount`), and `search` (e.g., `searchAccounts`) to name functions when possible. Don't use `get`, `set`, etc. unless needed.

## Writing Tests

- Images must be actual images (e.g., [`76px×57px.jpg`](../src/test/resources/76px×57px.jpg)) because a thumbnail is generated upon upload, and the program will crash if dummy data is provided. Other file formats such as audio should use dummies such as `kotlin.ByteArray`s.
- These test cases must be implemented when testing [forward](ForwardPaginationTest.kt) and [backward](BackwardPaginationTest.kt) pagination.
- The test source set must mirror the main source set. Files containing tests must be named using the format `<FILE>Test.kt` (e.g., `AppTest.kt` for `App.kt`). Files in the main source set which have testing utilities but no tests in the test source set must be named using the format `<FILE>Util.kt`. For example, [`DbUtil.kt`](src/test/kotlin/db/DbUtil.kt) is named `DbUtil.kt` instead of `DbTest.kt` because it contains testing utilities for `Db.kt` but no tests.
- Test cases must be placed in classes named after the class getting tested (e.g., `class ImageTest` for `class Image`). Keep tests for top-level functions in a class named after the file (e.g., the top-level `fun myFun()` in `MyFile.kt` would have its tests placed in `class MyFileTest`).
- Each function tested must have its test cases placed in a `@Nested inner class`. The name of this class must have its first letter capitalized, and `.`s replaced with `_`s. For example, `MyFun` for `fun myFun()`, `Expression_iLike` for `fun Expression<String>.iLike(pattern: String)`, `Init` for an `init`. Test cases must be placed in the `@Nested inner class` of the function getting tested (i.e., if you're testing a private function through its public interface, or testing a function via a convenience function, place the test cases in the class of the function actually getting tested).

## Diagram

Here's a diagram of how the service works. The client application isn't included in this repo but is included in the diagram for the purpose of explanation. PostgreSQL and Redis may be set up as clusters similar to the API server.

![Diagram](diagram.svg)

## Releasing

1. Update the version in the [build file](../build.gradle.kts), [OpenAPI spec](openapi.yaml), and the `chat` service's image in [`docker-compose.yml`](docker-compose.yml).
1. Ensure the [API docs **Operations** section](api.md#operations), [`Types.kt`](../src/main/kotlin/graphql/engine/Types.kt), and [`AppTest.kt`](../src/test/kotlin/AppTest.kt) are up-to-date.
1. Add a [changelog](CHANGELOG.md) entry.
1. Update the steps to migrate to the new version in [`docker-compose.md`](docker-compose.md).
1. Update [`cloud.md`](cloud.md).
1. Commit to the `master` branch to either release a new version or overwrite the previous one.
