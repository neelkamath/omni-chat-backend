# Omni Chat Backend

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

Read about the service [here](docs/about.md).

To view a previous version's docs, go to `https://github.com/neelkamath/omni-chat-backend/tree/<VERSION>`, where `<VERSION>` is the [release tag](https://github.com/neelkamath/omni-chat-backend/tags) (e.g., `v0.1.1`).

Here are the guides for running the server using [Docker Compose](docs/docker-compose.md) (recommended for local development), and the [cloud](docs/cloud.md) (recommended for production).

Read these [recommendations](docs/frontend-recommendations.md) if you're a developer creating a frontend UI which uses this API.

Here are some notable projects built using the Omni Chat API. If you've built an app, library, etc., create a [GitHub issue](https://github.com/neelkamath/omni-chat-backend/issues/new) to have it added to this list.
- (Official) [Web app](https://github.com/neelkamath/omni-chat-web)
- (Official) [JavaScript API wrapper library](https://www.npmjs.com/package/@neelkamath/omni-chat)

## Usage

**Note**: Operations starting with `Query`, `Mutation`, and `Subscription` (e.g., `Query.readChat`) are from the GraphQL API. Operations starting with `/` (e.g., `/profile-pic`) are from the REST API.

- [Docs](docs/api.md)
    - Download the `rest-api.html` asset from a [release](https://github.com/neelkamath/omni-chat-backend/releases). It'll be referenced in the docs.
    - Optionally, generate a wrapper for the GraphQL API using [GraphQL Code Generator](https://graphql-code-generator.com/) on [`schema.graphqls`](src/main/resources/schema.graphqls).
    - Optionally, generate a wrapper for the REST API using [OpenAPI Generator](https://openapi-generator.tech/) on [`openapi.yaml`](docs/openapi.yaml). Note that backwards compatible REST API updates don't guarantee backwards compatible wrappers. For example, a wrapper for REST API v0.3.1 may not be backwards compatible with a wrapper for REST API v0.3.0.
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
