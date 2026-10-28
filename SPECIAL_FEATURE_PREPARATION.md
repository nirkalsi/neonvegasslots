# Disabled Wild and Bonus preparation

The normal game constructor still uses `SpecialFeature.ENABLED=false`. No release or account configuration was changed. The enabled constructor is package-private and used only by local integration tests.

## Candidate design
- Wild substitutes regular symbols on middle reels only. Bonus never substitutes.
- Three Bonus-bearing reels award five free spins at the trigger stake, no retrigger.
- Free spins do not debit coins or add paid-spin XP, quest counts or trophy rolls. External coin grants remain in the ordinary balance.
- Provisional 3% Bonus/cell and 0.8574342727661131% Wild/cell on middle reels were selected by offline mean-return matching, not approved for production. See Tidepool Merge research/SPECIAL_SYMBOL_DECISION_2026-10-28.txt for expected return, rounding, variance and session caveats.

## Implemented preparation
Real cabinet rendering, themed bitmap sprites with idle/win pulse, frozen sampled grid while spinning, Scatter anticipation on remaining reels, quick stop, free-spin counter and intro banner. Enabled-only save includes the grid, pending award, trigger stake, remaining count and sequence in the same ordinary game snapshot as balance and progression; saves use synchronous commits on that path. Duplicate finish is ignored. Reopened pending spins replay the stored result, not a new draw. Invalid enabled save throws rather than silently replacing a pending result.

## Verified locally
Focused Robolectric integration checks cover default-disabled selection, full snapshot recreation and single settlement, stored-grid immutability during rendering, free-spin no debit/fixed stake/no extra paid progression, and remaining-count restoration. Prototype, isolated engine, Collect, refill, known payout, quests and popup queue regression groups pass. Instrumentation sources compile. Synthetic pixels inspected for cabinet/symbol and bonus-banner fit. These are not device captures.

## Remaining acceptance
Actual process-kill/disk-failure recovery, device recovery after commit failure, real-device frame timing, sound/haptics, background/foreground and rewarded/IAP interleaving; bonus pacing and level/popup ordering; paytable/first-use explanation; final economy/design authority. No store upload until the combined portfolio gates are met.

Enabled save failures now fail closed, block further spins and raise an error rather than continuing unpersisted or silently dropping an external coin delivery. Local fault injection proves the stop and old-snapshot reload, not physical disk failure or purchase acknowledgement recovery. Bonus intro waits before level Collect and counts as busy for popup queuing. Conditional paytable prose describes the candidate rules. Final device recovery and billing interleaving remain release blockers.
