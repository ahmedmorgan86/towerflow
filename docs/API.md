# HTTP API v0.5.0 (Master Implementation)

Same-origin `/api`. JSON request/response. Every business endpoint requires `?org=<UUID>` and an authenticated session. Organization IDs are never sufficient for authorization. Node establishes transaction-local actor identity and enforces PostgreSQL Row-Level Security (RLS) on every table. Financial operations validate roles and organizations before owner transactions.

## Endpoint Reference

| Method | Path | Behavior |
|---|---|---|
| GET | /health | Service & PostgreSQL database readiness; zero secrets |
| POST | /auth/register | email, password, name, organization; creates admin workspace + chart of accounts |
| POST | /auth/login | email/password; HttpOnly SameSite cookie |
| POST | /auth/logout | Revokes current hashed session and cancels device notifications |
| GET | /me | Actor profile, organizations, and roles |
| PATCH | /me/profile | Update name, phone, bio, avatar, and locale |
| POST | /organizations | Creates isolated workspace |
| GET | /members | Manager/admin views workspace membership |
| POST | /members | Admin adds or changes user's role, links resident party |
| GET | /{entity} | Paginated list (up to 500 rows), filtered strictly by RLS |
| POST | /{entity} | Validated, allowlisted fields; role-restricted |
| PATCH | /{entity}/{id} | Allowed fields; excludes financial posted documents |
| POST | /journals | description + lines[{account_id, debit, credit}]; atomically posts balanced entry |
| POST | /invoices/{id}/issue | Atomically posts receivable/income |
| POST | /invoices/{id}/collect | amount, reference; locks invoice, posts cash/receivable, prevents overpayment |
| POST | /expenses/{id}/decide | decision=approved/rejected; non-requester manager required |
| GET | /trial_balance | Posted-only debit/credit grouped by account |
| POST | /notifications/{id}/read | Acknowledges only the current recipient's notification |
| **POST** | **/properties/create-tower** | Creates tower with strictly zero starting balances, initializes 4 money accounts (cash, bank, wallet, maintenance fund) |
| **POST** | **/properties/{id}/units_batch** | Generates batch units (floors, units per floor, area, prefix) without duplicate numbers |
| **POST** | **/properties/{id}/transfer_funds** | Intra-tower money transfer; strictly forbids cross-tower transfers |
| **POST** | **/collections/multi** | Multi-allocation invoice collections with automatic excess credit balance recording |
| **POST** | **/credit_balances/{id}/apply** | Settles invoice using resident credit balance without generating redundant cash receipts |
| **POST** | **/payment_proofs** | Resident submits bank transfer / electronic wallet proof; duplicate reference protected |
| **POST** | **/payment_proofs/{id}/review** | Manager reviews (approve/reject); approval posts receipt and settles invoice atomically |
| **POST** | **/expense_distributions** | Distributes expense by equal, area pro-rata, or percentage shares; maintenance fund subsidies for exempt units |
| **POST** | **/fines/{id}/exempt** | Manager waives fine with mandatory justification rationale logged to immutable audit trail |
| **POST** | **/properties/{id}/transfer_management** | Initiates building management handover to new manager with mutual consent workflow |
| **POST** | **/management_transfers/{id}/respond** | Target manager accepts/rejects handover; acceptance deactivates assistant delegations automatically |
| **POST** | **/properties/{id}/archive** | Soft-archives tower; preserves all historical financial records and unit data |
| **DELETE** | **/properties/{id}** | Safe deletion guard; strictly refuses deletion if financial records exist |
| **GET** | **/properties/{id}/backup** | Full JSON backup export of tower structure, units, leases, money accounts, and contracts |
| **POST** | **/properties/import_backup** | Inter-edition migration (Free <-> Commercial); imports tower backup into isolated workspace |
| **POST** | **/reconciliations** | Cash and bank account reconciliation; tracks difference and flags discrepancies |
| **POST** | **/periods/{id}/reopen** | Reopens closed accounting period with mandatory reason and immutable audit log entry |
| **POST** | **/support_tickets** | Resident or manager creates support ticket (Free / Commercial priority) |
| **POST** | **/support_tickets/{id}/messages** | Threaded replies between customer and support team |
| **POST** | **/support_grants/{id}/approve** | Building manager approves temporary, time-limited support access to workspace |
| **POST** | **/support_grants/{id}/revoke** | Building manager revokes temporary support access immediately |

## Architecture & Data Invariants

1. **Per-Tower Isolation**: Every tower is an independent administrative and financial entity. Cross-tower transfers are prohibited at the database and application levels.
2. **Zero-Balance Invariant**: New towers always start with zero balances. Opening balances must be explicitly recorded through journals or formal opening entries.
3. **Credit Balance Settlement**: Resident excess payments are credited to their account. Subsequent invoice settlements using credit balances debit the credit account and credit accounts receivable without generating false cash receipts.
4. **Replay Protection**: Payment proofs and receipts enforce unique references per account to prevent duplicate submissions or replay attacks.
5. **Safe Deletion Guard**: Properties with journals, invoices, or receipts cannot be deleted; they must be archived to maintain historical auditing integrity.
