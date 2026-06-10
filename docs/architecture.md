# Architecture Overview

## Goal

Build a scalable URL shortening service capable of handling a large number of redirects while maintaining low latency.

---

## Components

### Spring Boot Application

Responsible for:

- URL creation
- URL resolution
- Analytics retrieval
- Expiration validation

---

### PostgreSQL

Stores:

- Original URL
- Short code
- Click count
- Creation timestamp
- Expiration timestamp

---

### Redis

Used as a cache layer.

Caches:

shortCode → originalUrl

Benefits:

- Faster redirects
- Reduced database load
- Better scalability

---

### Kafka

Used for asynchronous event processing.

Redirect requests publish analytics events without blocking the redirect flow.

Benefits:

- Low redirect latency
- Better throughput
- Decoupled analytics processing

---

### NGINX

Acts as a reverse proxy and load balancer.

Benefits:

- Traffic distribution
- Horizontal scaling
- High availability