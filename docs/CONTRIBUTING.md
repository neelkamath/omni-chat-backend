# Contributing

## Forking

If you're forking this repo, and want the CI/CD pipeline to run, you'll need to create the following [secrets](https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets#creating-encrypted-secrets-for-a-repository):
- Create secrets for the variables starting with `SMTP_` (e.g., `SMTP_HOST`) documented in [`config.md`](config.md).
- To upload the Docker image, you'll need to [create a Docker Hub account](https://hub.docker.com/signup), and create an [access token](https://docs.docker.com/docker-hub/access-tokens/). Store the access token as the value of the `DOCKER_HUB_PASSWORD` secret.

## Installation

1. Install [Docker](https://docs.docker.com/get-docker/).
1. Install [OpenJDK 14 (HotSpot)](https://adoptopenjdk.net/?variant=openjdk14&jvmVariant=hotspot).
1. Install the latest [node.js](https://nodejs.org/en/download/) LTS.
1. Clone the repository using one of the following methods:
    - SSH: `git clone git@github.com:neelkamath/omni-chat.git`
    - HTTPS: `git clone https://github.com/neelkamath/omni-chat.git`
1. `cd omni-chat`
1. [Configure](config.md).

## [Developing](developing.md)
