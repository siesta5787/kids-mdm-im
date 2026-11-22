/*
 * Kids MDM IM: parental settings storage.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.kidsmdm.im.push.KidsPush

/**
 * Parental settings, kept in their own private preferences file instead of Signal's key-value
 * store, so Signal's SignalStore needs no changes. Not included in Signal backups: these belong to
 * this install. Values are read through this object only, so they can later be pushed from
 * kid-phone-server through the launcher without touching the call sites.
 */
object KidsSettings {
  private const val FILE = "kidsmdm_settings"

  private const val PIN_HASH = "parental_pin.hash"
  private const val BLOCK_VOICE_CALLS = "call_blocking.block_voice"
  private const val BLOCK_VIDEO_CALLS = "call_blocking.block_video"
  private const val BLOCK_UNKNOWN_CONTACTS = "contacts.block_unknown"
  private const val PUSH_METHOD = "push.method"
  private const val MOLLYSOCKET_URL = "push.mollysocket_url"

  @Volatile
  private var prefs: SharedPreferences? = null

  fun init(context: Context) {
    prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
  }

  private fun requirePrefs(): SharedPreferences = prefs ?: error("KidsSettings.init was not called")

  var pinHash: String?
    get() = requirePrefs().getString(PIN_HASH, null)
    set(value) = requirePrefs().edit { putString(PIN_HASH, value) }

  var blockVoiceCalls: Boolean
    get() = prefs?.getBoolean(BLOCK_VOICE_CALLS, false) ?: false
    set(value) = requirePrefs().edit { putBoolean(BLOCK_VOICE_CALLS, value) }

  var blockVideoCalls: Boolean
    get() = prefs?.getBoolean(BLOCK_VIDEO_CALLS, false) ?: false
    set(value) = requirePrefs().edit { putBoolean(BLOCK_VIDEO_CALLS, value) }

  /** See [KidsContactFilter]. Off by default, like Molly. */
  var blockUnknownContacts: Boolean
    get() = prefs?.getBoolean(BLOCK_UNKNOWN_CONTACTS, false) ?: false
    set(value) = requirePrefs().edit { putBoolean(BLOCK_UNKNOWN_CONTACTS, value) }

  /** How new messages arrive; null until chosen (see [KidsPush]). */
  var pushMethod: KidsPush.Method?
    get() = prefs?.getString(PUSH_METHOD, null)?.let { runCatching { KidsPush.Method.valueOf(it) }.getOrNull() }
    set(value) = requirePrefs().edit { putString(PUSH_METHOD, value?.name) }

  /** The MollySocket server's address, e.g. https://pi.example.ts.net:8443. */
  var mollySocketUrl: String?
    get() = prefs?.getString(MOLLYSOCKET_URL, null)
    set(value) = requirePrefs().edit { putString(MOLLYSOCKET_URL, value) }
}
