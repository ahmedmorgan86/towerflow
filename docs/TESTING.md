# Verification guide

Run npm test for isolated HTTP/PostgreSQL acceptance. The master phase adds tests/building-ledger.test.mjs to the existing six suites. Each file gets an isolated PGlite database; concurrency is 1 to avoid shared global adapter interference.

Run npm run build and node --check on edited browser/API modules. The build copies web assets; it is not a substitute for JavaScript checking or browser acceptance.

For local browser QA, choose a new TF_LOCAL_DATA directory and set APP_ORIGIN to the exact localhost port before starting scripts/local.mjs. Never open two database processes on the same PGlite directory. Use synthetic data and never write it to production. Start with a genuinely empty building, then test both independent currencies and permissions.

Build Android with the installed compatible JDK/SDK and Gradle: assembleDebug lint. Android uses real API handlers, but compilation/lint do not prove device execution, background FCM delivery, camera behavior or full native parity.

Test migrations on an isolated provider branch before production. Verify legacy cash postings are unchanged, payment and transfer retries do not add journals, changed references conflict, closed periods roll back every side effect, and archive writes are rejected. Real provider concurrency/load and human usability studies remain pending.
