# Developing

## Flow

Here's how a standard project iteration looks like.

1. Pick a feature from the [spec](spec.md) to work on.
1. Plan the feature in the [OpenAPI spec](openapi.yaml).
1. Write tests (i.e., TDD). If you're writing tests for an HTTP API endpoint, perform the following sub-steps.
    1. Create a function in [`Server.kt`](../src/test/kotlin/routes/Server.kt) which returns a `TestApplicationResponse`. It's name should be the operation ID used by the endpoint in the [OpenAPI spec](openapi.yaml).
    1. In the relevant file, create classes for the endpoint using the format `<HTTP_VERB><ENDPOINT>Test`. For example, if the endpoint `/chat_message` accepts the POST and PATCH verbs, then two classes named `PostChatMessageTest` and `PatchChatMessageTest` would be created.
1. Implement the feature.
1. Update the spec and tests for deviations from the implementation you had planned.
1. Mark the feature as completed in the [spec](spec.md).
1. If you have updated the server or OpenAPI spec, follow these steps to create a new release.
    1. Update the version in the [OpenAPI spec](openapi.yaml) and the [build file](../build.gradle.kts). Even if the server is still backwards compatible, you must bump the major version if you've renamed an entity in the OpenAPI spec (e.g., a key under `schemas/components/`). This is because [OpenAPI Generator](https://openapi-generator.tech/) uses key names when creating client SDKs, which would have otherwise become backwards-incompatible.
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created, and the new documentation will be hosted.

## [Server](server.md)

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