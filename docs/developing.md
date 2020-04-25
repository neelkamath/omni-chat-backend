# Developing

## Flow

Here's what a standard project iteration looks like.

1. Pick a feature to work on using the [project board](https://github.com/neelkamath/omni-chat/projects/1).
1. Update the [GraphQL schema](../src/main/resources/schema.graphql) or [OpenAPI spec](openapi.yaml) if required.
1. Create any required [JSON models](../src/main/kotlin/Json.kt).
1. If you're updating the DB, keep in mind that you might have to wipe this data when an account is deleted.
1. Write tests (i.e., TDD). If you're writing tests for the API, perform the following sub-steps.
    - HTTP API
        1. Create a function to deal with the HTTP request and response in [`RoutingTest.kt`](../src/test/kotlin/RoutingTest.kt). Name it the operation ID used by the endpoint in the OpenAPI spec, and have it return a `io.ktor.server.testing.TestApplicationResponse`.
        1. Create a class to hold the tests. Name it using the format `<HTTP_VERB><URL>Test`. For example, if the endpoint `/jwt-refresh` accepts the POST verb, then the class `PostJwtRefreshTest` would be created.
    - GraphQL API
        1. Create two functions in the `com.neelkamath.omniChat.test.graphql` package to deal with the HTTP request and response. One function should be named using the format `operate<OPERATION>` (e.g., `operateReadChats`), and return a `com.neelkamath.omniChat.test.graphql.GraphQlResponse`. The other function should be named the GraphQL operation (e.g., `readChats`), and return the operation's data mapped to a Kotlin type (e.g., `List<com.neelkamath.omniChat.Chat>`).
        1. Create a class to hold the tests. Name it using the format `<OPERATION>Test` (e.g., `ReadChatsTest`).
1. Implement the feature.

    If the feature is an HTTP API endpoint, name the routing function the same as its operation ID in the OpenAPI spec. If the feature is a GraphQL operation, name it the same as the operation.
1. If the feature is in the [spec](spec.md), mark it as completed.
1. If you have updated the server's functionality, the OpenAPI spec, or the GraphQL schema, follow these sub-steps to create a new release.
    1. Update the version in the [OpenAPI spec](openapi.yaml), and the [build file](../build.gradle.kts). Even if the server is still backwards compatible, you must bump the major version if you've renamed an entity in the OpenAPI spec (e.g., a key under `schemas/components/`). This is because [OpenAPI Generator](https://openapi-generator.tech/) uses the names of keys when creating client SDKs, which would have otherwise become backwards-incompatible.
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created, and the new docs will be hosted.

## Server

### Notes

- Always use the [`src/main/kotlin/Auth.kt`](../src/main/kotlin/Auth.kt) and [`src/test/kotlin/Auth.kt`](../src/test/kotlin/Auth.kt) abstraction layers instead of directly using Keycloak's API.
- Always use the [`src/main/kotlin/db/`](../src/main/kotlin/db) and [`src/test/kotlin/db/`](../src/test/kotlin/db) abstraction layers instead of directly operating on the DB.

### Development

1. Run the server on http://localhost:5000 with autoreload enabled.
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

1. Spin up the services.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        up --scale chat=0 -d
    ```
1. Enter into the shell. You needn't have a valid email configuration in `.env` (e.g., `KEYCLOAK_SMTP_HOST`, `KEYCLOAK_SMTP_PASSWORD`), because the testing environment disables the sending of emails.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        run --rm chat bash
    ```
1. `gradle test` whenever you want. Build reports save to `build/reports/tests/test/`.

### [Production](production.md)

## Auth

- The auth system disallows two users from having the same registered email address.
- The auth system strips leading and trailing whitespace for usernames, first names, and last names.
- There is an [admin panel](auth_admin_panel.md).

## [Spec](spec.md)

## OpenAPI Spec

[`openapi.yaml`](openapi.yaml) is the OpenAPI spec.

### Development

```
npx redoc-cli serve -wp 8081 docs/openapi.yaml
```

The documentation will be served on http://127.0.0.1:8081. It will automatically rebuild when the spec is updated. Refresh the page to view the updated version.

### Testing

```
npx @stoplight/spectral lint docs/openapi.yaml
```

### Production

```
npx redoc-cli bundle docs/openapi.yaml --title 'Omni Chat'
```

The documentation will be saved to `redoc-static.html`.