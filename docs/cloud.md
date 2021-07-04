# Running the Server in the Cloud

This is the recommended method to run the server in the cloud. For example, you might use this method if you're running Omni Chat in production. The server starts up within seconds and can be replicated which makes it a good candidate for serverless environments.

## Installation

1. Set up [PostgreSQL 12.4](https://www.postgresql.org/) (e.g., [Cloud SQL](https://cloud.google.com/sql/docs/postgres/) on Google Cloud, [RDS](https://aws.amazon.com/rds/postgresql/) on AWS).
1. Set up [Redis 6.0.8](https://redis.io) (e.g., [Memorystore](https://cloud.google.com/memorystore/) on Google Cloud, [Redis](https://aws.amazon.com/redis/) on AWS). Since we only use the message brokering feature, you needn't persist data to the disk.
1. Set up the server:
   1. Run the `neelkamath/omni-chat-backend:0.23.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat-backend) (e.g., [Cloud Run](https://cloud.google.com/run/) on Google Cloud, [Fargate](https://aws.amazon.com/fargate/?whats-new-cards.sort-by=item.additionalFields.postDateTime&whats-new-cards.sort-order=desc&fargate-blogs.sort-by=item.additionalFields.createdDate&fargate-blogs.sort-order=desc) on AWS).
   1. Set the [environment variables](env.md).
   1. Connect to PostgreSQL and Redis.

### Migrating

Here's how to migrate from the previous version (0.22.0) to this version (0.23.0):

1. Since the new version is backwards-incompatible, the previous version's instances must be stopped before running the new version.
1. Wipe the DB because the schema changed.
1. Run the `neelkamath/omni-chat-backend:0.23.0` [Docker Hub image](https://hub.docker.com/repository/docker/neelkamath/omni-chat-backend).
