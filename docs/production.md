# Production

1. Start the server on http://localhost:80.
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        up --build
    ```
1. [Set up authentication](auth_setup.md).

## [Authentication](authentication.md)