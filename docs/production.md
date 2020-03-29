# Production

```
docker-compose \
    -f docker/docker-compose.yml \
    -f docker/docker-compose.prod.yml \
    --project-directory . \
    up --build
```

The server will be running on http://localhost:80. DB data is stored in `/var/lib/postgresql/data`.