# Kids MDM IM

[![Test](https://github.com/siesta5787/kids-mdm-im/workflows/Test/badge.svg)](https://github.com/siesta5787/kids-mdm-im/actions)
[![Reproducible build](https://github.com/siesta5787/kids-mdm-im/actions/workflows/reprocheck.yml/badge.svg)](https://github.com/siesta5787/kids-mdm-im/actions/workflows/reprocheck.yml)

Kids MDM IM is a hardened, parent-controlled messenger for kids, built as a
downstream fork of [Molly](https://github.com/mollyim/mollyim-android)
(itself a hardened fork of [Signal](https://github.com/signalapp/Signal-Android)).
It connects to Signal's own servers, so kids can message their Signal/Molly
contacts normally, while giving parents visibility and controls appropriate
for a child's device.

It's designed to pair with a self-hosted child-MDM stack:

- [kid-phone-server](https://github.com/siesta5787/kid-phone-server) — the
  self-hosted admin server
- [kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm) — the
  device-owner launcher that enforces restrictions and syncs data to the
  server

## About this fork

This is an independent project, not affiliated with, endorsed by, or
sponsored by Molly, Signal Messenger LLC, or the Signal Foundation. See
[LEGAL.md](LEGAL.md) for the full disclaimer.

It intentionally changes some of the privacy properties Signal/Molly
advertise: on a device running Kids MDM IM, conversations (including
disappearing messages and messages later deleted-for-everyone), media, and
call history are journaled locally so the device owner (the parent) can
review them. This is meant for a parent's own child-MDM deployment, not as
a general-purpose Signal client — if that's not what you want, use unmodified
[Molly](https://github.com/mollyim/mollyim-android) or
[Signal](https://github.com/signalapp/Signal-Android) instead.

We aim to track upstream Molly closely and keep our changes small and
isolated, so it stays easy to rebase and low-maintenance to keep current with
upstream security fixes.

## Parental-control features

| Feature | Status |
| --- | --- |
| Distinct branding/icon, no Signal/Molly trademarks | ✅ Done |
| GIF search removed (stickers & emoji unaffected) | ✅ Done |
| In-app self-updater removed | ✅ Done |
| minSdk raised to Android 14, matching the launcher | ✅ Done |
| Independent release pipeline (own version numbers, auto-published) | ✅ Done |
| Conversation/media/call journal, synced to the launcher via local IPC | 🚧 In progress |
| Settings gated behind a dedicated local parental PIN | 🚧 In progress |
| Independent voice/video call blocking | 🚧 In progress |
| Claude-reviewed auto-merge for upstream Molly syncs | 🚧 In progress |

Beyond these, it inherits all of Molly's own hardening features (database
passphrase encryption, secure RAM wiping, automatic lock, SOCKS/Tor support,
etc.) — see [Molly's README](https://github.com/mollyim/mollyim-android#features)
for the full list.

## Download

Builds are published on this repo's [Releases](https://github.com/siesta5787/kids-mdm-im/releases)
page. Release version numbers (`v0.01`, `v0.02`, ...) are our own, separate
from Molly/Signal's version string — see [tagrelease.yml](.github/workflows/tagrelease.yml).

> [!NOTE]
> Releases are currently unsigned test builds (no release signing key has
> been configured yet). A permanent signing key will be added, and this
> section updated with its fingerprint, before these builds are used on a
> real device.

## Free and Open-Source

Like Molly, this fork comes in two variants: one with proprietary Google
Play Services blobs, and one fully FOSS. Choose the FOSS variant for
GrapheneOS or other de-Googled devices — see
[Molly's README](https://github.com/mollyim/mollyim-android#free-and-open-source)
for the details, which apply unchanged here.

## Compatibility with Signal

This app and Signal/Molly can be installed on the same device. See
[Molly's README](https://github.com/mollyim/mollyim-android#compatibility-with-signal)
for how registration and linked devices work — unchanged in this fork.

## Backups

Backup format compatibility with Signal/Molly is unchanged; see
[Molly's docs](https://github.com/mollyim/mollyim-android#backups).

## Feedback

- [Submit bugs and feature requests](https://github.com/siesta5787/kids-mdm-im/issues)
  for this fork
- For the launcher/server side, use the
  [kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm/issues)
  or [kid-phone-server](https://github.com/siesta5787/kid-phone-server/issues)
  issue trackers
- For general Molly/Signal questions unrelated to the parental-control
  features, see [Molly's own community links](https://github.com/mollyim/mollyim-android#feedback)

## Reproducible Builds

This fork keeps Molly's reproducible build setup unchanged. See the guide in
the [reproducible-builds](reproducible-builds) directory.

## License

Licensed under the GNU Affero General Public License, version 3 only
([`AGPL-3.0-only`](LICENSE)).

See [LEGAL.md](LEGAL.md) for legal and copyright information.

## Acknowledgements

This fork is built entirely on the work of the [Molly](https://github.com/mollyim/mollyim-android)
project and, through it, [Signal](https://github.com/signalapp/Signal-Android).
We're deeply grateful to both projects' contributors — please consider
supporting them directly if you find this fork useful.
