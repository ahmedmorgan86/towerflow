# TowerFlow 0.4.0 · 25 October 2026

Web: https://towerflow.vercel.app
Private source: https://github.com/ahmedmorgan86/towerflow

## Verified locally

- 68 tests passed: actual HTTP + isolated PostgreSQL, RLS, ledger, identity collision/replay, owner verification, scoped broadcast idempotency, push outbox leases and invalidation, report isolation.
- Web build passed. Browser acceptance covered profile persistence, occupancy, detailed reports and a broadcast to synthetic localhost residents only.
- Android assembleRelease and lint passed. APK signature v2 verified. SHA256: 844a41d518fee8dda40aba408bb07391bea3da3118f034a32bbdd07b76a8f1a6.
- Google OAuth publishing status visibly In production.
- Production feature commit `33ac5b8` deployed READY as `dpl_3YuWosvnMhyLf9Ceh3J9mqngePvi`, aliased to towerflow.vercel.app. Health returned database ready and ai false.
- Actual Google sign-in with the authorized owner email completed on production; dashboard displayed the authenticated member as app owner.
- FCM-only service account authentication verified; private credential stored as a production Vercel Secret. A validation-only FCM request reached the API and rejected the intentionally invalid synthetic device token (400 INVALID_ARGUMENT), with no real notification sent. The uploaded certificate expires 25 October 2027 and must be replaced before then. The unused generated key was revoked with user approval.
- Database migrations through 006 applied to isolated test and production Neon branches.

## Delivery limits

No physical device or emulator acceptance run has been completed, and closed-app delivery is unverified. Android supports a native subset; detailed export and broadcast management are currently in the web app. Google Play publication is separate from Vercel and this signed APK. A debug installation may need to be uninstalled before installing this release because the signing certificate differs.

Owner permissions require verified Google identity matching the configured owner email; a password signup with that email alone does not grant platform ownership. Never publish signing credentials, service account private keys or real env files. No AI API keys or AI services are used.
