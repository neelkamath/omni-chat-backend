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

- Note that you don't need to understand anything about the services used by the application. For example, the auth system uses a DB, but whatever needs to be known about it is explicitly documented here.
- The auth system has an [admin panel](auth_admin_panel.md).
- The `proxy` service handles log emission. View logs by running `docker logs proxy`.
- You can freely scale services (i.e., `docker-compose up --scale`) which allow you to, and they will automatically be load balanced.