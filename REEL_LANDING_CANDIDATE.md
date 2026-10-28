# Disabled reel landing candidate, 28 October 2026

Normal constructors leave smoothLanding=false. SpecialFeature.ENABLED remains false.

The visual-only test constructor demonstrates cubic Hermite phase in symbol-pitch units, whole three-row-cycle alignment, fading blur and continuous position at settle. A shortened quick-stop or extended anticipation deadline preserves position and speed. The target cycle satisfies the monotone Hermite bound. Original three visible grid rows repeat; no off-grid symbol or deliberate near-miss distribution is added.

## Result-preserving replacement

The first disabled draft froze the visible grid180ms before the ordinary sampler stopped. That draft was rejected as a production design. A seed mismatch alone would not require a user decision, but early freeze could shorten a user's displayed-grid stop-influence window.

This replacement leaves the authoritative sampler, logical stop deadlines, quick-stop110ms staggering and anticipation decision unchanged. Only the display stop adds180ms after logical stop, allowing the actual frozen grid to decelerate into place. Settlement waits until that visual end; debit, payout formula, progression and trophy rolls are unchanged. The first four logical stops, not delayed display stops, still decide anticipation. No new random-selection model is needed.

27 focused local tests per game cover matched30ms/53ms traces in both paths, normal/shortened stop/forced actual anticipation, identical RNG call counts at every frame, identical full grids and logical deadlines, payout equality, identical settled serialized economy/progression/trophy state and duplicate-finish rejection. Other tests cover phase/replanning/view integration, grid freeze after logical stop, default-off, payout, pending save/runtime and purchase receipt handling. Instrumentation compiles.

Synthetic16ms-step clips and final-approach crops were generated and inspected. They are not device captures, frame-time measurements, gameplay listening acceptance or physical quick-stop tests. Real-device motion/performance and lifecycle acceptance remain open. The settle bounce has position continuity, not a proven velocity-continuous join. Delaying presentation settlement180ms also delays the next-spin opportunity by that amount; this is a visible timing tradeoff, not an odds or reward change. Keep disabled until the final visual/control review.
