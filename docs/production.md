# Production

## Setup

1. Start the server on http://localhost:80 (Docker Swarm isn't supported):
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

- The auth system has an [admin panel](auth_admin_panel.md).
- The `chat` service can be scaled freely.
- When restoring backups for the `chat-db` and `auth` services, make sure the backups had been taken at the same time because they're dependent on each other's state.
- The `chat` service is dependent on other services, such as `auth`. Please see the docs of the other services to operate them. For example, the `auth` service uses the [Keycloak](https://hub.docker.com/r/jboss/keycloak) image, which documents how to back up, restore, etc. it.