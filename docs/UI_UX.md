# Interface rules

Use bundled Cairo, existing AD navy/teal colors, Arabic RTL and English LTR. Preserve keyboard labels, readable contrast and text status labels. Default to the daily task and progressively reveal advanced fields.

The building wizard asks for building name, country and currency; optional floor/unit layout stays collapsed. This is currently a compact layout editor, not a finished visual floor planner. Invalid/duplicate unit identifiers cause an atomic rejection.

Financial screens must distinguish available cash, approved expense, paid expense and approved unpaid expense. All current summary figures are explicitly cumulative for the selected building; period comparison is not implied. An internal transfer explains its source/destination impact and cannot target another building.

Expense rows show approved and paid values and offer a separate payment action. The requester's own approval action is hidden in web and always forbidden by API. A failed request retains its reference for a safe retry. A changed payload with the same reference conflicts, including on Android.

Browser checks are evidence of a working UI path, not non-specialist usability testing. Human timing/error-rate studies and full mobile accessibility acceptance remain pending. No competitor artwork or designs are copied.
