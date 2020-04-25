# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

For people who need to communicate via chat, Omni Chat is a free, open core, federated chat system that can replace every existing chat app. Unlike other chat apps, our product brings together all the useful features of existing services, while leaving out their annoying parts.

The [spec](docs/spec.md) explains how Omni Chat differentiates itself from existing services.

## Installation

1. Install the [app](docs/install.md).
1. Optionally, generate a wrapper for the HTTP API using [OpenAPI Generator](https://openapi-generator.tech/) using the [OpenAPI spec](docs/openapi.yaml).
1. Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on the [schema](src/main/resources/schema.graphql).

## Usage

- Branding assets are in [`branding/`](branding). 
- [Changelog](docs/CHANGELOG.md).
- Older versions can be found in the [releases](https://github.com/neelkamath/omni-chat/releases).
- We use the term "auth" to refer to "authentication and authorization".

### [Running the App](docs/production.md)

### [Docs](docs/api.md)

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
