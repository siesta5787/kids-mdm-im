/*
 * Kids MDM IM: drop incoming content that isn't from approved people.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import org.kidsmdm.im.journal.KidsJournal
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.groups.GroupId
import org.thoughtcrime.securesms.messages.SignalServiceProtoUtil.isGroupV2Update
import org.thoughtcrime.securesms.recipients.Recipient
import org.whispersystems.signalservice.internal.push.CallMessage
import org.whispersystems.signalservice.internal.push.Content

/**
 * When "Only approved contacts" is on ([KidsApprovedContacts]), content is dropped before Signal
 * stores it unless the sender is approved and, in a group, every member is: messages, media,
 * calls, stories, typing indicators. No message request, notification or delivery receipt is
 * created. Dropped messages and calls are journaled for the parent, in a thread of their own per
 * sender (see [KidsJournal.onBlockedContactMessage]).
 *
 * Group changes always apply in groups the kid is in, so the member list stays current. An approved
 * person can add the kid to a new group; it stays closed unless everyone in it is approved.
 */
object KidsContactFilter {
  private val TAG = Log.tag(KidsContactFilter::class.java)

  /** Hook: MessageContentProcessor.shouldIgnore. True to drop this content. */
  @JvmStatic
  fun shouldIgnore(content: Content, sender: Recipient, thread: Recipient): Boolean {
    if (!KidsApprovedContacts.enabled) return false
    if (sender.isSelf || sender.isReleaseNotes) return false

    val dataMessage = content.dataMessage ?: content.editMessage?.dataMessage
    if (dataMessage?.isGroupV2Update == true && thread.isGroup && thread.isActiveGroup) return false

    val typingGroup = content.typingMessage?.groupId?.let { runCatching { Recipient.externalPossiblyMigratedGroup(GroupId.push(it)) }.getOrNull() }
    val group = typingGroup ?: thread.takeIf { it.isGroup }

    val senderApproved = KidsApprovedContacts.isPersonApproved(sender)
    // A group the kid isn't in yet has no known members; its messages are checked once it does.
    val groupAllowed = group == null || !group.isActiveGroup || KidsApprovedContacts.isConversationAllowed(group)
    if (senderApproved && groupAllowed) return false

    Log.i(TAG, "Dropped content (sender approved: $senderApproved, group allowed: $groupAllowed).")
    journal(content, sender, group?.takeIf { !groupAllowed })
    return true
  }

  private fun journal(content: Content, sender: Recipient, closedGroup: Recipient?) {
    val offer = content.callMessage?.offer
    if (offer != null) {
      KidsJournal.onBlockedContactCall(sender.id, System.currentTimeMillis(), offer.type == CallMessage.Offer.Type.OFFER_VIDEO_CALL)
      return
    }

    val message = content.dataMessage ?: return
    val attachments = message.attachments.size + (if (message.sticker != null) 1 else 0)
    if (message.body.isNullOrEmpty() && attachments == 0) return // reactions, receipts, profile key updates...
    KidsJournal.onBlockedContactMessage(sender.id, closedGroup?.id, message.timestamp ?: System.currentTimeMillis(), message.body, attachments)
  }
}
