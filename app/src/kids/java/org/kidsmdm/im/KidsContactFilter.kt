/*
 * Kids MDM IM: block messages and calls from unknown senders.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import org.kidsmdm.im.journal.KidsJournal
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.messages.SignalServiceProtoUtil.hasSignedGroupChange
import org.thoughtcrime.securesms.recipients.Recipient
import org.whispersystems.signalservice.internal.push.CallMessage
import org.whispersystems.signalservice.internal.push.Content

/**
 * When [KidsSettings.blockUnknownContacts] is on, everything from a sender the kid doesn't know is
 * dropped before Signal stores it: messages, media, calls, stories, typing indicators. No message
 * request or notification is created. Dropped messages and calls are journaled for the parent
 * (in a thread of their own per sender, see [KidsJournal.onUnknownContactBlocked]).
 *
 * A sender is known if any of these is true (Molly's rule):
 *  - they're in the phone's contacts,
 *  - the kid shares their profile with them (accepted their message request or messaged them first),
 *  - they're in a group the kid has accepted.
 *
 * Group changes from unknown senders still apply in groups the kid is already in (members added
 * or removed, the kid being removed), but an unknown sender can't add the kid to a new group.
 */
object KidsContactFilter {
  private val TAG = Log.tag(KidsContactFilter::class.java)

  /** Hook: MessageContentProcessor.shouldIgnore. True to drop this content. */
  @JvmStatic
  fun shouldIgnore(content: Content, sender: Recipient, thread: Recipient): Boolean {
    if (!KidsSettings.blockUnknownContacts) return false
    if (sender.isSelf || sender.isReleaseNotes) return false

    val dataMessage = content.dataMessage ?: content.editMessage?.dataMessage
    if (dataMessage?.hasSignedGroupChange == true && thread.isGroup && thread.isActiveGroup) return false

    if (isKnown(sender)) return false

    Log.i(TAG, "Dropped content from an unknown sender (blocking unknown contacts is on).")
    journal(content, sender)
    return true
  }

  private fun journal(content: Content, sender: Recipient) {
    val offer = content.callMessage?.offer
    if (offer != null) {
      KidsJournal.onUnknownContactCallBlocked(sender.id, System.currentTimeMillis(), offer.type == CallMessage.Offer.Type.OFFER_VIDEO_CALL)
      return
    }

    val message = content.dataMessage ?: return
    val attachments = message.attachments.size + (if (message.sticker != null) 1 else 0)
    if (message.body.isNullOrEmpty() && attachments == 0) return // reactions, receipts, profile key updates...
    KidsJournal.onUnknownContactMessageBlocked(sender.id, message.timestamp ?: System.currentTimeMillis(), message.body, attachments)
  }

  private fun isKnown(sender: Recipient): Boolean {
    return sender.isSystemContact || sender.isProfileSharing || isProfileSharedViaGroup(sender)
  }

  /** Same check as Signal's private RecipientUtil.isProfileSharedViaGroup. */
  private fun isProfileSharedViaGroup(sender: Recipient): Boolean {
    return SignalDatabase.groups.getPushGroupsContainingMember(sender.id).any { group ->
      Recipient.resolved(group.recipientId).isProfileSharing
    }
  }
}
