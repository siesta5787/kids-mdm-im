/*
 * Kids MDM IM: writes one journal entry in the background.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.journal

import android.app.Application
import android.content.Context
import android.webkit.MimeTypeMap
import org.signal.core.models.database.AttachmentId
import org.signal.core.models.recipients.RecipientId
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.jobmanager.JsonJobData
import org.thoughtcrime.securesms.jobs.BaseJob
import org.thoughtcrime.securesms.recipients.Recipient
import java.io.File

/**
 * Mirrors one message/media/call event into [JournalDatabase]. Queued from [KidsJournal] so journal
 * writes never block the caller, which is sometimes the main thread (outgoing sends).
 *
 * Media bytes are copied into a private journal-only directory, so the journal copy survives the
 * original being deleted or expiring. The launcher reads them back via [JournalProvider].
 *
 * [threadId]/[recipientId] may be null and are then resolved here, off the caller's thread: calls
 * know only the peer, and downloaded attachments know only the thread.
 *
 * Registered in Signal's JobManagerFactories under [KEY] (one-line hook).
 */
class JournalWriteJob private constructor(
  parameters: Parameters,
  private val threadId: Long?,
  private val recipientId: String?,
  private val direction: JournalDatabase.Direction,
  private val entryType: JournalDatabase.EntryType,
  private val timestamp: Long,
  private val body: String?,
  private val attachmentId: Long?,
  private val callType: String?,
  private val callEvent: String?
) : BaseJob(parameters) {

  companion object {
    const val KEY = "JournalWriteJob"
    const val JOURNAL_MEDIA_DIR = "kids-journal-media"

    private const val KEY_THREAD_ID = "thread_id"
    private const val KEY_RECIPIENT_ID = "recipient_id"
    private const val KEY_DIRECTION = "direction"
    private const val KEY_ENTRY_TYPE = "entry_type"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_BODY = "body"
    private const val KEY_ATTACHMENT_ID = "attachment_id"
    private const val KEY_CALL_TYPE = "call_type"
    private const val KEY_CALL_EVENT = "call_event"

    fun enqueueMessage(threadId: Long, recipientId: RecipientId, direction: JournalDatabase.Direction, timestamp: Long, body: String?) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.MESSAGE, timestamp, body, null, null, null)
    }

    fun enqueueMedia(threadId: Long, recipientId: RecipientId?, direction: JournalDatabase.Direction, timestamp: Long, attachmentId: Long) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.MEDIA, timestamp, null, attachmentId, null, null)
    }

    fun enqueueCall(threadId: Long?, recipientId: RecipientId, direction: JournalDatabase.Direction, timestamp: Long, callType: String, callEvent: String) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.CALL, timestamp, null, null, callType, callEvent)
    }

    private fun enqueue(
      threadId: Long?,
      recipientId: RecipientId?,
      direction: JournalDatabase.Direction,
      entryType: JournalDatabase.EntryType,
      timestamp: Long,
      body: String?,
      attachmentId: Long?,
      callType: String?,
      callEvent: String?
    ) {
      AppDependencies.jobManager.add(
        JournalWriteJob(
          Parameters.Builder()
            .setQueue(KEY)
            .setMaxAttempts(5)
            .build(),
          threadId,
          recipientId?.serialize(),
          direction,
          entryType,
          timestamp,
          body,
          attachmentId,
          callType,
          callEvent
        )
      )
    }
  }

  override fun serialize(): ByteArray? {
    val builder = JsonJobData.Builder()
      .putString(KEY_DIRECTION, direction.name)
      .putString(KEY_ENTRY_TYPE, entryType.name)
      .putLong(KEY_TIMESTAMP, timestamp)
      .putString(KEY_RECIPIENT_ID, recipientId)
      .putString(KEY_BODY, body)
      .putString(KEY_CALL_TYPE, callType)
      .putString(KEY_CALL_EVENT, callEvent)

    if (threadId != null) builder.putLong(KEY_THREAD_ID, threadId)
    if (attachmentId != null) builder.putLong(KEY_ATTACHMENT_ID, attachmentId)

    return builder.serialize()
  }

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onShouldRetry(e: Exception): Boolean = true

  public override fun onRun() {
    val resolvedRecipientId: RecipientId = recipientId?.let { RecipientId.from(it) }
      ?: threadId?.let { SignalDatabase.threads.getRecipientIdForThreadId(it) }
      ?: return
    val recipient = Recipient.resolved(resolvedRecipientId)
    val resolvedThreadId: Long = threadId ?: SignalDatabase.threads.getOrCreateThreadIdFor(recipient)

    var mediaPath: String? = null
    var mediaContentType: String? = null

    if (entryType == JournalDatabase.EntryType.MEDIA && attachmentId != null) {
      val id = AttachmentId(attachmentId)
      val attachment = SignalDatabase.attachments.getAttachment(id) ?: return

      val extension = attachment.contentType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
      val mediaDir = context.applicationContext.getDir(JOURNAL_MEDIA_DIR, Context.MODE_PRIVATE)
      val outFile = File(mediaDir, "${timestamp}_$attachmentId.$extension")

      SignalDatabase.attachments.getAttachmentStream(id, 0).use { input ->
        outFile.outputStream().use { output -> input.copyTo(output) }
      }

      mediaPath = outFile.absolutePath
      mediaContentType = attachment.contentType
    }

    JournalDatabase.getInstance(context.applicationContext as Application).entries.insert(
      JournalDatabase.JournalEntry(
        threadId = resolvedThreadId,
        recipientId = resolvedRecipientId.serialize(),
        displayName = recipient.getDisplayName(context),
        direction = direction,
        entryType = entryType,
        timestamp = timestamp,
        body = body,
        mediaPath = mediaPath,
        mediaContentType = mediaContentType,
        callType = callType,
        callEvent = callEvent
      )
    )
  }

  class Factory : Job.Factory<JournalWriteJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): JournalWriteJob {
      val data = JsonJobData.deserialize(serializedData)
      return JournalWriteJob(
        parameters,
        if (data.hasLong(KEY_THREAD_ID)) data.getLong(KEY_THREAD_ID) else null,
        data.getStringOrDefault(KEY_RECIPIENT_ID, null),
        JournalDatabase.Direction.valueOf(data.getString(KEY_DIRECTION)),
        JournalDatabase.EntryType.valueOf(data.getString(KEY_ENTRY_TYPE)),
        data.getLong(KEY_TIMESTAMP),
        data.getStringOrDefault(KEY_BODY, null),
        if (data.hasLong(KEY_ATTACHMENT_ID)) data.getLong(KEY_ATTACHMENT_ID) else null,
        data.getStringOrDefault(KEY_CALL_TYPE, null),
        data.getStringOrDefault(KEY_CALL_EVENT, null)
      )
    }
  }
}
