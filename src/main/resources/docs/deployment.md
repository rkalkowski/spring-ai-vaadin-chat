# Deploying Acme ERP

Acme ERP ships as a single Spring Boot jar plus a PostgreSQL 17 database.

## Requirements

- Java 21 or newer
- PostgreSQL 17 with the `pgcrypto` extension enabled
- 2 vCPU / 4 GB RAM minimum per application node

## Configuration

Set these environment variables:

- `ACME_DB_URL`, `ACME_DB_USER`, `ACME_DB_PASSWORD`
- `ACME_ENCRYPTION_KEY` — 32-byte base64 value, rotated yearly
- `ACME_BASE_URL` — the public URL, used in emails and SSO redirects

## Scaling

The application is stateless apart from HTTP sessions, which are stored in the
database. Run as many nodes as you need behind a load balancer with sticky
sessions disabled. Background jobs (invoicing, nightly reconciliation) run only
on the node holding the `acme.scheduler` advisory lock, so no extra coordination
is required.

## Backups

Take a nightly logical dump plus continuous WAL archiving. Restoring the database
is sufficient to restore the application; nodes hold no local state.
