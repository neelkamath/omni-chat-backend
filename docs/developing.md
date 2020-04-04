# Developing

## Flow

Here's how a standard project iteration looks like.

1. If you're updating the version of Gradle used, remember to update the Gradle image tags used in [`docker/`](../docker).
1. Pick a feature from the [spec](spec.md) to work on.
1. Plan the feature in the [OpenAPI spec](openapi.yaml).
1. Implement the feature.
1. Mark the feature you built as completed in the [spec](spec.md).
1. If you have updated the server or OpenAPI spec, follow these steps to create a new release.
    1. Update the version in the [OpenAPI spec](openapi.yaml) and the [build file](../build.gradle.kts).
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created, and the new documentation will be hosted.

## Server

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

## [Authentication](authentication.md)

### [Production](production.md)

## [Spec](spec.md)

## OpenAPI Spec

[`openapi.yaml`](openapi.yaml) is the OpenAPI spec.

### Development

```
npx redoc-cli serve docs/openapi.yaml -wp 8081
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