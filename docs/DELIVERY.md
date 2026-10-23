# TowerFlow delivery · 23 October 2026

## Verified resources

- Web: https://towerflow.vercel.app
- Private repository: https://github.com/ahmedmorgan86/towerflow
- PostgreSQL: dedicated Neon project `lingering-meadow-82354727` (TowerFlow). Supabase creation was blocked by the free-project limit; the user requested another provider. Existing projects were not modified.
- Verified production deployment: `dpl_Ew1XHJbrbdRAuTBLSdnp26MhFFAf`, READY, commit `e394365cb280e72f014c690d01864b352e5c14aa`.
- Android build and lint: https://github.com/ahmedmorgan86/towerflow/actions/runs/39894609262
- Android debug artifact: https://github.com/ahmedmorgan86/towerflow/actions/runs/39894609262/artifacts/12476775237 (requires repository access; expires under GitHub artifact retention).

## Verification evidence

- 43 automated tests passed locally. They include actual HTTP handlers backed by PostgreSQL, organization isolation, resident access, technician restrictions, mass-assignment prevention, accounting balance/immutability, invoice/receipt flows, expenses, ownership, occupancy, notification recipient checks, ticket state transitions and auth.
- Cloud smoke test passed on actual Neon: tenant A/B visibility, invoice issue, partial/final receipts and ledger balance. Test transaction rolled back.
- Production `/api/health`: `{"service":"TowerFlow","database":"ready","ai":false}`.
- Production login probe rejected intentionally invalid credentials. No production user account was created for this probe.
- Browser: local sign-in → create property → saved row; Arabic/English and light/dark checked. Production sign-in page renders.
- Native Android `assembleDebug lint` completed successfully on GitHub Actions. APK is a debug build; no real-device or emulator acceptance run has been completed.
- Repository and archive exclude real `.env`, `.vercel`, local database, node_modules, and signing credentials. Only `.env.example` is tracked.
- Vercel Git connection returned Connected after the user explicitly authorized persistent repository access. Future main pushes are configured for automatic deployment; do not equate that configuration with a completed release check for every future commit.

## Start using it

Open the website and choose Create account & workspace. Use your own email and a password of at least 12 characters. No production credentials have been created for you. Email verification and password recovery are pending, so retain access to your chosen password securely. Add properties, units, people and leases, then invoices and collections. For expense approval, a different registered admin/manager must approve.

## Release boundaries

This is a working initial release, not the entire requested enterprise platform. Roles are fixed, not an arbitrary per-action permission builder. Reports are initially trial balance and list exports. Requests return up to 500 rows; server pagination remains. In-app notifications are generated; email/SMS/Push are not integrated. Android implements a native subset, not web feature parity. Tax, bank reconciliation, recurrence, documents/attachments, advanced maintenance, period closing/reversals, backup restore exercises and independent security review remain.

Vercel hosts the web/API. Android distribution requires a separately signed APK/AAB and Google Play publication. The APK here is for testing only. No AI services or AI API keys are used.
