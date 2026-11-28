# Startup/library verification

The unit tests exercise process-once initialization, failure retry, exact stored-like migration/blacklist parity, folder count parity and list reconciliation. Android instrumentation tests exercise the actual Room ID projection and its live invalidation, and verify that position ticks do not recompose the surrounding player scope.

`Benchmark.kt` is a synthetic component benchmark of folder counts, using the actual production helper, 20,000 playlist-folder memberships and 1,000 folders. Both algorithms produce identical counts; warmup and nine alternating batches precede the median report. It does not measure app startup, Android RAM or navigation latency.

`query_projection.py` extracts both SQL statements from the actual DAO, checks membership parity including empty/whitespace paths, negative IDs, edits/deletions and reset, and compares full-row hydration with ID-only hydration on synthetic data. It reports Python/SQLite component timings and retained Python row allocations, not Android RAM.

The workflow builds Android/Wear debug variants, runs focused JVM suites and runs isolated Room/Compose instrumentation tests in an Android emulator. It does not publish releases. Device startup/navigation/RAM profiling remains outstanding.
