/*
 * Kids MDM IM: entry points for the parental journal. Every journal hook in Signal's code is a
 * single line calling one of these; the filtering and the work live here.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.journal

import org.signal.core.models.database.AttachmentId
import org.signal.core.models.recipients.RecipientId
import org.thoughtcrime.securesms.attachments.Attachment
import org.thoughtcrime.securesms.util.MediaUtil

object KidsJournal {

  /** Hook: MessageTable.insertMessageInbox, just before it returns. */
  @JvmStatic
  fun onIncomingMessageInserted(threadId: Long, from: RecipientId, sentTimestamp: Long, body: String?, isStoryOrEdit: Boolean) {
    if (isStoryOrEdit) return
    JournalWriteJob.enqueueMessage(threadId, from, JournalDatabase.Direction.INCOMING, sentTimestamp, body)
  }

  /**
   * Hook: MessageTable.insertMessageOutbox, just before it returns. Journals the message and its
   * photos/videos/audio. Outgoing attachments are already local, so they can be copied now.
   */
  @JvmStatic
  fun onOutgoingMessageInserted(
    threadId: Long,
    threadRecipient: RecipientId,
    sentTimestamp: Long,
    body: String?,
    isStoryOrEdit: Boolean,
    insertedAttachments: Map<Attachment, AttachmentId>?
  ) {
    if (isStoryOrEdit) return
    JournalWriteJob.enqueueMessage(threadId, threadRecipient, JournalDatabase.Direction.OUTGOING, sentTimestamp, body)

    insertedAttachments.orEmpty()
      .filter { (attachment, _) -> attachment.uri != null && isJournaledMedia(attachment.contentType) }
      .forEach { (_, id) ->
        JournalWriteJob.enqueueMedia(threadId, threadRecipient, JournalDatabase.Direction.OUTGOING, sentTimestamp, id.id)
      }
  }

  /**
   * Hook: AttachmentTable.finalizeAttachmentAfterDownload. Incoming media can only be journaled once
   * it has downloaded. Restores from a backup are skipped so a restore doesn't flood the journal.
   */
  @JvmStatic
  fun onAttachmentDownloaded(threadId: Long, attachmentId: AttachmentId, contentType: String?, isStory: Boolean, isRestore: Boolean) {
    if (isStory || isRestore || !isJournaledMedia(contentType)) return
    JournalWriteJob.enqueueMedia(threadId, null, JournalDatabase.Direction.INCOMING, System.currentTimeMillis(), attachmentId.id)
  }

  /** Hooks: CallTable.insertOneToOneCall and CallTable.insertAcceptedGroupCall. */
  @JvmStatic
  fun onCallInserted(peer: RecipientId, outgoing: Boolean, timestamp: Long, callType: String, callEvent: String) {
    val direction = if (outgoing) JournalDatabase.Direction.OUTGOING else JournalDatabase.Direction.INCOMING
    JournalWriteJob.enqueueCall(null, peer, direction, timestamp, callType, callEvent)
  }

  /** A call refused by parental call blocking (it never reaches Signal's call log). */
  @JvmStatic
  fun onIncomingCallBlocked(peer: RecipientId, timestamp: Long, isVideo: Boolean) {
    JournalWriteJob.enqueueCall(null, peer, JournalDatabase.Direction.INCOMING, timestamp, if (isVideo) "VIDEO_CALL" else "AUDIO_CALL", CALL_EVENT_BLOCKED)
  }

  /** A group call ring refused by parental call blocking, journaled in the group's thread. */
  @JvmStatic
  fun onIncomingGroupCallBlocked(group: RecipientId, timestamp: Long) {
    JournalWriteJob.enqueueCall(null, group, JournalDatabase.Direction.INCOMING, timestamp, "GROUP_CALL", CALL_EVENT_BLOCKED)
  }

  /**
   * A message dropped by "Block unknown contacts" (KidsContactFilter). Stored as a MESSAGE entry so
   * kid-phone-server shows it as is, with a prefix saying it was blocked. Attachments are never
   * downloaded from unknown senders, so only their count is noted.
   */
  @JvmStatic
  fun onUnknownContactMessageBlocked(sender: RecipientId, timestamp: Long, body: String?, attachmentCount: Int) {
    val text = buildString {
      append(BLOCKED_PREFIX)
      if (!body.isNullOrEmpty()) append(body)
      if (attachmentCount > 0) {
        if (!body.isNullOrEmpty()) append(' ')
        append(if (attachmentCount == 1) "[1 attachment, not downloaded]" else "[$attachmentCount attachments, not downloaded]")
      }
    }
    JournalWriteJob.enqueueBlocked(sender, JournalDatabase.EntryType.MESSAGE, timestamp, text, null, null)
  }

  /** A call dropped by "Block unknown contacts"; shown like other blocked calls. */
  @JvmStatic
  fun onUnknownContactCallBlocked(sender: RecipientId, timestamp: Long, isVideo: Boolean) {
    JournalWriteJob.enqueueBlocked(sender, JournalDatabase.EntryType.CALL, timestamp, null, if (isVideo) "VIDEO_CALL" else "AUDIO_CALL", CALL_EVENT_BLOCKED)
  }

  private const val BLOCKED_PREFIX = "\uD83D\uDEAB Blocked (unknown contact): "

  /** Same event name the Molly-based builds used, so kid-phone-server shows it the same way. */
  const val CALL_EVENT_BLOCKED = "MISSED_CALL_BLOCKING"

  private fun isJournaledMedia(contentType: String?): Boolean {
    return MediaUtil.isImageType(contentType) || MediaUtil.isVideoType(contentType) || MediaUtil.isAudioType(contentType)
  }
}
