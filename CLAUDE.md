# Kids MDM IM: project notes

A downstream fork of [Signal-Android](https://github.com/signalapp/Signal-Android), hardened as a
monitored messenger for a child's phone. It used to be based on Molly. It was rebased directly onto
Signal on 2026-11-20 because Molly had stalled at Signal 8.19.2. The Molly-era code is kept on the
`legacy-molly` branch after cutover; until then it's still on `main`, and this work lives on `next`.

Pairs with [kid-phone-server](https://github.com/siesta5787/kid-phone-server) and
[kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm).

- **Package**: `com.kidsmdm.im` · **Launcher label**: "Kids MDM IM" · **In-app brand**: "Messenger"
- **Shipped variant**: `kidsProdRelease`, built by `./gradlew :Signal-Android:assembleKidsProdRelease`
  (Signal renames the `:app` project to `:Signal-Android`)

## The one rule: stay surgical

Signal stable releases are **merged automatically** (`kids-sync.yml`: merge, build, unit test,
fast-forward `main`). That only works while our changes rarely conflict. In order of preference:

1. **New files**: our own modules/packages, `app/src/kids/` (the `kids` flavor source set: its
   resources override Signal's, and its manifest is merged on top of Signal's).
2. **Build-time mechanisms**: `build-logic/plugins/src/main/java/kidsmdm-app.gradle.kts` holds
   *all* build customizations (flavor, applicationId, signing, minSdk, ABI, versionCode,
   generated branding). It changes Signal's DSL in `finalizeDsl`, after Signal's own `android {}`
   block has run.
3. **One-line hooks** in Signal files, only at real choke points, each marked `// KIDS MDM:`.

Never edit Signal's strings, `verification-metadata.xml`, or reformat Signal code. Keep the list of
touched Signal files short: `git diff --stat signal-stable next -- . ':!**/kids*' ':!.github'`.

### Signal files we touch (keep this list current)

| File | Why |
|---|---|
| `app/build.gradle.kts` | one line: `id("kidsmdm-app")` |
| `settings.gradle.kts` | one line at the end: `apply(from = "kidsmdm/settings.gradle.kts")`, which swaps every Google library for a stub in `kidsmdm/stubs/` (see its README) |
| `.github/workflows/*` (Signal's) | deleted: they'd retarget our PRs to release branches, run a stale bot, etc. The sync job auto-resolves Signal edits to them by keeping the deletion. |

## Build details

- **JDK 21** is required, and Gradle auto-download is disabled. Locally:
  `JAVA_HOME=~/.local/share/jdks/jdk-21* ANDROID_HOME=~/Android/Sdk ./gradlew :Signal-Android:assembleKidsProdDebug`.
  Never install a locally built APK on a kid's phone.
- **versionName stays Signal's exact `X.Y.Z`.** Signal puts it in the user agent its servers check, and
  `RemoteDeprecation`/`SemanticVersion` parse it with `^\d+\.\d+\.\d+$`; anything else breaks
  them. Our release number goes in the **versionCode** instead: `200000 + major*100 + minor` from
  the `vX.YY` tag (`-PkidsVersion`), always above the last Molly-based build (171915). It's also
  exposed as `BuildConfig.KIDS_VERSION`.
- **Debug is the test variant and keeps Signal's identity; release is Kids MDM IM.** The package
  (`com.kidsmdm.im`), minSdk 34 and the "Messenger" branding are applied to **release variants only**.
  `kidsProdDebug` is where Signal's ~5,400 unit tests run, and they hard-code Signal's identity:
  Robolectric pinned to SDK 28, `UriUtilTest` expecting `org.thoughtcrime.securesms` URIs, and
  settings tests looking for "Signal Secure Backups". Keeping debug as Signal means no Signal test is
  ever edited or skipped. Debug builds are only for compiling and tests and never go on a phone.
  Anything that depends on the real package (journal IPC with the launcher, push) has to be tested
  on a release build.
- **One arm64 APK**, no ABI splits. kid-phone-server installs the first `.apk` asset of the latest
  GitHub release and rejects non-arm64 APKs and lower version codes.
- **Branding**: the `generateKidsBrandingResources` task rewrites "Signal" to "Messenger" in every
  string of app/core/feature/lib modules and every locale, and sets `app_name` to "Kids MDM IM". It
  writes a generated res dir for kids release variants (see above). It deliberately keeps "Signal" in
  invite/donation/legal strings and in "Signal Login/Messenger/Technology Foundation/Protocol".
  Non-Latin transliterations of "Signal" in some locales aren't caught.
- **Dependency verification is strict** (`gradle/verification-metadata.xml`). Adding an external
  dependency means writing checksums. Prefer code that needs none.

## Release channels (beta → stable)

Every build is published as a GitHub **prerelease**, which is the **beta** channel. "Promote"
(`kids-promote.yml`) clears the prerelease flag on that same release, so stable phones get
byte-for-byte the APK that was tested. Nothing is rebuilt. kid-phone-server needs no code for
this; its app catalog has two entries for this repo:

- **Kids MDM IM (beta)**: "Include prereleases" on, hold 0 days. Selected only on the parent's test phone.
- **Kids MDM IM**: prereleases off. Selected on the kids' phones.

The server installs the first `.apk` of the newest release the entry allows, and holds a release
for review if its package, signing key or permissions change, or its versionCode goes down.

## CI

| Workflow | What |
|---|---|
| `kids-test.yml` | PRs + pushes to `main`/`next`: `kidsmdm/scripts/check-no-google.sh` (fails if any Google artifact would ship) + `assembleKidsProdDebug` + `testKidsProdDebugUnitTest` + `processKidsProdReleaseResources` (checks the release-only branding overlay merges) |
| `kids-test-apk.yml` | pushes to `next`: signed release APK as a workflow **artifact** (not a release) for side-loading. versionCode 200000. |
| `kids-build-apk.yml` | reusable: builds one signed `kidsProdRelease` APK |
| `kids-tagrelease.yml` | `gh workflow run kids-tagrelease.yml -R siesta5787/kids-mdm-im [-f version=X.YY]`: tags `main` (default: next version after the highest `vX.YY`), then calls release |
| `kids-release.yml` | builds the tag, checks its versionCode beats every other release, publishes `KidsMDMIM-vX.YY.apk` as a **beta prerelease** |
| `kids-promote.yml` | `gh workflow run kids-promote.yml -R siesta5787/kids-mdm-im -f version=X.YY`: beta → stable |
| `kids-sync.yml` | twice daily: Signal stable (from `updates.signal.org/android/latest.json`) → `sync/signal-vX` → test → fast-forward `main` → automatic beta release; opens an issue on conflict or failure; mirrors the release to `signal-stable` |

`workflow_dispatch` and `schedule` only work for workflows on the default branch. Until cutover
makes this branch `main`, tag, release, promote and sync can't run. Only `kids-test` and
`kids-test-apk` can.

Secrets: `SECRET_KEYSTORE` (base64 PKCS12), `SECRET_KEYSTORE_ALIAS=kidsmdm`,
`SECRET_KEYSTORE_PASSWORD` (shared Kids MDM release key, cert SHA-256
`C1:01:C6:F4:4B:7E:CC:27:AF:0C:E8:B4:2A:81:64:7F:15:A6:1A:26:74:33:B8:45:9B:8B:95:4F:D4:CB:4D:AB`), and
`SYNC_PAT`. `SYNC_PAT` is a fine-grained token with Contents + Workflows read/write; GitHub refuses
`GITHUB_TOKEN` pushes whose commits touch `.github/workflows`, which Signal's history does.

The GitHub repo is a *fork of mollyim/mollyim-android*, so always pass `-R siesta5787/kids-mdm-im`
to `gh pr create` or it targets Molly. Signal's screenshot-test PNGs are Git LFS and are never
fetched (`GIT_LFS_SKIP_SMUDGE=1`); they aren't needed to build.

## Rebase status (see the plan in the Kids MDM memory / chat of 2026-11-20)

- [x] Phase 1: skeleton (flavor, package, signing, branding, CI)
- [x] Phase 2: FOSS. Swap every Google library (firebase-messaging, play-services-*, play core,
      billing, wallet, credentials compat, googleid) for do-nothing stub modules via Gradle dependency
      substitution. **No Google push at all** (decided 2026-11-20): delivery is UnifiedPush (Phase 6)
      or Signal's own websocket fallback, so no Firebase project is needed.
- [ ] Phase 3: port journal, call blocking, GIF-off; app-wide parental PIN gate
- [ ] Phase 4: hide payments/donations/megaphones/prompts (forced remote config + few hooks);
      disable location sharing
- [ ] Phase 5: block unknown contacts
- [ ] Phase 6: UnifiedPush (Molly's `im.molly.unifiedpush`, launcher distributor + MollySocket)
- [ ] Phase 7: cutover (uninstall/reinstall phones: Molly's on-device data isn't readable)
