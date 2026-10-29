# Migration 007 release plan

1. Record the target project/branch, current deployment, and preflight totals. Obtain review/approval for production separately from test execution.
2. Take an available provider snapshot/restore point before production application. Verify a usable recovery procedure; do not assume retention provides unlimited backups.
3. Identify organizations with more than one property. They become `legacy_review` and stop new financial writes. Never automatically split historical journals, debt or memberships. Prepare an explicit allocation plan if any are found.
4. Apply db/007_building_ledger.sql as one transaction with a direct connection or the Neon transactional connector. This migration adds columns/tables/guards. It does not delete records or rewrite posted journal values.
5. Compare posted journal count and sum(debit)/sum(credit) before/after, and check approved expenses with existing cash journals now have paid_amount=amount and legacy_cash_posted=true.
6. Deploy the matching API and clients only after the database upgrade succeeds. Older expense approval code must not keep running: it posts cash while the new API posts payables. Use a short maintenance window for the matching DB/API rollout, or a deployment gate; do not accept mixed-version finance traffic.
7. Verify health, login, selected building/currency, zero-balance new account, other-manager approval without cash movement, partial payment/replay, cross-building transfer rejection, closed-period rejection and archive/reactivation.

## Rollback

Do not merely roll back to the old expense approval API after new accrual approvals exist. Stop financial writes and assess the restore point or a reviewed forward repair. Reconcile all journals and payments created since deployment. Posted history is never silently deleted. Column/table rollback is not the normal recovery plan.

## Executed evidence

Baseline: 68 local tests. The migration has been applied to Neon validation branch br-delicate-voice-b5azrvuy, never to production in this phase. A real PostgreSQL smoke verified one-property boundary and archived write rejection, with all synthetic records rolled back (persisted count 0). Local migration regression preserves historical cash journals and marks old expenses paid without creating another entry. Final test/build evidence is in PHASE_01_REPORT.md.
