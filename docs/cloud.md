# Running the Server in the Cloud

This is the recommended method to run the server in the cloud. For example, you might use this method if you're running Omni Chat in production. The server starts up within seconds and can be replicated which makes it a good candidate for serverless environments.

## Running

1. Set up [PostgreSQL 12.4](https://www.postgresql.org/) (e.g., GCP [Cloud SQL](https://cloud.google.com/sql/docs/postgres/)).
1. Set up [Redis 6.0.8](https://redis.io) (e.g., GCP [Memorystore](https://cloud.google.com/memorystore/)). Since we only use the message brokering feature, you needn't persist data to the disk.
1. Set up the server:
    1. Run the `neelkamath/omni-chat:0.8.1` Docker image.
    1. Set the [environment variables](env.md).
    1. Connect to PostgreSQL and Redis.

### Migrating

Here's how to migrate from the previous version (v0.8.0) to this version (v0.8.1): 
1. Stop the running servers.
1. Since DB migrations aren't provided for this version, you'll have to wipe the Postgres DB.
1. Start the server using the `neelkamath/omni-chat:0.8.1` Docker image.
