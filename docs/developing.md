# Developing

It is worth noting that two users cannot have the same registered email address.

## Flow

Here's how a standard project iteration looks like.

1. Pick a feature to work on using the [spec](spec.md). Since the spec only lists features end-users are aware of, and not implementation details, the feature doesn't have to be present in the spec. If the feature isn't present in the spec, but is relevant to a technical (e.g., a programmer who will make bots for the service) or nontechnical end-user, add it to the spec. 
1. If the feature is an HTTP API endpoint, plan it in the [OpenAPI spec](openapi.yaml).
1. Create any required [models](../src/main/kotlin/Models.kt).
1. Write tests (i.e., TDD). If you're writing tests for an HTTP API endpoint, perform the following sub-steps.
    1. Create a file in [`src/test/kotlin/routes`](../src/test/kotlin/routes) named using the format `<TAG>Test.kt`, where `<TAG>` is the feature's tag in the OpenAPI spec. For example, the JWT feature's `/jwt-request` and `/jwt-refresh` endpoints are tagged `jwt` in the OpenAPI spec, and are therefore have their tests in [`src/test/kotlin/routes/JwtTest.kt`](../src/test/kotlin/routes/JwtTest.kt).
    1. Create functions for each endpoint in the newly created file, each of which return a `io.ktor.server.testing.TestApplicationResponse`. The functions should be named the operation ID used by the endpoint in the OpenAPI spec.
    1. Create classes for the endpoints using the format `<HTTP_VERB><ENDPOINT>Test`. For example, if the endpoint `/chat-message` accepts the POST and PATCH verbs, then two classes named `PostChatMessageTest` and `PatchChatMessageTest` would be created.
1. Implement the feature.

    If the feature is an HTTP API endpoint, implement it in files created mirroring the test source set's structure. For example, the JWT feature's tests are in [`src/test/kotlin/routes/JWTTest.kt`](../src/test/kotlin/routes/JwtTest.kt), and therefore its implementation is in [`src/main/kotlin/routes/JWT.kt`](../src/main/kotlin/routes/Jwt.kt).
1. If the feature was from the [spec](spec.md), mark it as completed.
1. If you have updated the server's functionality, or the OpenAPI spec, follow these sub-steps to create a new release.
    1. Update the version in the [OpenAPI spec](openapi.yaml) and the [build file](../build.gradle.kts). Even if the server is still backwards compatible, you must bump the major version if you've renamed an entity in the OpenAPI spec (e.g., a key under `schemas/components/`). This is because [OpenAPI Generator](https://openapi-generator.tech/) uses the names of keys when creating client SDKs, which would have otherwise become backwards-incompatible.
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created, and the new docs will be hosted.

## Server

### Abstraction Layers

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
1. [Set up authentication](auth_setup.md).

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
1. Enter into the shell. The connection will be refused continuously for approximately one minute until the required services are up and running. You do not need to worry about having valid email credentials in `.env` because the tests do not fail if emails aren't actually sent.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.test.yml \
        -p test \
        --project-directory . \
        run --rm chat bash
    ```
1. Test by running `gradle test` whenever you want. Build reports save to `build/reports/tests/test/`.
1. Run `exit` once you're done.

### [Production](production.md)

## [Authentication](authentication.md)

## [Spec](spec.md)

## OpenAPI Spec

[`openapi.yaml`](openapi.yaml) is the OpenAPI spec. Top-level keys under `paths` must be alphabetically ordered. Top-level sub-keys under `components` (e.g., keys under `components/schemas`) must be alphabetically ordered.

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