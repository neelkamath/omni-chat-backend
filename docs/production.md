# Production

Start the server on http://localhost:80:

```
docker-compose \
    -f docker/docker-compose.yml \
    -f docker/docker-compose.prod.yml \
    --project-directory . \
    up --build
```

## [Authentication](authentication.md)