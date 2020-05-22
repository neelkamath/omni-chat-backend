# Production

1. Start the server on http://localhost:80. (It is safe to run multiple instances of the `chat` service, since the only shared data is running in a single instance of the `auth` service, etc.)

    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        up --build
    ```

1. [Set up auth](auth_setup.md).

There is an [auth admin panel](auth_admin_panel.md).