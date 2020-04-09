# Server

## Abstraction Layers

The following topics discuss, in part, how to create [extensions](https://kotlinlang.org/docs/reference/extensions.html) for abstraction layers which are exclusively required by the test source set. For certain extended functionality, a regular function/variable will suffice. In such cases, use the same conventions (e.g., mirroring the file structure), but use a regular function/variable instead of an extension.

### Authentication

- Never operate on the authentication service's API directly. Always use the `com.neelkamath.omniChat.Auth` abstraction layer instead.
- For the test source set, use the extensions housed in [`src/test/kotlin/Auth.kt`](../src/test/kotlin/Auth.kt).

### DB

- Always use `com.neelkamath.omniChat.db.DB.transact` instead of `import org.jetbrains.exposed.sql.transactions.transaction`.
- Never operate on the DB tables directly. Always use the abstraction layers in `com.neelkamath.omniChat.db` instead.
- The abstraction layer for the database (excluding the tables) is [`src/main/kotlin/db/DB.kt`](../src/main/kotlin/db/DB.kt). For the test source set, use the extensions housed in [`src/test/kotlin/db/DB.kt`](../src/test/kotlin/db/DB.kt).
- Every table and its related functions have their own file. This file has the same name as the `object` it contains, which is named using the format `<TABLE>Data`. The table `object` is named `Table`, and its SQL table name is overridden to provide the actual name. For the test source set, use the extensions housed in its mirror file (e.g., [`src/main/kotlin/db/ContactsData.kt`](../src/main/kotlin/db/ContactsData.kt)'s mirrored file is [`src/test/kotlin/db/ContactsData.kt`](../src/test/kotlin/db/ContactsData.kt)).

## Development

1. Run the server on http://localhost:5000 with autoreload enabled.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.override.yml \
        -p dev \
        --project-directory . \
        up
    ```
1. [Set up authentication](auth_setup.md).

## Testing

1. Spin up the services.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        up --scale chat=0 -d
    ```
1. Enter into the shell.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        run --rm chat bash
    ```
1. Test using `gradle test` whenever you want. Build reports save to `build/reports/tests/test/`.
1. Run `exit` once you're done.

## [Production](production.md)