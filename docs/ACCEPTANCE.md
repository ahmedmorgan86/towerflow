# Acceptance and remaining phases

| Flow | Automated acceptance |
|---|---|
| Multi-tenant isolation | Actor B cannot read or create in A; records cannot move across orgs; composite FK rejects cross-org relations |
| Resident portal | Resident sees own lease/invoice/unit and can create a ticket only for their leased unit |
| Finance | Balanced journal only, immutable posted headers/lines, issue once, partial/final collection, reject excess or repeated collection |
| Expenses | Requester cannot self-approve; different manager approves once and posts balanced expense |
| Ownership / occupancy | Share total ≤100%; active lease updates occupied status |
| Audit / notifications | Client cannot forge audit; notification visible only to recipient |
| Auth/API | Sign-up/login/logout, default chart, cross-origin rejection, access denied before login, revoked sessions fail |
| Web build | Static assets build and JavaScript syntax check |
| Android | CI configured for debug compile + lint; final result must be checked from the actual run |

Phase 1: implemented foundational vertical slices in this release. Phase 2: complete user lifecycle, configurable permissions, pagination/search at server, billing periods and tax/accounting rules. Phase 3: recurring maintenance, attachments, push/email, resident workflows and property-specific access. Phase 4: comprehensive reports, bank reconciliation, export formats, backup/restore exercises, performance/concurrency tests and independent security review. Phase 5: Android feature parity, device tests, accessibility/localization pass and signed Play release.

No legal/compliance, production capacity or full ERP completeness claim is made by passing this initial suite. Two-sided authorization checks run in PostgreSQL; production provider tests and concurrent worker tests remain separately necessary.


## 0.4 verification update

68 automated tests pass. Additional cases cover Google verified-owner enrollment and spoof prevention, future-organization enrollment, owner role protection, detailed report isolation, selected-unit broadcast recipients and replay protection, device registration ownership, concurrent outbox processing and invalid-token cleanup. Android signed release compilation and lint pass; physical-device notification delivery and full native feature parity remain unverified.
