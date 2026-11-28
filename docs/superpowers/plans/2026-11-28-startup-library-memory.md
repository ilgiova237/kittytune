# Startup and library performance implementation plan

**Goal:** Faster opening/navigation and less transient memory, preserving data, appearance and playback.
**Architecture:** Process-once observer initialization, prewarmed likes at Activity entry with completion before UI consumers, narrow download-ID query, indexed hierarchy counts and cached display calculations. Existing full entity queries remain available. No schema migration or data truncation.
**Spec:** User authorization in this chat on 2026-11-28, all five investigated paths, special care on narrow projections.
**Tech:** Kotlin, Compose snapshots, coroutines, Room, JUnit, Android instrumentation.

## Constraints and review focus
- Complete likes initialization before displaying or modifying likes. Recreated activities and services reuse initialized repositories.
- Failed initialization must remain retryable; concurrent callers wait for completion.
- Preserve download predicate exactly (`localAudioPath != ''`), including whitespace, negative IDs, updates/deletes and metadata-only rows.
- Preserve duplicate playlist counts, null/root folder behavior, ordering, pinning, filters, accounts and VK behavior.
- Keep navigation, layouts, animation timings and audio behavior. No Android whole-app percentage claims without device measurements.

## Tasks
1. Test `ProcessInitializer` concurrency/completion/failure retry. Use it in DownloadManager and LikeRepository. Keep per-entry download maintenance behind CoalescingTask, with tests for overlap/later entry/failure retry. Prewarm likes at Activity entry (avoid loading a full library for widget-only launches) and await from Activity on IO before completing existing setup. Guard destruction before asynchronous setup; process latest intent after readiness.
2. Add download ID-only Room query; switch only the ID-set consumer. Verify Room query parity and live updates with isolated in-memory database instrumentation test, plus SQL benchmark/parity locally. Keep full-row APIs untouched.
3. Test stored-like preparation equivalence and rewrite decision. Avoid rewriting unmodified storage; preserve fallback, migration and blacklist behavior.
4. Test child-count index equivalence and list patching. Replace nested folder scans with counts, patch snapshot lists atomically, memoize SoundCloud display calculations with explicit metadata and liked-ID invalidation. Preserve VK getter behavior.
5. Confine debug position reads to a separate composable; retain existing dock isolation. Add Compose regression verifying position updates do not recompose parent. No speculative UI rewrites.
6. Run focused local tests/benchmarks, inspect diff and compile syntax. Commit/publish through official GitHub connection; launch Android/Wear build, unit suites and isolated Room/Compose emulator checks. Stop monitoring once started.

## Review and verification checkpoint
- Independent static review: identified fresh-intent handling during deferred Activity setup and lost per-entry maintenance. Fixed with persisted FIFO intent handling and a coalesced maintenance task. Focused re-review found no remaining Critical/Important issues.
- Activity launch retains its existing window until data/content are ready; no prewarming in widget-only processes. Cancellation/destruction guards preserve incomplete startup.
- Ten focused local JVM tests passed using real Track models (only an unused Spotify display helper is shimmed for desktop compilation). Android uses real dependencies.
- Exact SQL membership parity passed locally; Room live-invalidation and Compose parent-recomposition regressions are scheduled on emulator in CI.
- Synthetic component benchmarks show reduced hierarchy work and full-row hydration. These establish algorithm/query scope, not Android startup/navigation latency or Android RAM. Device profiling remains outstanding.
