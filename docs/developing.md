# Developing

## Flow

Here's how a standard project iteration looks like.

1. If you're updating the version of Gradle used, remember to update the Gradle image tags used in [`docker/`](../docker).
1. Pick a feature from the [spec](spec.md) to work on.
1. Plan the feature in the [OpenAPI spec](openapi.yaml).
1. Implement the feature.
1. Mark the feature you built as completed in the [spec](spec.md).
1. If you have updated the functionality or the OpenAPI spec, follow these steps to create a new release.
    1. Update the version in the [OpenAPI spec](openapi.yaml) and the [build file](../build.gradle.kts).
    1. Add an entry to the [changelog](CHANGELOG.md).
    1. Commit to the `master` branch. If the CI/CD pipeline passes, a new GitHub release will be created, and the new documentation will be hosted.

## Server

### Development

```
docker-compose \
    -f docker/docker-compose.yml \
    -f docker/docker-compose.override.yml \
    -p dev \
    --project-directory . \
    up
```

The server will be running on http://localhost:5000 with autoreload enabled.

### Testing

```
docker-compose \
    -f docker/docker-compose.yml \
    -f docker/docker-compose.test.yml \
    -p test \
    --project-directory . \
    run --rm chat
```

Run `gradle test` whenever you want. Build reports save to `build/reports/tests/test/`.

### [Production](production.md)

## [Spec](spec.md)

## OpenAPI Spec

[`openapi.yaml`](openapi.yaml) is the OpenAPI spec.

### Development

```
npx redoc-cli serve docs/openapi.yaml -w
```

The documentation will be served on http://127.0.0.1:8080. It will automatically rebuild when the spec is updated. Refresh the page to view the updated version.

### Testing

```
npx @stoplight/spectral lint docs/openapi.yaml
```

### Production

```
npx redoc-cli bundle docs/openapi.yaml -o public/index.html --title 'Omni Chat'
```

The documentation will be saved to `public/index.html`.