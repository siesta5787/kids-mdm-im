# Kids MDM IM — project notes

## What this is

A downstream fork of [Molly](https://github.com/mollyim/mollyim-android) (a hardened
Signal fork), hardened further for use as a monitored messenger on a child's phone.
Pairs with two companion repos (separate Claude sessions, not this one):
- [kid-phone-server](https://github.com/siesta5787/kid-phone-server) — self-hosted admin server
- [kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm) — device-owner launcher

**Repo**: https://github.com/siesta5787/kids-mdm-im (public, forked from mollyim/mollyim-android)
**App name**: Kids MDM IM · **Package ID**: `com.kidsmdm.im`
**Branches**: `main` (our work) · `molly-upstream` (tracks mollyim-android's `main`, synced
daily via `sync.yml` — note the naming: Molly's own repo already has an `upstream-main`
branch tracking Signal, which came along with the fork, so ours had to be named differently)

## Local clone

The local clone used during development lives in a session-scoped temp scratchpad
directory that does **not** persist between sessions — re-clone from GitHub each time:
```bash
git -c core.longpaths=true clone --filter=blob:none https://github.com/siesta5787/kids-mdm-im.git
cd kids-mdm-im
git config core.longpaths true
git config core.autocrlf false
```
(Windows path-length limits and CRLF auto-conversion both broke a plain clone — see gotcha below.)

## Critical gotcha: Edit tool + CRLF on this Windows machine

The Edit tool silently converts whole files to CRLF line endings when saving, even for a
one-line change — Write does not. **After every Edit tool call, before staging:**
```bash
file <path>              # look for "with CRLF line terminators" — do NOT trust grep -c $'\r', it gives false negatives here
sed -i 's/\r$//' <path>  # fix if needed
```
Skipping this turns small diffs into full-file rewrites, which both hides the real change
and guarantees a merge conflict on the next upstream sync. This bit us twice before the
habit stuck.

## Design principle

Every change is a small, isolated hook/addition at an existing choke point (not a rewrite
of upstream logic), specifically to keep the diff against `molly-upstream` small and
low-conflict for future syncs. Check `git diff --stat molly-upstream main` periodically —
it should stay in the hundreds of lines, not thousands.

## Signing

CI signs with a **shared debug-grade keystore** (same one used by `kids-launcher-mdm`,
required so the two apps' signature-permission-gated IPC can work) — stored as repo secrets
`SECRET_KEYSTORE` (base64), `SECRET_KEYSTORE_ALIAS=androiddebugkey`,
`SECRET_KEYSTORE_PASSWORD=android`. A copy of the keystore file was sent to the user
directly (not retained by Claude) — GitHub secrets are write-only, so that file is the only
backup. If it's ever lost, every future release needs a new key and anyone with the app
installed must uninstall/reinstall.

## Release process

```bash
gh workflow run tagrelease.yml -R siesta5787/kids-mdm-im -f version=X.XX
```
Auto-bumps the Android `versionCode`-relevant hotfix counter, tags, builds, and
auto-publishes. Currently at **v0.05**. Typical build time: **~17-18 minutes**.

### Release pipeline history (why it looks the way it does)

1. Originally built inside Molly's `reproducible-builds` Docker container (bit-for-bit
   reproducibility guarantee) — but that container has no build/compile cache, only a
   read-only dependency cache, so every release recompiled the entire codebase cold: ~50
   minutes every time.
2. Switched to building directly with Gradle on the runner (same approach `test.yml`
   already used successfully) — real Gradle caching, no Docker overhead, and reproducibility
   isn't a guarantee this fork needs (not publicly distributed for independent verification).
   `reproducible-builds/`/`Dockerfile`/`Makefile` are left in place untouched (unused, not
   deleted) specifically to avoid a recurring modify/delete merge conflict on every future
   upstream sync.
3. That then hit a genuine OOM — confirmed with direct `free -h` monitoring during the
   build, not guessed: RAM (15GB) climbed to full, then swap (3GB) also filled completely,
   the runner thrashed for minutes, then got killed externally. Fixed with
   `--max-workers=1` (fully serial), an explicit `org.gradle.jvmargs=-Xmx4g` cap on the
   Gradle daemon (written to `gradle.properties` before the build — a system property on
   the command line does not reliably reconfigure the daemon that gets spawned), and
   skipping `lintVital` (a pre-release quality gate not needed for internal sideloaded
   builds). See `.github/workflows/release.yml` for the current state and comments.

Only `assembleRelease` (APK) is built — `bundleRelease` (AAB) was dropped since nothing
downstream consumes it.

## Feature status (all shipped as of v0.05)

| Feature | Notes |
|---|---|
| Rebrand (name/icon/package) | `app/gradle.properties` is the single rebrand knob upstream already provides |
| GIF search removed | One property: `RemoteConfig.gifSearchAvailable` hardcoded `false` |
| Self-updater removed | Zero code changes — just build the `store` flavor (now the default) |
| minSdk 34, arm64-v8a only | Broke Robolectric (pinned to sdk 28) and several sdk-30/31-specific tests — both fixed |
| Conversation/media/call journal | See contract below |
| Parental PIN gate for Settings | Dedicated local PIN (`PinHashUtil`), independent of Signal's account PIN |
| Independent voice/video call blocking | Toggle in Settings → Call blocking; enforced in `CommunicationActions`/`WebRtcActionProcessor` |

**Not yet built**: `sync-review.yml` (Claude-reviewed auto-merge for upstream syncs — the
plan calls for it but it's on hold per user request). End-to-end device verification pass
is otherwise complete — user has tested everything above on-device successfully.

## Journal ContentProvider contract (for the launcher side)

Authority `com.kidsmdm.im.journal`, gated by signature permission
`com.kidsmdm.im.ACCESS_JOURNAL` (launcher must be signed with the same shared keystore
above). Full spec (columns, query pattern, media-URI gotcha) was handed to the
`kids-launcher-mdm` Claude session directly — see that project's own notes, or
`JournalProvider.kt` / `JournalDatabase.kt` in this repo for the source of truth.

Journals messages (text), media (photos/videos/**and audio, including voice memos** —
added in v0.05), and calls (including blocked-call attempts, `CallTable.Event.MISSED_CALL_BLOCKING`).
Captured at *insert time*, not by watching for deletion — so disappearing messages and
manual/remote deletes cannot outrace it (verified: journal write completes in
single-digit-to-low-hundreds of milliseconds after insert, vs. the shortest disappearing
timer of several seconds). One honest caveat: large media on a very short disappearing
timer over a slow connection has a theoretical (unobserved) race, since media can only be
journaled after it finishes downloading.

## Known non-issues (already chased down, don't re-investigate)

- **Incoming calls not ringing**: confirmed via live logcat monitoring to be Android's
  system Do Not Disturb being on, unrelated to anything in this fork (the call-blocking
  veto is provably a no-op when disabled — traced the actual log line, `IncomingCallActionProcessor`:
  `"Silently ignoring call due to mute settings"`, which is pure upstream Molly code).
  Not used for the launcher's own enforcement, just happened to be on. Check DND first if
  this comes up again.
- **2 pre-existing test failures**: `LocalArchiverTest` (backup-archive code, never touched
  by this fork) fails in CI regardless of any change here. Not a regression, not fixed,
  not chased further — pre-existing/unrelated.

## CI notes

- `test.yml` runs `./gradlew testProdStoreDebugUnitTest` (scoped — plain `./gradlew build`
  builds+tests+lints all ~7 upstream flavor combinations, most unused by this fork, and was
  turning every push into a 1h+ run).
- Workflows had to be manually enabled once via the GitHub UI (Actions tab banner) since
  this repo is technically a GitHub "fork" and forks default to disabled automatic triggers
  (`push`/`schedule`) — manual `workflow_dispatch` runs are unaffected by this.
- Test suite: 2592 tests, 2 known failures (see above).
