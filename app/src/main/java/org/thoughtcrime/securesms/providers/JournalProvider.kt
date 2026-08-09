package org.thoughtcrime.securesms.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.database.JournalDatabase
import java.io.File
import java.io.FileNotFoundException

/**
 * KIDS MDM IM: Read-only, signature-permission-protected surface the
 * companion launcher app uses to pull journal data (see JournalDatabase /
 * JournalWriteJob) for syncing to the parent's server.
 *
 * Unlike every other provider in this app, this one is intentionally
 * `android:exported="true"`, since it exists specifically to be read by a
 * different, trusted app (signed with the same key, holding the
 * `${applicationId}.ACCESS_JOURNAL` signature permission declared in
 * AndroidManifest.xml). It cannot extend BaseContentProvider, which
 * actively forbids being exported.
 *
 * Two URIs:
 *  - content://<authority>/entries/<sinceId> - up to [ENTRY_PAGE_SIZE] journal
 *    rows with _id > sinceId, oldest-first. The caller-supplied selection/
 *    selectionArgs/sortOrder are ignored (this provider builds its own fixed
 *    query) so there's no query-injection surface here.
 *  - content://<authority>/media/<fileName> - openFile for a journaled photo/
 *    video, previously copied into the private journal media directory by
 *    JournalWriteJob. fileName is validated to reject path traversal.
 */
class JournalProvider : ContentProvider() {

  companion object {
    private val TAG = Log.tag(JournalProvider::class.java)

    private const val JOURNAL_MEDIA_DIR = "kids-journal-media"
    private const val ENTRY_PAGE_SIZE = 200

    private const val CODE_ENTRIES_SINCE = 1
    private const val CODE_MEDIA = 2

    private val AUTHORITY = "${BuildConfig.APPLICATION_ID}.journal"

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
      addURI(AUTHORITY, "entries/#", CODE_ENTRIES_SINCE)
      addURI(AUTHORITY, "media/*", CODE_MEDIA)
    }
  }

  override fun onCreate(): Boolean {
    Log.i(TAG, "onCreate()")
    return true
  }

  override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
    if (uriMatcher.match(uri) != CODE_ENTRIES_SINCE) {
      return null
    }

    val sinceId = uri.lastPathSegment?.toLongOrNull() ?: return null
    val context = context ?: return null

    return JournalDatabase.getInstance(context.applicationContext as android.app.Application)
      .entries
      .queryEntriesSince(sinceId, ENTRY_PAGE_SIZE)
  }

  override fun getType(uri: Uri): String? {
    return when (uriMatcher.match(uri)) {
      CODE_ENTRIES_SINCE -> "vnd.android.cursor.dir/${AUTHORITY}.entries"
      CODE_MEDIA -> null // let the caller sniff it; media_content_type in the entry row has the real value
      else -> null
    }
  }

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    if (uriMatcher.match(uri) != CODE_MEDIA) {
      throw FileNotFoundException("Unsupported URI: $uri")
    }

    if (mode != "r") {
      throw SecurityException("Journal media is read-only")
    }

    val requestedName = uri.lastPathSegment ?: throw FileNotFoundException("Missing file name")
    val context = context ?: throw FileNotFoundException("No context")
    val mediaDir = context.applicationContext.getDir(JOURNAL_MEDIA_DIR, Context.MODE_PRIVATE)

    val file = File(mediaDir, requestedName)
    if (!file.canonicalPath.startsWith(mediaDir.canonicalPath + File.separator)) {
      Log.w(TAG, "Rejected path-traversal attempt: $requestedName")
      throw FileNotFoundException("Invalid file name")
    }

    if (!file.exists()) {
      throw FileNotFoundException("No such journal media file: $requestedName")
    }

    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    throw UnsupportedOperationException("JournalProvider is read-only")
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    throw UnsupportedOperationException("JournalProvider is read-only")
  }

  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
    throw UnsupportedOperationException("JournalProvider is read-only")
  }
}
