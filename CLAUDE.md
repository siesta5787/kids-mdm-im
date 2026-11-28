# Kids MDM IM: project notes

A downstream fork of [Signal-Android](https://github.com/signalapp/Signal-Android), hardened as a
monitored messenger for a child's phone. **It's no longer a Molly fork.**
- Up to v0.06 it was based on Molly.
- It was rebased directly onto Signal on 2026-11-20, because Molly had stalled at Signal 8.19.2.
- At cutover on 2026-11-22, `next` became `main`. When the repo was recreated on 2026-11-27, the
  Molly-era branches and releases up to v0.10 were left behind (no `legacy-molly` branch any more).
- UnifiedPush/MollySocket is ported from Molly; see below.

The public-facing description is **`.github/README.md`**, which GitHub shows instead of Signal's
root `README.md` (left untouched, so it never conflicts). Keep it true when behaviour changes.

Pairs with [kid-phone-server](https://github.com/siesta5787/kid-phone-server) and
[kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm).

- **Package**: `com.kidsmdm.im` · **Launcher label**: "Kids MDM IM" · **In-app wording**: Signal's own (no rebrand)
- **Shipped variant**: `kidsProdRelease`, built by `./gradlew :Signal-Android:assembleKidsProdRelease`
  (Signal renames the `:app` project to `:Signal-Android`)

## The one rule: stay surgical

Signal stable releases are **merged automatically** (`kids-sync.yml`: merge, build, unit test,
fast-forward `main`). That only works while our changes rarely conflict. In order of preference:

1. **New files**: our own modules/packages, `app/src/kids/` (the `kids` flavor source set: its
   resources override Signal's, and its manifest is merged on top of Signal's).
2. **Build-time mechanisms**: `build-logic/plugins/src/main/java/kidsmdm-app.gradle.kts` holds
   *all* build customizations (flavor, applicationId, signing, minSdk, ABI, versionCode,
   generated launcher label). It changes Signal's DSL in `finalizeDsl`, after Signal's own `android {}`
   block has run.
3. **One-line hooks** in Signal files, only at real choke points, each marked `// KIDS MDM:`.

Never edit Signal's strings, `verification-metadata.xml`, or reformat Signal code. Keep the list of
touched Signal files short: `git diff --stat signal-stable main -- . ':!**/kids*' ':!.github'`.

### Signal files we touch (keep this list current)

| File | Why |
|---|---|
| `app/build.gradle.kts` | one line: `id("kidsmdm-app")` |
| `settings.gradle.kts` | one line at the end: `apply(from = "kidsmdm/settings.gradle.kts")`, which swaps every Google library for a stub in `kidsmdm/stubs/` (see its README) |
| `feature/app-settings/.../home/AppSettingsScreen.kt` | one line: `KidsHooks.appSettingsItems(this)` (adds the "Parental controls" row) |
| `lib/remote-config/.../RemoteConfig.kt` | one line in `effectiveRawValue`: forced values from `KidsForcedConfig` |
| `database/MessageTable.kt` | two lines: journal incoming/outgoing inserts (`KidsJournal`) |
| `database/AttachmentTable.kt` | one line: journal downloaded incoming media |
| `database/CallTable.kt` | two lines: journal 1:1 and joined/rung group calls |
| `jobs/JobManagerFactories.java` | one line: register `JournalWriteJob` |
| `util/CommunicationActions.java` | one line: block outgoing calls, by type and approved contacts (`KidsCallBlocking`) |
| `service/webrtc/WebRtcActionProcessor.java` | three lines: hang up blocked incoming calls |
| `service/webrtc/IdleActionProcessor.java` | one line: don't ring for a blocked incoming group call (follows the video switch) |
| `service/webrtc/SignalCallManager.java` | five lines (two added, three changed): with video blocked, no camera or screen share during a call, no answering with video, and the other side's video / screen share stays "off" |
| `components/webrtc/BroadcastVideoSink.java` | one line: with video blocked, no call video frame is drawn |
| `conversation/AttachmentKeyboardButtonAdapter.java` | one line: hide the Location and Payment buttons (`KidsPolicy`) |
| `feature/app-settings/.../home/AppSettingsScreen.kt` (again) | the Donate and Invite rows each wrapped in `if (KidsHooks.showAppSettingsRow(...))` |
| `megaphone/Megaphones.java` | one line: no megaphones at all (`KidsPolicy.MEGAPHONES_ENABLED`) |
| `components/DebugLogsPromptDialogFragment.kt` | one line: no "submit a debug log?" prompts |
| `jobs/RetrieveRemoteAnnouncementsJob.kt` | one line: no release-note/announcement fetch |
| `org/signal/core/util/invite/InviteActions.kt` (app module) | one line: every invite button shows "Invites are turned off" instead (`KidsPolicy.blockInvite`) |
| `ContactSelectionListFragment.java` | one line: no "Invite to Signal" row in contact pickers |
| `conversation/NewConversationActivity.kt`, `calls/new/NewCallActivity.kt` | one line each: no "Invite friends" menu item |
| `conversation/v2/DisabledInputView.kt` | one line: no invite button on non-Signal contacts |
| `main/MainToolbar.kt` | one line: no Donate item in the chats/calls/stories menu (`KidsPolicy.DONATIONS_ENABLED`) |
| `messages/MessageContentProcessor.kt` | one line at the top of `shouldIgnore`: drop content that isn't from approved people (`KidsContactFilter`) |
| `recipients/RecipientUtil.java` | one line in `getEligibleForSending`: unapproved people are left out of every send to a member list |
| `jobs/IndividualSendJobV2.kt`, `jobs/PushGroupSendJob.java` | one line each: refuse (mark failed) a message to a conversation that isn't approved. The legacy `IndividualSendJob` is no longer created. |
| `jobs/ReactionSendJob.java`, `jobs/TypingSendJob.kt`, `jobs/SendReadReceiptJob.kt` | one line each: send nothing to a conversation that isn't approved |
| `webrtc/VoiceCallShare.java` | one line: calls started from the phone's Contacts app are checked too |
| `keyvalue/SettingsValues.kt` | one line in `shouldStayConnectedInBackground`: a working UnifiedPush counts as push, like FCM (`KidsPush.isPushActive`) |
| `jobs/JobManagerFactories.java` (again) | one line: register `KidsPushRefreshJob` |
| `.github/workflows/*` (Signal's) | deleted: they'd retarget our PRs to release branches, run a stale bot, etc. The sync job auto-resolves Signal edits to them by keeping the deletion. |
| `.github/FUNDING.yml`, `ISSUE_TEMPLATE/`, `PULL_REQUEST_TEMPLATE.md` (Signal's) | deleted: they sent people to Signal's donation page, forums and CLA. Auto-resolved like the workflows. |

## Where our code lives

- `app/src/kids/java/org/kidsmdm/im/`: all app-side Kids MDM code, compiled only into `kids` variants.
  Signal's code reaches it only through the one-line hooks above (fully qualified, `// KIDS MDM:` comment).
  - `KidsMdm` + `KidsInitProvider`: startup, run from a manifest provider before Signal's
    `Application.onCreate`. No change to Signal's Application class.
  - `KidsSettings`: parental settings in their own SharedPreferences (`kidsmdm_settings`), not
    SignalStore. Read only through this object, so they can later be pushed from the server.
  - `pin/`: `KidsPinGate` locks settings-type screens **by package** with an activity-lifecycle
    callback. It needs no hook, and screens Signal adds in those packages are locked automatically.
    PIN prompt/setup screens. Re-locks 15 s after leaving locked screens, or when backgrounded.
  - `journal/`: `KidsJournal` (hook entry points), `JournalWriteJob`, `JournalDatabase`
    (`kids-journal.db`), `JournalProvider`. The schema, authority (`<appId>.journal`), URIs
    (`entries/<sinceId>`, `media/<file>`), 200-row pages and the `ACCESS_JOURNAL` signature
    permission are a **contract with kids-launcher-mdm's JournalSync**: change both sides together.
    Each journal has a base (its creation time in ms, in `journal_meta`): entry ids start above
    it and `thread_id` is base + Signal's thread id (DB version 2 shifted older rows). After a
    reinstall, ids stay above the launcher's saved cursor, and new threads don't merge with old
    ones on the server, which groups conversations by `thread_id`.
  - `KidsCallBlocking`, `KidsPolicy` (feature switches: location, invites, megaphones, prompts,
    hidden settings rows, attachment buttons), `KidsForcedConfig` (forced remote config: GIF search,
    payments kill switch, call-quality survey, backup megaphones), `KidsDefaults` (one-time defaults:
    Stories off, since they aren't journaled).
  - **Approved contacts** (`KidsApprovedContacts`, replaced Molly's "block unknown contacts" on
    2026-11-28): the parent adds numbers/usernames, resolved to Signal accounts. A group is allowed
    only if **every** member is approved. Incoming is dropped by `KidsContactFilter` (group updates
    still apply, so membership stays current); outgoing is refused by the hooks above. Dropped
    messages/calls are journaled in a per-sender thread `base - recipientId`, messages as MESSAGE
    entries prefixed "🚫 Blocked (...): " (refused outgoing ones "🚫 Not sent (not approved): "),
    calls as `MISSED_CALL_BLOCKING`, so kid-phone-server shows them without changes. Call links are
    never approved. Signal updates that add a new kind of send must be checked against this.
  - `push/`: message delivery, see "Push" below.
  - `res/values/kidsmdm_signal_overrides.xml`: Signal strings reworded by name (the battery
    banner said "This device does not support Play Services").
- `kidsmdm/hooks` (`:kidsmdm-hooks`): hook points for Signal's *library* modules, which can't see
  app code. `kidsmdm/settings.gradle.kts` adds the dependency to those modules, so their build files
  stay untouched.
- `kidsmdm/stubs`: FOSS stand-ins for Google libraries (see its README).

## Push (UnifiedPush via the launcher + MollySocket)

- The parent picks **Always connected** (Signal's own websocket, default) or **UnifiedPush** on first
  run (`PushChoiceActivity`, shown once MainActivity opens after registration) and later in Parental
  controls, which also shows the status. No Google Play wording anywhere.
- **Molly's code, Molly's library.** `org.unifiedpush.android:connector:3.0.10` (Apache-2.0, pulls in
  Tink + Gson; added for the kids flavor in `kidsmdm-app.gradle.kts` with Molly's exclusions; checksums
  in `gradle/verification-metadata.xml`). `UnifiedPushReceiver`, `UnifiedPushDistributor` and
  `KidsPushRefreshJob` are Molly's classes with only Signal/storage adaptations (no SignalStore
  values, no VAPID, no air-gapped mode, no Molly passphrase lock; the distributor is picked
  automatically, preferring the Kids MDM launcher). A v0.10 hand-written client lagged and
  duplicated notifications; don't reinvent this part.
- The library provides `RaiseToForegroundService`. The launcher (0.58.0+) binds to it for 5 s per
  push (UnifiedPush AND_3 MUST), which is what lets the fetch service start from the background.
  Launcher 0.58.0+ also returns the same endpoint when the app re-registers (on every start).
- `MollySocketClient` (ported from Molly): links a "MollySocket" device with Signal's own
  `registerAsSecondaryDevice` and posts `{uuid, device_id, password, endpoint, ping}` to the server
  (webserver mode only; the Pi serves it at `https://<pi>.<tailnet>.ts.net:8443`). Molly's prekey
  workaround isn't needed: Signal now skips sending to own devices without a session.
- A push carries no content: `UnifiedPushReceiver` starts Signal's `FcmFetchManager` like an FCM push.
- **Until UnifiedPush works (`Status.REGISTERED` with an endpoint), the websocket stays on.**
  `KidsPush.onPushActiveChanged` restarts the network when that flips, like Signal's own "stay
  connected" setting. State is in its own prefs file (`kidsmdm_push`), not SignalStore.

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
  (`com.kidsmdm.im`), minSdk 34 and the "Kids MDM IM" launcher label are applied to **release variants only**.
  `kidsProdDebug` is where Signal's ~5,400 unit tests run, and they hard-code Signal's identity:
  Robolectric pinned to SDK 28, `UriUtilTest` expecting `org.thoughtcrime.securesms` URIs, and
  settings tests looking for "Signal Secure Backups". Keeping debug as Signal means no Signal test is
  ever edited or skipped. Debug builds are only for compiling and tests and never go on a phone.
  Anything that depends on the real package (journal IPC with the launcher, push) has to be tested
  on a release build.
- **One arm64 APK**, no ABI splits. kid-phone-server installs the first `.apk` asset of the latest
  GitHub release and rejects non-arm64 APKs and lower version codes.
- **Label only, no rebrand**: the `generateKidsAppLabel` task sets `app_name` to "Kids MDM IM" for
  kids release variants. Every in-app string keeps Signal's own wording (a "Signal"→"Messenger"
  rewrite was dropped on 2026-11-21 over licensing concerns).
- **Calls need the other side on RingRTC 2.70+.** RingRTC 2.75.0 (Signal 8.37) dropped "legacy
  bidirectional codec support", which Molly 8.19 / our Molly-based builds (RingRTC 2.69.5) still
  use. Between the two, every call fails during setup and shows as missed on both phones
  (debug log: `Incoming offer: encode codecs: [], decode codecs: []` → `Unable to munge SDP`).
  It isn't a bug in this fork: current Signal works, and old Molly-based installs can't be fixed.
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
| `kids-test.yml` | PRs + pushes to `main` (and a `next` staging branch, if one is ever used again): `kidsmdm/scripts/check-no-google.sh` (fails if any Google artifact would ship) + `assembleKidsProdDebug` + `testKidsProdDebugUnitTest` + `processKidsProdReleaseResources` (checks the release-only label overlay merges). Skips the push of a sync merge (already tested by the sync). Caches Robolectric's Android jars, because Maven Central rate-limits the runners |
| `kids-test-apk.yml` | pushes to a `next` staging branch (none since cutover): signed release APK as a workflow **artifact** (not a release) for side-loading. versionCode 200000. |
| `kids-build-apk.yml` | reusable: builds one signed `kidsProdRelease` APK |
| `kids-tagrelease.yml` | `gh workflow run kids-tagrelease.yml -R siesta5787/kids-mdm-im [-f version=X.YY]`: tags `main` (default: next version after the highest `vX.YY`), then calls release |
| `kids-release.yml` | runs on a pushed `vX.YY` tag (from any branch: `git tag v0.07 <commit> && git push origin v0.07`) or when called; builds the tag, checks its versionCode beats every other release, publishes `KidsMDMIM-vX.YY.apk` as a **beta prerelease** |
| `kids-promote.yml` | `gh workflow run kids-promote.yml -R siesta5787/kids-mdm-im -f version=X.YY`: beta → stable |
| `kids-sync.yml` | twice daily: Signal stable (from `updates.signal.org/android/latest.json`) → uploads Signal's new LFS files → `sync/signal-vX` → test → fast-forward `main` → automatic beta release; opens an issue on conflict or failure; mirrors the release to `signal-stable` |

`workflow_dispatch` and `schedule` only work for workflows on the default branch, which is `main`
since cutover. (Before that, betas were cut by pushing a `vX.YY` tag by hand, and v0.09/v0.11 were
promoted with `gh release edit` instead of `kids-promote.yml`.)

Secrets: `SECRET_KEYSTORE` (base64 PKCS12), `SECRET_KEYSTORE_ALIAS=kidsmdm`,
`SECRET_KEYSTORE_PASSWORD` (shared Kids MDM release key, cert SHA-256
`C1:01:C6:F4:4B:7E:CC:27:AF:0C:E8:B4:2A:81:64:7F:15:A6:1A:26:74:33:B8:45:9B:8B:95:4F:D4:CB:4D:AB`), and
`SYNC_PAT`. `SYNC_PAT` is a fine-grained token with Contents + Workflows read/write; GitHub refuses
`GITHUB_TOKEN` pushes whose commits touch `.github/workflows`, which Signal's history does.

**Standalone repository, not a GitHub fork.** It was recreated on 2026-11-27 to leave the
mollyim fork network (too big for GitHub's "Leave fork network" button). Consequences:
- Pull request numbers restarted. `(#N)` in commit messages up to that date refers to the old repo.
- The repo holds its own copy of Signal's Git LFS files (screenshot-test PNGs), which `kids-sync.yml`
  keeps up to date. A hand-merge of a Signal tag needs the same two steps before pushing:
  `git lfs fetch --all signal <ref>` and `git lfs push origin <ref>`. Builds and tests don't need
  the files (`GIT_LFS_SKIP_SMUDGE=1`).

## Rebase status (see the plan in the Kids MDM memory / chat of 2026-11-20)

- [x] Phase 1: skeleton (flavor, package, signing, launcher label, CI)
- [x] Phase 2: FOSS. Swap every Google library (firebase-messaging, play-services-*, play core,
      billing, wallet, credentials compat, googleid) for do-nothing stub modules via Gradle dependency
      substitution. **No Google push at all** (decided 2026-11-20): delivery is UnifiedPush (Phase 6)
      or Signal's own websocket fallback, so no Firebase project is needed.
- [x] Phase 3: port journal, call blocking, GIF-off; app-wide parental PIN gate; location
      sharing disabled (pulled forward from Phase 4 after it crashed in testing)
- [x] Phase 4: hide payments/donations/invites/megaphones/prompts/release notes (forced remote
      config + one-line hooks). Kept on purpose: the battery-optimization prompt (the websocket
      needs it) and the functional chat-list banners. Remaining invite buttons hit the
      `InviteActions` hook; Signal's invite screen is PIN-gated.
- [x] Phase 5: block unknown contacts (Molly's rule: system contact, profile shared, or shared
      accepted group), toggle in Parental controls, off by default
- [x] Approved contacts replaced "block unknown contacts" (2026-11-28); Stories off by default
- [x] Phase 6: UnifiedPush (launcher distributor + MollySocket) with a first-run choice between
      Always connected and UnifiedPush, no Google Play wording; journal messages/calls blocked by
      "Block unknown contacts"
- [x] Phase 7: cutover (2026-11-22).
      - Branches: the Molly `main` was set aside; `next` → `main`, now the default branch.
      - Releases: v0.11 promoted to stable; the twice-daily Signal sync and the dispatch workflows now run.
      - Phones: replacing a Molly-based install means uninstalling and reinstalling, because Molly's
        on-device data isn't readable. Re-register the account, then pick the delivery method.
      - Branches now: `main` and `signal-stable` (the sync's mirror of Signal's stable release).
