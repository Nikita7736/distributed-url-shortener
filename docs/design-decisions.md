# Design Decisions

## Why Base62?

Base62 uses:

- a-z
- A-Z
- 0-9

Benefits:

- URL safe
- Short identifiers
- Large key space

---

## Why Redis?

Redis provides:

- In-memory lookups
- Sub-millisecond latency
- Reduced database pressure

---

## Why Kafka?

Analytics processing should not affect redirect speed.

Kafka allows analytics events to be processed asynchronously.

---

## Why PostgreSQL?

- Reliable
- ACID compliant
- Strong indexing support
- Mature ecosystem

---

## Why HTTP 308?

The redirect endpoint uses HTTP 308 Permanent Redirect.

Benefits:

- Preserves the original HTTP method
- Explicitly indicates a permanent redirect
- More correct than HTTP 302 for permanent URL mappings

Implementation:

```java
return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);