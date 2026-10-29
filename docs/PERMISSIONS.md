# Permissions for the first master implementation phase

All new treasury reads require finance capability and account read permission. Financial summary also requires expense read permission. Creating a treasury account requires account-create permission. Paying an expense requires finance capability and expense-update permission. Transfers require finance capability and journal-create permission. Explicit per-entity revocations are checked even when the role normally has finance capability; administrators retain the existing administrative override.

Expense approval requires management capability and a different actor from the requester. The implementation does not yet provide configurable per-person approval amount ceilings or timed delegations. Those remain unmet master requirements, not implied by the role names.

Building archive/reactivation and configuration require management capability in that building. Creating an independent building gives its authenticated creator administration of the new ledger and does not grant their other building members access automatically. The existing verified platform owner enrollment behavior is retained.

Composite organization foreign keys prevent financial lines, payments and transfers from referencing an account in another building. Normal entity lists run under the RLS client role. New privileged workflow routes check authorization before querying or mutating data. No private device tokens, user password hashes, sessions or service credentials are included in public list routes or exports.
