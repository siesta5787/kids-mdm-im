package org.thoughtcrime.securesms.database

import android.app.Application
import android.database.Cursor
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import org.signal.core.util.insertInto
import org.signal.core.util.logging.Log
import org.signal.core.util.select
import org.thoughtcrime.securesms.crypto.DatabaseSecret
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider

/**
 * KIDS MDM IM: A parental-visibility journal of conversation activity
 * (messages, media, calls), independent of the main Signal database.
 *
 * Entries are written the instant a message/attachment/call is created
 * (see the hooks in MessageTable/AttachmentTable/CallTable), so this
 * journal is unaffected by later deletion, remote-delete, or disappearing-
 * message expiration of the original row. It is read by the companion
 * launcher app via JournalProvider, a signature-permission-protected
 * ContentProvider.
 *
 * This is its own separate physical database, so it cannot do joins or
 * queries with any other tables.
 */
class JournalDatabase private constructor(
  application: Application,
  databaseSecret: DatabaseSecret
) :
  SQLiteOpenHelper(
    application,
    DATABASE_NAME,
    databaseSecret.asString(),
    null,
    DATABASE_VERSION,
    0,
    SqlCipherDeletingErrorHandler(DATABASE_NAME),
    SqlCipherDatabaseHook(),
    true
  ),
  SignalDatabaseOpenHelper {

  companion object {
    private val TAG = Log.tag(JournalDatabase::class.java)

    private const val DATABASE_VERSION = 1
    private const val DATABASE_NAME = "kids-journal.db"

    @Volatile
    private var instance: JournalDatabase? = null

    @JvmStatic
    fun getInstance(context: Application): JournalDatabase {
      if (instance == null) {
        synchronized(JournalDatabase::class.java) {
          if (instance == null) {
            SqlCipherLibraryLoader.load()
            instance = JournalDatabase(context, DatabaseSecretProvider.getOrCreateDatabaseSecret(context))
          }
        }
      }
      return instance!!
    }
  }

  @get:JvmName("entries")
  val entries: JournalEntryTable by lazy { JournalEntryTable(this) }

  override fun onCreate(db: SQLiteDatabase) {
    Log.i(TAG, "onCreate()")
    db.execSQL(JournalEntryTable.CREATE_TABLE)
    JournalEntryTable.CREATE_INDEXES.forEach { db.execSQL(it) }
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    Log.i(TAG, "onUpgrade($oldVersion, $newVersion)")
  }

  override fun onOpen(db: SQLiteDatabase) {
    db.setForeignKeyConstraintsEnabled(true)
  }

  override fun getSqlCipherDatabase(): SQLiteDatabase {
    return writableDatabase
  }

  enum class Direction { INCOMING, OUTGOING }

  enum class EntryType { MESSAGE, MEDIA, CALL }

  data class JournalEntry(
    val threadId: Long,
    val recipientId: String,
    val displayName: String?,
    val direction: Direction,
    val entryType: EntryType,
    val timestamp: Long,
    val body: String? = null,
    val mediaPath: String? = null,
    val mediaContentType: String? = null,
    val callType: String? = null,
    val callEvent: String? = null
  )

  class JournalEntryTable(private val openHelper: JournalDatabase) {
    companion object {
      const val TABLE_NAME = "journal_entry"
      const val ID = "_id"
      const val THREAD_ID = "thread_id"
      const val RECIPIENT_ID = "recipient_id"
      const val DISPLAY_NAME = "display_name"
      const val DIRECTION = "direction"
      const val ENTRY_TYPE = "entry_type"
      const val TIMESTAMP = "timestamp"
      const val BODY = "body"
      const val MEDIA_PATH = "media_path"
      const val MEDIA_CONTENT_TYPE = "media_content_type"
      const val CALL_TYPE = "call_type"
      const val CALL_EVENT = "call_event"
      const val CREATED_AT = "created_at"

      const val CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
          $ID INTEGER PRIMARY KEY AUTOINCREMENT,
          $THREAD_ID INTEGER NOT NULL,
          $RECIPIENT_ID TEXT NOT NULL,
          $DISPLAY_NAME TEXT,
          $DIRECTION TEXT NOT NULL,
          $ENTRY_TYPE TEXT NOT NULL,
          $TIMESTAMP INTEGER NOT NULL,
          $BODY TEXT,
          $MEDIA_PATH TEXT,
          $MEDIA_CONTENT_TYPE TEXT,
          $CALL_TYPE TEXT,
          $CALL_EVENT TEXT,
          $CREATED_AT INTEGER NOT NULL
        )
      """

      val CREATE_INDEXES = arrayOf(
        "CREATE INDEX journal_entry_thread_id_index ON $TABLE_NAME ($THREAD_ID)",
        "CREATE INDEX journal_entry_timestamp_index ON $TABLE_NAME ($TIMESTAMP)"
      )

      val PROJECTION = arrayOf(ID, THREAD_ID, RECIPIENT_ID, DISPLAY_NAME, DIRECTION, ENTRY_TYPE, TIMESTAMP, BODY, MEDIA_PATH, MEDIA_CONTENT_TYPE, CALL_TYPE, CALL_EVENT, CREATED_AT)
    }

    private val readableDatabase: SQLiteDatabase get() = openHelper.readableDatabase
    private val writableDatabase: SQLiteDatabase get() = openHelper.writableDatabase

    fun insert(entry: JournalEntry): Long {
      return writableDatabase
        .insertInto(TABLE_NAME)
        .values(
          THREAD_ID to entry.threadId,
          RECIPIENT_ID to entry.recipientId,
          DISPLAY_NAME to entry.displayName,
          DIRECTION to entry.direction.name,
          ENTRY_TYPE to entry.entryType.name,
          TIMESTAMP to entry.timestamp,
          BODY to entry.body,
          MEDIA_PATH to entry.mediaPath,
          MEDIA_CONTENT_TYPE to entry.mediaContentType,
          CALL_TYPE to entry.callType,
          CALL_EVENT to entry.callEvent,
          CREATED_AT to System.currentTimeMillis()
        )
        .run()
    }

    /** Cursor over rows with _id > [sinceId], oldest-first, for the launcher's pull-sync. */
    fun queryEntriesSince(sinceId: Long, limit: Int): Cursor {
      return readableDatabase
        .select(*PROJECTION)
        .from(TABLE_NAME)
        .where("$ID > ?", sinceId)
        .orderBy("$ID ASC")
        .limit(limit)
        .run()
    }
  }
}
