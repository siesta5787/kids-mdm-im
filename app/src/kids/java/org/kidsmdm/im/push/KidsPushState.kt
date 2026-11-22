/*
 * Kids MDM IM: UnifiedPush runtime state.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * What the app currently knows about its UnifiedPush registration, in its own private preferences
 * file (Signal's SignalStore stays untouched). The parent's choice itself (method, MollySocket
 * address) lives in [org.kidsmdm.im.KidsSettings].
 */
internal object KidsPushState {
  private const val FILE = "kidsmdm_push"

  private const val TOKEN = "up.token"
  private const val DISTRIBUTOR = "up.distributor"
  private const val ENDPOINT = "up.endpoint"
  private const val STATUS = "up.status"
  private const val LAST_PUSH_AT = "up.last_push_at"
  private const val DEVICE_ID = "mollysocket.device_id"
  private const val DEVICE_PASSWORD = "mollysocket.password"

  @Volatile
  private var prefs: SharedPreferences? = null

  fun init(context: Context) {
    prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
  }

  private fun requirePrefs(): SharedPreferences = prefs ?: error("KidsPushState.init was not called")

  /** Our secret registration token with the distributor; every broadcast from it must carry it. */
  var token: String?
    get() = prefs?.getString(TOKEN, null)
    set(value) = requirePrefs().edit { putString(TOKEN, value) }

  /** Package of the distributor we registered with (normally the Kids MDM launcher). */
  var distributor: String?
    get() = prefs?.getString(DISTRIBUTOR, null)
    set(value) = requirePrefs().edit { putString(DISTRIBUTOR, value) }

  /** The push URL the distributor gave us; MollySocket posts to it. */
  var endpoint: String?
    get() = prefs?.getString(ENDPOINT, null)
    set(value) = requirePrefs().edit { putString(ENDPOINT, value) }

  var status: KidsPush.Status
    get() = prefs?.getString(STATUS, null)?.let { runCatching { KidsPush.Status.valueOf(it) }.getOrNull() } ?: KidsPush.Status.OFF
    set(value) = requirePrefs().edit { putString(STATUS, value.name) }

  var lastPushAt: Long
    get() = prefs?.getLong(LAST_PUSH_AT, 0L) ?: 0L
    set(value) = requirePrefs().edit { putLong(LAST_PUSH_AT, value) }

  /** The linked device MollySocket uses to watch for new messages. */
  var device: MollySocketClient.Device?
    get() {
      val prefs = prefs ?: return null
      val password = prefs.getString(DEVICE_PASSWORD, null) ?: return null
      return MollySocketClient.Device(prefs.getInt(DEVICE_ID, 0), password)
    }
    set(value) = requirePrefs().edit {
      putInt(DEVICE_ID, value?.id ?: 0)
      putString(DEVICE_PASSWORD, value?.password)
    }

  /** Forget the registration (keeps nothing that would make the next setup reuse stale values). */
  fun clear() {
    requirePrefs().edit {
      remove(TOKEN)
      remove(DISTRIBUTOR)
      remove(ENDPOINT)
      remove(DEVICE_ID)
      remove(DEVICE_PASSWORD)
      putString(STATUS, KidsPush.Status.OFF.name)
    }
  }
}
