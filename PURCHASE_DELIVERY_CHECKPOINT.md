# Purchase delivery correction, 2026-10-28

Audit found a code-level risk, not an observed customer loss: the previous flow consumed coin/energy products before delivering and asynchronously saving the grant. An interruption between those steps could lose a grant. Duplicate callbacks had no durable token receipt.

New flow, for existing consumables only:
1. Dispatch grant processing on the UI thread.
2. Check a SHA-256 token receipt in the ordinary game preference file. Matching delivered receipt skips the grant; a mismatched product is rejected.
3. Add the unchanged product grant without an intermediate autosave. Synchronously commit the full game snapshot and token receipt in one preference transaction.
4. Only a successful delivery check permits the Play consume request. If consumption fails, a later Play restore can retry consumption without granting again.
5. A failed local commit restores the old in-memory snapshot, blocks subsequent autosave and withholds consumption. Delivery failure raises an error rather than silently continuing.

No product IDs, amounts, prices, ads-removed entitlements or refill limits were changed. No purchase was made. Token receipts are private local state; raw tokens are not persisted in these receipts. Local receipts are not server-side purchase validation and do not restore consumed balances across reinstall or devices.

Non-purchase local tests cover atomic persisted grant+receipt, duplicate callback and object recreation, separate tokens, invalid token, product conflict, failed commit/rollback/blocked retry, and actual game snapshot restoration. Real Play license-tester callback/consume/reconnect/process-death validation remains a combined-release acceptance gate. No claim of production reliability or customer-loss incidence is made.

13:38 release-variant verification:36 focused tests pass including purchase delivery, accessibility, Collect, refill, known payout and disabled runtime. Added synthetic-token delivery during a pending sampled spin, followed by serialized recreation: grant, original grid and stored award survive; result credits once. Not a Play callback or Android process-kill test. No production code changed.
