# Installation

1. Install [Docker](https://hub.docker.com/search/?type=edition&offering=community).
1. If you're developing the project, or want to generate an SDK to use the HTTP API, install the latest [node.js](https://nodejs.org/en/download/) LTS.
1. Clone the repository using one of the following methods.
    - SSH: `git clone git@github.com:neelkamath/omni-chat.git`
    - HTTPS: `git clone https://github.com/neelkamath/omni-chat.git`
1. `cd omni-chat`
1. Create a file named `.env` with the following content, entering the passwords you would like to use on the right hand side of the equals sign.
    ```
    CHAT_DB_PASSWORD=
    KEYCLOAK_DB_PASSWORD=
    KEYCLOAK_PASSWORD=
    ```