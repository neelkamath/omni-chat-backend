# Developing

## Flow

Here's what a standard project iteration looks like.

1. Pick a feature to work on using the [project board](https://github.com/neelkamath/omni-chat/projects/1).
1. Update the [GraphQL schema](../src/main/resources/schema.graphqls).
1. Create [JSON models](../src/main/kotlin/Json.kt).
1. When updating the DB, keep in mind the following.
    - This data may need to be wiped when an account is deleted. For example, the user's messages must be deleted when they delete their account.
    - Subscribers may need to be notified whenever this data is created. For example, deleting a chat requires notifying subscribers of such.
1. Write tests.

    We need to test both the HTTP/WebSocket interface, and the GraphQL operations. The HTTP/WebSocket interface provides a wrapper for the GraphQL operations which is light enough to be easily used as a substitute for the GraphQL engine (i.e., `graphql.GraphQL`) while still providing the same functionality we require from the engine. Hence, we only test GraphQL operations through the HTTP/WebSocket interface so that we needn't write the same test twice (i.e., a unit test using the GraphQL engine, and an integration test using the HTTP/WebSocket interface). For example, if you are testing `Subscription.messageUpdates`, and need to use `Mutation.createMessage` in the test, use the HTTP interface for `Mutation.createMessage` instead of the GraphQL engine directly. If you're writing tests for the GraphQL API, perform the following steps.
    1. Create inline fragments in [`Fragments.kt`](../src/test/kotlin/graphql/api/Fragments.kt) named using the format `<TYPE>Fragment` (e.g., `PRIVATE_CHAT_DELETION_FRAGMENT`).    
    1. Create a file named using the format `<OPERATION>Test.kt` (e.g., `DeleteAccountTest.kt`).
    1. Create a `const val` `String` for the GraphQL document's query named using the format `<OPERATION>_QUERY` (e.g., `MESSAGE_UPDATES_QUERY`).
    1. Follow these steps for `Query`s and `Mutation`s.
        1. Create a private function which deals with the HTTP request (the "operator function"). Name it using the format `operate<OPERATION>` (e.g., `operateReadChats`), and have it return a `com.neelkamath.omniChat.test.graphql.GraphQlResponse`.
        1. Create a function which deals with the HTTP response's data. Name it the GraphQL operation (e.g., `readChats`). It must return the operator function's `com.neelkamath.omniChat.test.graphql.GraphQlResponse.data` mapped to a Kotlin type (e.g., `List<com.neelkamath.omniChat.Chat>`).
        1. When required, create a function which deals with the HTTP response's error message. Name it using the format `err<OPERATION>` (e.g., `errCreateAccount`). It must return the operator function's `com.neelkamath.omniChat.test.graphql.GraphQlResponse.errors[0].message`.
    1. Follow these steps for `Subscription`s.
        1. Create a function which creates the subscription request. Name the function `build<OPERATION>Request` (e.g., `buildMessageUpdatesRequest`), and have it return a `com.neelkamath.omniChat.GraphQlRequest`.
        1. Create a function which receives the events. Name the function `operate<OPERATION>` (e.g., `operateMessageUpdates`).
    1. Create a class named using the format `<OPERATION>Test` (e.g., `ReadChatsTest`).
1. Implement the feature. For `Query`s and `Mutation`s, name the resolver the same as the GraphQL operation (e.g., `readChats`). For `Subscription`s, name the resolver named using the format `operate<OPERATION>` (e.g., `routeMessageUpdates`).
1. Mark the feature as completed in the [spec](spec.md).
1. Create a new release.
    1. Update the version in the [build file](../build.gradle.kts).
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created.

## Style Guide

- GraphQL uses 2 spaces for query indentation. We use 4 spaces to indent GraphQL queries in Kotlin files for maintainability.
- Use `io.kotest.core.spec.style.FunSpec` for test suites.
- Ignore this point if you're writing functional tests (e.g., tests in `com.neelkamath.omniChat.test.graphql.api`). Each function tested should have its test cases placed in a `io.kotest.core.spec.style.FunSpecDsl.context` (`context`). The argument to `context` must be the function's signature without the packages and top-level namespace. For example, the test suite for `Contacts` would have a `context` with the argument `"create(String, Set<String>)"` for `Contacts.create(String, Set<String>)`. If you're testing a private function through its public interface, the signature in the `context` must be the private function's signature so that you can easily find where its functionality is tested.

## DB

- Always use the abstraction layer ([`src/main/kotlin/db/`](../src/main/kotlin/db) and its mirror files in [`src/test/kotlin/db/`](../src/test/kotlin/db)) instead of directly operating on the DB.
- The [abstraction layer](../src/main/kotlin/db) isn't concerned whether the user IDs it processes exist in the auth system.
- It is possible that the nanosecond a user deletes a private chat, the other user sends a message. Thus, chat deletions delete messages _until_, and not _up to_, the time of deletion.
- You may need to drop data if you've updated the schema (e.g., renamed a column, updated a type):
    1. Spin up the services:
        ``` 
        docker-compose \
            -f docker/docker-compose.yml \
            -f docker/docker-compose.test.yml \
            -p test \
            --project-directory . \
            up --scale chat=0 -d
        ```
    1. `docker run --rm -it --network test_chat-db postgres:12.3 psql -h test_chat-db_1 -U postgres`
    1. When prompted for the password, enter the value of the `CHAT_DB_PASSWORD` environment variable.
    1. Drop the relevant data.
        - Drop tables (code taken from [this Stack Overflow QA](https://stackoverflow.com/a/36023359/6354805)):
            ```
            DO $$ DECLARE
                r RECORD;
            BEGIN
                FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP
                    EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
                END LOOP;
            END $$;
            ```
        - Drop types: `DROP TYPE <TYPE>;` (e.g., `DROP TYPE message_status;`)
    1. `exit`

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

1. Spin up the services:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        up --scale chat=0 -d
    ```
1. Since the testing environment disables the sending of emails, the [`.example.env`](.example.env) file will suffice. Enter the shell:
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
    1. Optionally, debug.
        1. Wait for `Listening for transport dt_socket at address: 5005` to be printed.
        1. Run `jdb -attach 5005` in another terminal.
        1. Run `exit` in the debugger's terminal once you're done. 
1. `exit`

### [Production](production.md)

## Auth

- Usernames must be lowercase because the auth service saves them as such.
- The auth system disallows two users from having the same registered email address.
- The auth system strips leading and trailing whitespace for usernames, first names, and last names.
- There is an [admin panel](auth_admin_panel.md).
- Always use the [`src/main/kotlin/Auth.kt`](../src/main/kotlin/Auth.kt) and [`src/test/kotlin/Auth.kt`](../src/test/kotlin/Auth.kt) abstraction layers instead of directly using Keycloak's API.

## [Spec](spec.md)