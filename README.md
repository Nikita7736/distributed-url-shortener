<<<<<<< HEAD
# urlshortner
Distributed URL shortener inspired by Bitly, built using Java and Spring Boot. Uses Base62 hashing for collision-free URLs, Redis for caching, and Kafka for event-driven analytics. Designed for high scalability, low latency, and efficient database access with PostgreSQL and Docker deployment.
=======
# URL Shortener (Java + Spring Boot)

## Phase 1
- Short URL generation
- Redirect service
- Expiration support
- Analytics (click count)

## Phase 2 (Scalability)
- Redis caching (optional via `redis` profile)
- DB indexing (JPA indexes)
- Rate limiting (Redis-backed, in-memory fallback)
- Load balancing simulation (Nginx + 2 app instances via Docker Compose)

## Run locally (IntelliJ)
1. Open the project folder.
2. Run `UrlShortnerApplication`.
3. Open UI at `http://localhost:8080/`.

## Enable Redis cache (optional)
Start Redis, then run with:
- `SPRING_PROFILES_ACTIVE=redis`

## Load balancing simulation (Docker)
Requires Docker Desktop.

```bash
docker compose up --build
```

Then open:
- `http://localhost:8080/`

This goes through Nginx, load-balancing between `app1` and `app2`.

>>>>>>> d551f63 (initial commit)
