# Running the Server Using Docker Compose

This is the recommended method to run the server locally. For example, you might use this method if you're calling Omni Chat's API on your development machine to build a frontend UI for a chat app.

## Installation

1. Install [Docker](https://docs.docker.com/get-docker/).
1. Download [`dockerize`](../docker/dockerize).
1. If you're not using Windows, run `chmod +x dockerize`.
1. Download [`docker-compose.yml`](docker-compose.yml).
1. Create a file named `.env`. Copy-paste the key-value pairs from the [example `.env`](.env) into it, and [set your own values](env.md).

## Usage

Start the server on http://localhost:80: `docker-compose up -d`

To shut down: `docker-compose down`

## Migrating

Here's how to migrate from the previous version (v0.8.0) to this version (v0.8.1): 
1. Ensure the application isn't running: `docker-compose down`
1. Since DB migrations aren't provided for this version, you'll have to wipe the DB: `docker volume rm <DIR>_db` where `<DIR>` is the lowercase name of the present working directory
1. Delete the server files (e.g., `.env`, `docker-compose.yml`), and follow the **Installation** and **Usage** instructions again because the server setup may have changed.
