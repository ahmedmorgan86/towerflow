# TowerFlow 0.4.0 · 25 October 2026

Web: https://towerflow.vercel.app
Private source: https://github.com/ahmedmorgan86/towerflow

## Verified locally

- 68 tests passed: actual HTTP + isolated PostgreSQL, RLS, ledger, identity collision/replay, owner verification, scoped broadcast idempotency, push outbox leases and invalidation, report isolation.
- Web build passed. Browser acceptance covered profile persistence, occupancy, detailed reports and a broadcast to synthetic localhost residents only.
- Android assembleRelease and lint passed. APK signature v2 verified. SHA256: 844a41d518fee8dda40aba408bb07391bea3da3118f034a32bbdd07b76a8f1a6.
- Google OAuth publishing status visibly In production.
- Database migrations through 006 applied to isolated test and production Neon branches.

## Delivery limits

FCM provider configuration and production release status must be checked separately from local tests. No physical device or emulator acceptance run has been completed, and closed-app delivery is unverified. Android supports a native subset; detailed export and broadcast management are currently in the web app. Google Play publication is separate from Vercel and this signed APK.

Owner permissions require verified Google identity matching the configured owner email; a password signup with that email alone does not grant platform ownership. Never publish signing credentials, service account private keys or real env files. No AI API keys or AI services are used.
