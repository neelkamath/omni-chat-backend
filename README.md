# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

For people who need to communicate via instant messaging, Omni Chat is a free, open core, federated chat system that can replace every existing chat app. Unlike other chat apps, our product brings together all the useful features of existing services, while leaving out their annoying parts.

The [spec](docs/spec.md) explains how Omni Chat differentiates itself from existing services, and which features have been implemented so far. This repo is for the backend. There is no frontend yet.

## Installation

1. Grab a [release](https://github.com/neelkamath/omni-chat/releases), and then continue these steps from the source code you downloaded.
1. Install the [application](docs/install.md).
1. Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on the [schema](src/main/resources/schema.graphqls).

## Usage

- [Running the application](docs/production.md)
- [Docs](docs/api.md)
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
