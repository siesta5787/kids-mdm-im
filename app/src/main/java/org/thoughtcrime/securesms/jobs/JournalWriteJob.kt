package org.thoughtcrime.securesms.jobs

import android.app.Application
import android.content.Context
import android.webkit.MimeTypeMap
import org.signal.core.models.database.AttachmentId
import org.thoughtcrime.securesms.database.JournalDatabase
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.jobmanager.JsonJobData
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import java.io.File

/**
 * KIDS MDM IM: Mirrors one message/media/call event into the local
 * parental journal (see JournalDatabase). Queued from the message,
 * attachment, and call insert hooks so journal writes never block the
 * caller (which is sometimes the main thread, e.g. outgoing sends).
 *
 * For media entries, the attachment's plaintext bytes are copied into a
 * private journal-only directory here (not just referenced), so the
 * journal copy survives the original attachment/message later being
 * deleted, remote-deleted, or expiring. No compression is applied yet
 * (v1: straight copy) — a fast-follow can compress images/video before
 * writing them out, per AttachmentCompressionJob's approach.
 */
class JournalWriteJob private constructor(
  parameters: Parameters,
  private val threadId: Long,
  private val recipientId: String,
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
    private const val JOURNAL_MEDIA_DIR = "kids-journal-media"

    private const val KEY_THREAD_ID = "thread_id"
    private const val KEY_RECIPIENT_ID = "recipient_id"
    private const val KEY_DIRECTION = "direction"
    private const val KEY_ENTRY_TYPE = "entry_type"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_BODY = "body"
    private const val KEY_ATTACHMENT_ID = "attachment_id"
    private const val KEY_CALL_TYPE = "call_type"
    private const val KEY_CALL_EVENT = "call_event"

    @JvmStatic
    fun enqueueMessage(
      threadId: Long,
      recipientId: RecipientId,
      direction: JournalDatabase.Direction,
      timestamp: Long,
      body: String?
    ) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.MESSAGE, timestamp, body, null, null, null)
    }

    @JvmStatic
    fun enqueueMedia(
      threadId: Long,
      recipientId: RecipientId,
      direction: JournalDatabase.Direction,
      timestamp: Long,
      attachmentId: Long
    ) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.MEDIA, timestamp, null, attachmentId, null, null)
    }

    @JvmStatic
    fun enqueueCall(
      threadId: Long,
      recipientId: RecipientId,
      direction: JournalDatabase.Direction,
      timestamp: Long,
      callType: String,
      callEvent: String
    ) {
      enqueue(threadId, recipientId, direction, JournalDatabase.EntryType.CALL, timestamp, null, null, callType, callEvent)
    }

    private fun enqueue(
      threadId: Long,
      recipientId: RecipientId,
      direction: JournalDatabase.Direction,
      entryType: JournalDatabase.EntryType,
      timestamp: Long,
      body: String?,
      attachmentId: Long?,
      callType: String?,
      callEvent: String?
    ) {
      val job = JournalWriteJob(
        Parameters.Builder()
          .setQueue("JournalWriteJob")
          .setMaxAttempts(5)
          .build(),
        threadId,
        recipientId.serialize(),
        direction,
        entryType,
        timestamp,
        body,
        attachmentId,
        callType,
        callEvent
      )
      AppDependencies.jobManager.add(job)
    }
  }

  override fun serialize(): ByteArray {
    val builder = JsonJobData.Builder()
      .putLong(KEY_THREAD_ID, threadId)
      .putString(KEY_RECIPIENT_ID, recipientId)
      .putString(KEY_DIRECTION, direction.name)
      .putString(KEY_ENTRY_TYPE, entryType.name)
      .putLong(KEY_TIMESTAMP, timestamp)
      .putString(KEY_BODY, body)
      .putString(KEY_CALL_TYPE, callType)
      .putString(KEY_CALL_EVENT, callEvent)

    if (attachmentId != null) {
      builder.putLong(KEY_ATTACHMENT_ID, attachmentId)
    }

    return builder.serialize()
  }

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onShouldRetry(e: Exception): Boolean = true

  public override fun onRun() {
    val recipient = Recipient.resolved(RecipientId.from(recipientId))

    var mediaPath: String? = null
    var mediaContentType: String? = null

    if (entryType == JournalDatabase.EntryType.MEDIA && attachmentId != null) {
      val id = AttachmentId(attachmentId)
      val attachment = SignalDatabase.attachments.getAttachment(id)
      if (attachment == null) {
        return
      }

      val extension = attachment.contentType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
      val mediaDir = context.applicationContext.getDir(JOURNAL_MEDIA_DIR, Context.MODE_PRIVATE)
      val outFile = File(mediaDir, "${timestamp}_${attachmentId}.$extension")

      SignalDatabase.attachments.getAttachmentStream(id, 0).use { input ->
        outFile.outputStream().use { output -> input.copyTo(output) }
      }

      mediaPath = outFile.absolutePath
      mediaContentType = attachment.contentType
    }

    JournalDatabase.getInstance(context.applicationContext as Application).entries.insert(
      JournalDatabase.JournalEntry(
        threadId = threadId,
        recipientId = recipientId,
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
        data.getLong(KEY_THREAD_ID),
        data.getString(KEY_RECIPIENT_ID),
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
