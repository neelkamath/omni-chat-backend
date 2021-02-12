# Running the Server Using Docker Compose

This is the recommended method to run the server locally. For example, you might use this method if you're calling Omni Chat's API on your development machine to build a frontend UI for a chat app.

## Installation

1. Install [Docker](https://docs.docker.com/get-docker/).
1. `mkdir omni-chat`
1. `cd omni-chat`
1. Download [`dockerize`](../docker/dockerize).
1. If you're not using Windows, run `chmod +x dockerize`.
1. Download [`docker-compose.yml`](docker-compose.yml).
1. Create a file named `.env`. Copy-paste the key-value pairs from the [example `.env`](.env) into it, and [set your own values](env.md).

## Usage

Start the server on http://localhost: `docker-compose up -d`

To shut down: `docker-compose down`

## Migrating

Here's how to migrate from the previous version (0.14.0) to this version (0.15.0):

1. Re-download [`docker-compose.yml`](docker-compose.yml).
1. [Use](#usage).
