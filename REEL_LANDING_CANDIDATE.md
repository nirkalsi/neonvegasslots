# Disabled reel landing candidate, 28 October 2026

Normal constructors leave smoothLanding=false. SpecialFeature.ENABLED remains false.

The visual-only test constructor can demonstrate a cubic Hermite phase in symbol-pitch units, whole three-row-cycle alignment, fading blur and a continuous position at settle. Replanning a shortened quick-stop or extended anticipation deadline preserves position and speed. The target cycle is chosen to satisfy the monotone Hermite bound, rather than adding reverse motion. Original three visible grid rows repeat; no off-grid symbol is invented and no deliberate near-miss distribution is added.

Not accepted for production: this candidate freezes its visible grid 180ms earlier than the current draw-driven sampler, so it can change which independent symbol draw becomes the result. Unchanged marginal weights do not prove identical outcomes or random-stream use. Enabling requires a separately reviewed result-sampling design and explicit confirmation that it stays within the user's outcome constraints. No result, payout, rate, paid entitlement or enabled path is changed here.

26 focused local tests per game pass (phase/replanning/view integration, grid freeze, default-off, payout, pending save/runtime, purchase receipt handling, existing reel motion). Android instrumentation sources compile. Synthetic 16ms-step clips and final-approach crops were generated and inspected. These are not device captures, frame-time measurements, gameplay listening acceptance or physical quick-stop tests. Real-device motion/performance and process/lifecycle acceptance remain open. The final settling bounce intentionally has position continuity, not a proven velocity-continuous join.
