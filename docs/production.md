# Production

1. Start the server on http://localhost:80 (it's safe to run multiple instances of the `chat` service):
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        up --build
    ```
1. [Set up auth](auth_setup.md).

There's an [auth admin panel](auth_admin_panel.md).