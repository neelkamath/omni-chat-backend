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
- You can freely scale services (i.e., `docker-compose up --scale`) which allow you to, and they'll automatically be load balanced.
- You might want to automate backups for the `chat-db` and `auth` services.
- When restoring backups for the `chat-db` and `auth` services, make sure the backups had been taken at the same time because they must be perfectly synced.

### `chat-db` Service

Here's how to back up the chat's DB:
``` 
docker exec chat-db sh -c 'pg_dumpall -cU postgres | gzip > /var/backups/dump.gz'
docker cp chat-db:/var/backups/dump.gz backup.gz
```
This will save the backup to a file named `backup.gz` in the present working directory.

Here's how to restore a backup:
```
docker cp backup.gz chat-db:/var/backups/restore.gz
docker exec chat-db sh -c 'gunzip --stdout /var/backups/restore.gz | psql -U postgres'
```
This will restore a backup from a file named `backup.gz`, which is assumed to exist in the present working directory.

### `auth` Service

Here's how to back up the auth system's data:
1. Create a backup:
    ```
    docker exec auth /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=export \
        -Dkeycloak.migration.provider=dir \
        -Dkeycloak.migration.dir=/tmp/backup
    ```
1. Logs continuously emit until the auth system starts. Wait for something similar to the following to get printed:
    ```
    13:25:31,837 INFO  [org.jboss.as] (Controller Boot Thread) WFLYSRV0025: Keycloak 10.0.2 (WildFly Core 11.1.1.Final) started in 25663ms - Started 592 of 890 services (606 services are lazy, passive or on-demand)
    ```
1. Stop streaming logs:
    - macOS: `command+C`
    - Others: `Ctrl+C`
1. Copy the backup to a directory named `backup` in the present working directory:
    ``` 
    docker cp auth:/tmp/backup backup
    ```

Here's how to restore a backup:
1. Assuming the present working directory has the backup in a directory named `backup`, copy it into the container:
    ```
    docker cp backup auth:/tmp/backup
    ```
1. Import:
    ``` 
    docker exec auth /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=import \
        -Dkeycloak.migration.provider=dir \
        -Dkeycloak.migration.dir=/tmp/backup
    ```
1. Logs continuously emit until the auth system starts. Wait for something similar to the following to get printed:
    ```
    13:25:31,837 INFO  [org.jboss.as] (Controller Boot Thread) WFLYSRV0025: Keycloak 10.0.2 (WildFly Core 11.1.1.Final) started in 25663ms - Started 592 of 890 services (606 services are lazy, passive or on-demand)
    ```
1. Stop streaming logs:
    - macOS: `command+C`
    - Others: `Ctrl+C`
1. Restart container (there'll be an approximate downtime of one minute): `docker container restart auth`