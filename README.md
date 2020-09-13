# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

The [spec](docs/spec.md) explains how Omni Chat differentiates itself from existing services, and which features have been implemented so far. This repo is for the backend API. There's no frontend UI yet.

To view a previous version's docs, go to `https://github.com/neelkamath/omni-chat/tree/<VERSION>`, where `<VERSION>` is the release tag (e.g., `v0.1.1`).

## Installation

1. Install [Docker](https://docs.docker.com/get-docker/).
1. Download the `rest-api.html` asset from a [release](https://github.com/neelkamath/omni-chat/releases).
1. Download [`dockerize`](docker/dockerize).
1. If you're not using Windows, run `chmod +x dockerize`.
1. Download [`docker-compose.yml`](docs/docker-compose.yml).
1. [Configure](docs/config.md).
1. Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on [`schema.graphqls`](src/main/resources/schema.graphqls).
1. Optionally, generate a wrapper for the REST API using [OpenAPI Generator](https://openapi-generator.tech/) on [`openapi.yaml`](docs/openapi.yaml). Note that backwards compatible REST API updates don't guarantee backwards compatible wrappers. For example, a wrapper for REST API v0.3.1 may not be backwards compatible with a wrapper for REST API v0.3.0.

## Usage

- [Docs](docs/api.md)
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)
- The `chat` service is dependent on other services, such as `db`. Please see the docs of the other services to operate them in production. For example, the `db` service uses the [Postgres](https://hub.docker.com/_/postgres) image which documents how to back up, scale, etc. it.

### Running the Application

Start the server on http://localhost:80 (it'll take a few seconds to start): `docker-compose up -d`

To shut down: `docker-compose down`

### Migrating to a Newer Version

Since the application is still pre-release software quality, DB migrations aren't provided. This means you must delete the existing database when you migrate to a newer version:
1. Ensure the application isn't running: `docker-compose down`
1. `docker volume rm <DIR>_db` where `<DIR>` is the lowercase name of the present working directory
1. Delete the server files (e.g., `.env`, `docker-compose.yml`), and follow the **Installation** and **Usage** instructions again because the server setup may have changed.

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).