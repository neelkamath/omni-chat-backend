# Running the Server in the Cloud

This is the recommended method to run the server in the cloud. For example, you might use this method if you're running Omni Chat in production. The server starts up within seconds and can be replicated which makes it a good candidate for serverless environments.

## Installation

1. Set up [PostgreSQL 12.4](https://www.postgresql.org/) (e.g.,
   GCP [Cloud SQL](https://cloud.google.com/sql/docs/postgres/)).
1. Set up [Redis 6.0.8](https://redis.io) (e.g., GCP [Memorystore](https://cloud.google.com/memorystore/)). Since we
   only use the message brokering feature, you needn't persist data to the disk.
1. Set up the server:
   1. Run
      the `neelkamath/omni-chat:0.11.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat)
      .
   1. Set the [environment variables](env.md).
   1. Connect to PostgreSQL and Redis.

### Migrating

Here's how to migrate from the previous version (0.10.0) to this version (0.11.0):

1. The new version can run alongside the previous version while the old instances are shutting down since they are
   backwards-compatible.
1. Run
   the `neelkamath/omni-chat:0.11.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat). 
