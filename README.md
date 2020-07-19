# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

For people who need to communicate via instant messaging, Omni Chat is a free, open core, federated chat system that can replace every existing chat app. Unlike other chat apps, our product brings together all the useful features of existing services, while leaving out their annoying parts.

The [spec](docs/spec.md) explains how Omni Chat differentiates itself from existing services, and which features have been implemented so far. This repo is for the backend. There is no frontend yet.

To view a previous version's docs, go to `https://github.com/neelkamath/omni-chat/tree/<VERSION>`, where `<VERSION>` is the release tag (e.g., `v0.1.0`).

## Installation

1. Download the `rest-api.html` asset from a [release](https://github.com/neelkamath/omni-chat/releases).
1. Download [`dockerize`](docker/dockerize).
1. Download [`docker-compose.yml`](docs/docker-compose.yml).
1. Install [Docker](https://docs.docker.com/get-docker/).
1. [Configure](docs/config.md).
1. Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on [`schema.graphqls`](src/main/resources/schema.graphqls).

## Usage

- [Docs](docs/api.md)
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)

### Running the Application

1. Start the server on http://localhost:80: `docker-compose up -d`
1. [Set up the auth system](docs/auth_setup.md) if you haven't already.
1. To shut down: `docker-compose down`

- The auth system's admin panel runs on http://localhost:80/auth/admin. The username is `admin`, and the password is the value of `KEYCLOAK_PASSWORD` in the `.env` file. Note that users must only be created and deleted via Omni Chat's API because custom user data gets stored on a DB separate from the auth system's.
- The `chat` service can be scaled freely.
- The `chat` service is dependent on other services, such as `auth`. Please see the docs of the other services to operate them in production. For example, the `auth` service uses the [Keycloak](https://hub.docker.com/r/jboss/keycloak) image, which documents how to back up, restore, etc. it.
- When restoring backups for the `chat-db` and `auth` services, make sure the backups had been taken at the same time because they're dependent on each other's state.

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
