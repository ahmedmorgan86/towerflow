# Architecture and financial boundaries

The existing Node ESM HTTP API, `pg`, PostgreSQL RLS, vanilla browser modules and Kotlin Android app are retained. This work does not introduce another accounting engine or framework.

## One building, one existing ledger boundary

`org_id` remains the canonical ledger/security boundary. An isolated organization can contain at most one property. A user manages multiple buildings by holding memberships in multiple independent organizations, selected through the existing workspace selector. Each building has its own currency, chart, periods, members and treasury. An empty registration workspace remains valid until it receives its first property. The building wizard creates a new ledger/property/chart/units atomically with no opening money.

Migration 007 classifies an existing organization with multiple properties as `legacy_review`. It preserves all history and prevents further financial posting until a separately reviewed allocation migration is prepared. It does not guess which tower owns an old cash journal or move historic receivables. Existing single-property ledgers are compatible. These boundaries are enforced by database triggers and composite foreign keys, not just hidden controls.

Country and currency are chosen when creating a building. Currency cannot change after a posted journal. There is no currency conversion and no aggregate balance across different building ledgers. Current monetary validation uses two decimal places; a complete global minor-unit policy remains work to do.

## Accounting contracts

An expense request has no journal. Independent approval posts Dr operating expense / Cr approved expense payable (2200). Cash does not move. A separate payment posts Dr payable / Cr the chosen treasury account. Partial payments update `paid_amount` atomically; overpayment is rejected. Existing cash-posted approvals are marked fully paid by migration without reposting any journal.

An internal treasury transfer posts Dr destination / Cr source within the same `org_id`. It creates no revenue/expense. Source and destination must be treasury assets in the same building. Insufficient funds are rejected. Payment/transfer reference is unique per building; exact retries return the original record and changed retries conflict. An organization row lock serializes these workflows; database transactions roll back every document/line if any validation or closed-period guard fails.

Posted journal lines and headers remain immutable. Account code, kind, treasury classification and organization cannot change once used in posted history. Archive retains records and makes operational tables read only. Reactivation and archive decisions are audited.

## API and clients

The browser and Android call the same building/treasury/payment endpoints. There is no client-side financial posting implementation. All endpoints authenticate and authorize on the server. Normal list reads use the non-bypass RLS role; privileged workflow transactions explicitly check capabilities and rely on composite keys and database guards.

## Known remaining architecture work

Owner/tenant responsibility history, per-action timed approvals, allocation/waivers/penalties/credits, payment proofs, vendor contracts and budgets, controlled support access, restore/move workflows, and genuinely independent Free/Commercial deployments remain scheduled in MASTER_AUDIT.md. A successful foundational slice does not establish compliance with all 106 sections. No production migration runs automatically from the API or browser.
