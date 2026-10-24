# HTTP API v0.1

Same-origin `/api`. JSON request/response. Every business endpoint requires `?org=<UUID>` and a session. Organization IDs are never sufficient for authorization. Node establishes transaction-local actor identity and uses the RLS role for general reads/writes. Financial services validate role and organization before owner-connection transactions.

| Method | Path | Behavior |
|---|---|---|
| GET | /health | Database readiness; no secrets |
| POST | /auth/register | email, password (12–128), name, organization; creates admin workspace + default chart |
| POST | /auth/login | email/password; HttpOnly SameSite cookie |
| POST | /auth/logout | Revokes current hashed session |
| GET | /me | Actor and their organizations/roles |
| POST | /organizations | Creates another workspace |
| GET | /members | Manager/admin sees workspace membership |
| POST | /members | Admin adds or changes registered user's role, optional party_id to link resident |
| GET | /{entity} | Up to 500 visible rows, filtered by RLS |
| POST | /{entity} | Validated, allowlisted fields; role restricted |
| PATCH | /{entity}/{id} | Allowed fields, role restricted; excludes financial documents |
| POST | /journals | description + lines[{account_id,debit,credit}]; atomically posts balanced entry |
| POST | /invoices/{id}/issue | Atomically posts receivable/income |
| POST | /invoices/{id}/collect | amount, reference; locks invoice, posts cash/receivable, prevents overpayment |
| POST | /expenses/{id}/decide | decision=approved/rejected; different manager required |
| GET | /trial_balance | Posted-only debit/credit grouped by account |
| POST | /notifications/{id}/read | Acknowledges only the current recipient's notification |

Entities: properties, units, parties, leases, ownerships, accounts, technicians, assets, tickets, announcements, invoices, expenses. Read-only: receipts, journals, journal_lines, notifications, audit_logs, trial_balance. No hard-delete endpoint. The initial release has a 500-row read limit; cursor pagination remains to implement before large portfolios.

Mobile uses `X-TowerFlow-Client: android` on login to receive a random opaque session token, then `Authorization: Bearer <token>`. Tokens and passwords never belong in source control. Tokens are SHA-256 hashed in private.sessions. Passwords use randomly salted scrypt; rate limits persist in PostgreSQL by normalized email. Add IP-based gateway rate limiting and verified email/recovery before broad public onboarding.

Errors use `{error,code}` with 400 validation, 401 auth, 403 authorization, 404 missing route/record, 409 duplicate/invalid state, 429 throttling, 503 unconfigured dependency. Unexpected server errors hide internals.

Database schema: organizations → memberships (private) → role authorization; properties → units → leases/ownerships → parties; invoices → receipts; journals → journal_lines → accounts; assets/technicians → tickets; notifications and append-only audit logs. All business relationships include org_id in foreign keys. Secrets and auth tables are in a private schema. Authentication is server mediated; the database is never exposed directly to the browser.


## 0.4 additions

Member profile and avatar, Google OAuth/Android handoff, verified platform owner, detailed reports, resident broadcast preview/send/history, push device registration/removal and delivery status/process endpoints are implemented in api/index.mjs. Broadcasts require management permission, a building in the active organization, and linked residents with current active leases. A selected-unit request is validated against that building. Delivery status records FCM acceptance, not proof of device display. Report CSV exports up to 20,000 rows; print/PDF prints the displayed page.
