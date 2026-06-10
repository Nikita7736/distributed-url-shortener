# Scalability Considerations

## Bottleneck 1: Database Reads

Without caching:

Every redirect requires a database lookup.

Solution:

Redis cache-aside pattern.

Flow:

Redis → PostgreSQL fallback

---

## Bottleneck 2: Analytics Processing

Problem:

Writing analytics synchronously increases redirect latency.

Solution:

Kafka event pipeline.

Redirect returns immediately while analytics are processed asynchronously.

---

## Bottleneck 3: Traffic Growth

Problem:

Single application instance becomes overloaded.

Solution:

NGINX load balancing.

Multiple Spring Boot instances can be added horizontally.

---

## Future Scaling Options

- Redis Cluster
- PostgreSQL Read Replicas
- Bloom Filters
- Kubernetes Deployment
- Multi-region Architecture