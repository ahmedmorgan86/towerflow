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

## Master phase 1 — review branch, migration 007 required

- `POST /api/buildings`: authenticated user; name, country, currency, optional address and floors `[{floor:1,units:["101","102"]}]`. Returns organization, property, units_created. Each call creates a distinct ledger; duplicate unit identifiers reject the entire transaction. No opening journal is generated.
- `PATCH /api/building/config?org=...`: management permission. Accept either `{archived:true|false}` or `{name,country,currency}`. Currency is locked after posting. Archive/reactivation is audited.
- `GET /api/treasury?org=...`: finance and account-read permission. Treasury accounts with calculated posted balances.
- `POST /api/treasury?org=...`: finance and account-create permission. Name, treasury_kind cash/bank/wallet, optional unique 11xx code (automatically allocated otherwise). Initial balance is always zero.
- `POST /api/treasury/transfers?org=...`: finance and journal-create permission. source_id, target_id, positive amount, reference and optional transferred_on. Accounts must both be in this building. Reject insufficient balance. An exact retry returns the same transfer with repeated=true; changing the payload under the same reference returns 409.
- `POST /api/expenses/:id/pay?org=...`: finance and expense-update permission. account_id, amount, reference, optional paid_on. Expense must be approved; payment may be partial and cannot exceed unpaid amount or treasury balance. Same idempotency contract as transfers. Returns payment, not another approval.
- `GET /api/financial-summary?org=...`: finance and account/expense read permissions. Cumulative available_cash, approved_expenses, paid_expenses, unpaid_expenses, pending_approvals and accounts for this ledger only. Values are not combined across buildings/currencies.
- `GET /api/expense_payments` and `GET /api/treasury_transfers`: authenticated RLS-scoped list routes with existing pagination conventions.

`POST /api/expenses/:id/decide` now posts an accrued payable when approved. It must be released together with migration 007. See MIGRATION_007.md; do not mix old and new expense approval API versions. Closed-period or archive errors roll back the entire financial transaction.
