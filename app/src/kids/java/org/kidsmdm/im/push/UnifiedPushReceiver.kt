/*
 * Kids MDM IM: receives broadcasts from the UnifiedPush distributor.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.gcm.FcmFetchManager
import org.thoughtcrime.securesms.keyvalue.SignalStore

/**
 * Exported (the distributor is another app), but every broadcast must carry our secret token.
 * A push carries no content: it only means "messages are waiting", and the app fetches them the
 * same way Signal does after an FCM push (FcmFetchManager).
 */
class UnifiedPushReceiver : BroadcastReceiver() {
  companion object {
    private val TAG = Log.tag(UnifiedPushReceiver::class.java)
  }

  override fun onReceive(context: Context, intent: Intent) {
    val token = intent.getStringExtra(UnifiedPushClient.EXTRA_TOKEN)
    if (token == null || token != KidsPushState.token) {
      Log.w(TAG, "Ignoring ${intent.action} with an unknown token")
      return
    }

    when (intent.action) {
      UnifiedPushClient.ACTION_NEW_ENDPOINT -> {
        val endpoint = intent.getStringExtra(UnifiedPushClient.EXTRA_ENDPOINT) ?: return
        if (endpoint != KidsPushState.endpoint) {
          Log.i(TAG, "New endpoint")
          KidsPushState.endpoint = endpoint
          KidsPush.refresh(fromNewEndpoint = true, testPing = true)
        }
      }

      UnifiedPushClient.ACTION_MESSAGE -> {
        intent.getStringExtra(UnifiedPushClient.EXTRA_MESSAGE_ID)?.let { id ->
          KidsPushState.distributor?.let { UnifiedPushClient.acknowledge(context, it, token, id) }
        }
        KidsPushState.lastPushAt = System.currentTimeMillis()
        val text = intent.getByteArrayExtra(UnifiedPushClient.EXTRA_BYTES_MESSAGE)?.toString(Charsets.UTF_8).orEmpty()
        if (text.contains("\"test\":true")) {
          Log.i(TAG, "Test push received")
          return
        }
        if (SignalStore.account.isRegistered && KidsPush.isPushActive()) {
          Log.i(TAG, "Push received; fetching messages")
          try {
            FcmFetchManager.startForegroundService(context)
          } catch (e: Exception) {
            Log.w(TAG, "Couldn't start the fetch service", e)
          }
          FcmFetchManager.enqueueFetch(context, true)
        }
      }

      UnifiedPushClient.ACTION_UNREGISTERED -> {
        Log.w(TAG, "The distributor unregistered us")
        val wasActive = KidsPush.isPushActive()
        KidsPushState.endpoint = null
        KidsPushState.status = KidsPush.Status.WAITING
        KidsPush.onPushActiveChanged(wasActive)
        KidsPush.refresh()
      }

      UnifiedPushClient.ACTION_REGISTRATION_FAILED -> {
        Log.w(TAG, "The distributor refused the registration")
        val wasActive = KidsPush.isPushActive()
        KidsPushState.endpoint = null
        KidsPushState.status = KidsPush.Status.NO_DISTRIBUTOR
        KidsPush.onPushActiveChanged(wasActive)
      }
    }
  }
}
