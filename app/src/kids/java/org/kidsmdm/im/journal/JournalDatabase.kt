/*
 * Kids MDM IM: parental-visibility journal of conversation activity.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.journal

import android.app.Application
import android.database.Cursor
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import org.signal.core.database.insertInto
import org.signal.core.database.select
import org.signal.core.util.logging.Log
import org.signal.storage.database.DatabaseSecret
import org.signal.storage.database.DatabaseSecretProvider
import org.signal.storage.database.SignalDatabaseOpenHelper
import org.signal.storage.database.SqlCipherDatabaseHook
import org.signal.storage.database.SqlCipherLibraryLoader
import org.thoughtcrime.securesms.database.SqlCipherDeletingErrorHandler

/**
 * A journal of messages, media and calls, kept in its own encrypted database (`kids-journal.db`,
 * same SQLCipher key as Signal's databases), independent of Signal's tables.
 *
 * Entries are written when a message/attachment/call is created (see [KidsJournal] and its hooks),
 * so later deletion, remote-delete or disappearing-message expiry of the original doesn't remove
 * them. The companion launcher reads them through [JournalProvider].
 *
 * The table and column names are a contract with kids-launcher-mdm's JournalSync: change them
 * there too.
 */
class JournalDatabase private constructor(
  application: Application,
  databaseSecret: DatabaseSecret
) : SQLiteOpenHelper(
  application,
  DATABASE_NAME,
  databaseSecret.asHex(),
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

    private const val DATABASE_VERSION = 2
    private const val DATABASE_NAME = "kids-journal.db"

    /** Lowest possible id base (2023-11-14), in case the clock is unset when the journal is created. */
    private const val MIN_ID_BASE = 1_700_000_000_000L

    /**
     * Each journal gets a base, the time in milliseconds when it was created, stored in
     * [META_TABLE]. Entry ids start above it, and the `thread_id` the launcher sees is base +
     * Signal's thread id. After a reinstall (or a journal that had to be recreated), Signal's ids
     * restart at 1, but the journal's keep counting up past everything already on the server:
     *  - the launcher only asks for `_id > ` its last synced id, so ids restarting at 1 would hide
     *    every new entry from it;
     *  - kid-phone-server groups conversations by thread id, so a restarted thread 1 would merge
     *    with a different contact's old thread 1.
     * An install would need more than one entry per millisecond over its whole life to overlap
     * the next one's base.
     */
    private fun newIdBase(): Long = maxOf(System.currentTimeMillis(), MIN_ID_BASE)

    private const val META_TABLE = "journal_meta"
    private const val META_ID_BASE = "id_base"

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

  val entries: JournalEntryTable by lazy { JournalEntryTable(this) }

  override fun onCreate(db: SQLiteDatabase) {
    Log.i(TAG, "onCreate()")
    db.execSQL(JournalEntryTable.CREATE_TABLE)
    JournalEntryTable.CREATE_INDEXES.forEach { db.execSQL(it) }
    setIdBase(db, newIdBase())
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    Log.i(TAG, "onUpgrade($oldVersion, $newVersion)")
    if (oldVersion < 2) {
      // Version 1 used Signal's ids as they are. Move the existing rows above a new base, so a
      // launcher still holding a cursor from an earlier install picks them up too.
      val base = newIdBase()
      db.execSQL(
        "UPDATE ${JournalEntryTable.TABLE_NAME} SET " +
          "${JournalEntryTable.ID} = ${JournalEntryTable.ID} + $base, " +
          "${JournalEntryTable.THREAD_ID} = ${JournalEntryTable.THREAD_ID} + $base"
      )
      setIdBase(db, base)
    }
  }

  private fun setIdBase(db: SQLiteDatabase, base: Long) {
    db.execSQL("CREATE TABLE IF NOT EXISTS $META_TABLE (key TEXT PRIMARY KEY, value INTEGER NOT NULL)")
    db.execSQL("INSERT OR REPLACE INTO $META_TABLE (key, value) VALUES ('$META_ID_BASE', $base)")
    // The next entry gets an id above the base (AUTOINCREMENT never goes below the highest
    // existing id either, so shifted rows are safe).
    db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${JournalEntryTable.TABLE_NAME}'")
    db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES ('${JournalEntryTable.TABLE_NAME}', $base)")
  }

  /** This journal's base, see [newIdBase]. */
  val idBase: Long by lazy {
    readableDatabase.compileStatement("SELECT value FROM $META_TABLE WHERE key = '$META_ID_BASE'").use { it.simpleQueryForLong() }
  }

  override fun onOpen(db: SQLiteDatabase) {
    db.setForeignKeyConstraintsEnabled(true)
  }

  override val sqlCipherDatabase: SQLiteDatabase
    get() = writableDatabase

  enum class Direction { INCOMING, OUTGOING }

  enum class EntryType { MESSAGE, MEDIA, CALL }

  data class JournalEntry(
    /** Signal's thread id; stored offset by [idBase]. */
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
          THREAD_ID to openHelper.idBase + entry.threadId,
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

    /** Rows with _id > [sinceId], oldest first, for the launcher's pull sync. */
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
