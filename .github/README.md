# Kids MDM IM

A messenger for a child's Android phone, with parental controls. It is
[Signal-Android](https://github.com/signalapp/Signal-Android) with a small set of changes, and it
talks to the normal Signal service, so the people your child messages just use Signal.

It's built for one self-hosted setup, run by a parent for their own child's phone:

- [kid-phone-server](https://github.com/siesta5787/kid-phone-server): the parent's admin server.
- [kids-launcher-mdm](https://github.com/siesta5787/kids-launcher-mdm): the launcher and device
  management app on the child's phone.

**This is not an official Signal app.** It isn't made, endorsed or supported by Signal Messenger or
by Molly. Please don't take problems with it to them. If you want a private messenger for yourself,
use [Signal](https://signal.org/).

## What's different from Signal

**This app records conversations for a parent.** Messages, photos, videos, voice messages and calls
are copied into a separate encrypted journal on the phone. The launcher reads it and sends it to the
parent's server. The copy stays even if the message is deleted or disappears. Message edits,
stories, reactions and other file attachments aren't recorded. Only the launcher signed with the
same key can read the journal.

**Parental controls**, behind a parental PIN:

- Block unknown contacts: only people in the phone's contacts, people already chatted with and
  members of joined groups get through. Blocked messages and calls are still recorded.
- Block voice calls and video calls separately. (Incoming *group* call rings aren't blocked yet.)
- Choose how new messages arrive.
- The PIN also locks Signal's settings and other settings screens.

**Turned off or hidden:** location sharing, payments, donations, invites, GIF search, promotional
cards, release notes, debug log prompts and the call quality survey.

**No Google services.** Google's libraries are replaced with empty stand-ins, and there's no Google
push. New messages arrive over Signal's own connection ("Always connected"), or through
[UnifiedPush](https://unifiedpush.org/) with the launcher as distributor and a
[MollySocket](https://github.com/mollyim/mollysocket) server.

## Requirements

- Android 14 or newer, 64-bit ARM.
- A phone number for the Signal account.
- The launcher and server above for the journal and UnifiedPush.
- People calling the child need a current Signal app. Older apps based on Signal 8.19 or earlier,
  including Molly 8.19, can't connect calls with it.

## Releases

Each build is first a **beta** (a GitHub prerelease). Once tested, the same file is promoted to
**stable**. New Signal stable releases are merged, built and tested automatically.

## How it's maintained

Changes to Signal's own files are kept to a few single lines, so Signal's updates merge cleanly.
Everything else lives in `app/src/kids/`, `kidsmdm/` and one build plugin. Build instructions and
the full list of changes are in [CLAUDE.md](../CLAUDE.md).

Up to v0.06 this project was based on [Molly](https://github.com/mollyim/mollyim-android). That code
is kept on the `legacy-molly` branch. The UnifiedPush and "block unknown contacts" features are
adapted from Molly.

## Legal

Based on Signal-Android, copyright Signal Messenger, LLC. Licensed under the
[GNU AGPLv3](../LICENSE), including the changes made here. Signal is a trademark of Signal
Messenger, LLC.

This distribution includes cryptographic software. The country in which you currently reside may
have restrictions on the import, possession, use, and/or re-export to another country, of encryption
software. Before using any encryption software, please check your country's laws, regulations and
policies concerning the import, possession, or use, and re-export of encryption software, to see if
this is permitted. See <http://www.wassenaar.org/> for more information. The U.S. Government
Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as
Export Commodity Control Number (ECCN) 5D002.C.1.
