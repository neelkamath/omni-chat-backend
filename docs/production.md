# Production

## Setup

1. Start the server on http://localhost:80:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        up --build -d
    ```
1. [Set up auth](auth_setup.md).
1. To shut down:
    ```
    docker-compose \
        -f docker/docker-compose.yml \
        -f docker/docker-compose.prod.yml \
        --project-directory . \
        down
    ```


## Operating

- There's an [auth admin panel](auth_admin_panel.md).
- The `proxy` service handles log emission. For example, you can view logs by running `docker logs omni-chat_proxy_1`.
- The `chat` service can be scaled freely, and will automatically be load balanced. You'll have to research how to scale the other services, such as when you're replicating containers using Docker Swarm.
- You should use [Docker secrets](https://docs.docker.com/engine/swarm/secrets/) instead of the `.env` file.
- You should regularly back up the `auth-db-prod` and `chat-db-prod` volumes.