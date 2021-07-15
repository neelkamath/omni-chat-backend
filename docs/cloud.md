# Running the Server in the Cloud

This is the recommended method to run the server in the cloud. For example, you might use this method if you're running Omni Chat in production. The server starts up within seconds and can be replicated which makes it a good candidate for serverless environments.

## Installation

1. Set up [PostgreSQL 12.4](https://www.postgresql.org/) (e.g., [Cloud SQL](https://cloud.google.com/sql/docs/postgres/) on GCP, [RDS](https://aws.amazon.com/rds/postgresql/) on AWS).
1. Set up [Redis 6.0.8](https://redis.io) (e.g., [Memorystore](https://cloud.google.com/memorystore/) on GCP, [Redis](https://aws.amazon.com/redis/) on AWS). Since we only use the message brokering feature, you needn't persist data to the disk.
1. Set up the server:
   1. Run the `neelkamath/omni-chat-backend:0.24.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat-backend) on port 80 (e.g., [Cloud Run](https://cloud.google.com/run/) on GCP, [Fargate](https://aws.amazon.com/fargate/) on AWS). It's recommended to have a minimum of 1 vCPU, and 512 MiB of memory allocated.
   1. Set the [environment variables](env.md).
   1. Connect to PostgreSQL and Redis.

### Migrating

Here's how to migrate from the previous version (0.23.0) to this version (0.24.0):

1. Since the new version is backwards-incompatible, the previous version's instances must be stopped before running the new version.
1. Wipe the DB because the schema changed.
1. Update the [environment variables](env.md) because they've changed.
1. Run the `neelkamath/omni-chat-backend:0.24.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat-backend).
