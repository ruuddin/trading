# Production Environment Checklist

Use this checklist before any production deployment.

## 1. Access and credentials

- [ ] `jwt.secret` is rotated from default and managed via secure secret store
- [ ] All provider API keys are set by environment variables (no demo keys in production)
- [ ] Database credentials are non-default and rotated
- [ ] Deployment identity and least-privilege roles are validated

## 2. Security controls

- [ ] Session revocation endpoint (`POST /api/auth/sessions/revoke`) is verified
- [ ] 2FA setup/enable/disable flow is validated for production auth policy
- [ ] Public-route rate limits are tuned for expected traffic
- [ ] Audit log endpoint (`GET /api/audit`) is reachable for authenticated users
- [ ] CORS allowed origins are restricted to production front-end domains

## 3. Reliability and observability

- [ ] Provider circuit breaker settings are defined (`app.circuit-breaker.provider.*`)
- [ ] Metrics endpoints are monitored (`/api/metrics/summary`, `/api/metrics/circuit-breakers`)
- [ ] API latency and error-rate alerts are configured
- [ ] Quote data freshness checks are active

## 4. Data and migrations

- [ ] Database backups configured and restore tested
- [ ] Schema migration path validated on staging
- [ ] Retention policy for audit and operational logs documented

## 5. Application deployment

- [ ] Docker images are pinned by immutable digest/tag
- [ ] Health checks and startup probes are configured
- [ ] Rollback process tested in staging
- [ ] Release notes prepared and reviewed

## 6. Validation before go-live

- [ ] Backend integration suite passes with JDK 25
- [ ] Frontend unit tests and E2E smoke tests pass
- [ ] Legal pages are reachable (`/terms`, `/privacy`, `/risk-disclosure`)
- [ ] Pricing, auth, watchlists, alerts, and analytics smoke checks pass
