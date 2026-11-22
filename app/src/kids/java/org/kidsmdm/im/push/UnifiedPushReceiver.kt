/*
 * Kids MDM IM: UnifiedPush receiver (from Molly's im.molly.unifiedpush.receiver.UnifiedPushReceiver, AGPL-3.0).
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.content.Context
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.gcm.FcmFetchManager
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.concurrent.SerialMonoLifoExecutor
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * Molly's receiver, minus what Signal doesn't have (Molly's passphrase lock, air-gapped
 * MollySocket and its alert notifications; our status shows in Parental controls instead).
 * MollySocket's pushes carry no content: each one starts Signal's fetch, exactly like a
 * high-priority FCM push (FcmReceiveService.handleReceivedNotification).
 */
class UnifiedPushReceiver : MessagingReceiver() {

  companion object {
    private val TAG = Log.tag(UnifiedPushReceiver::class.java)
  }

  private val executor = SerialMonoLifoExecutor(SignalExecutors.UNBOUNDED)

  override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
    Log.i(TAG, "onNewEndpoint($instance)")
    refreshEndpoint(endpoint.url)
  }

  override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
    // called when the registration is not possible, eg. no network
    Log.w(TAG, "onRegistrationFailed($instance): $reason")
    val wasActive = KidsPush.isPushActive()
    KidsPushState.status = KidsPush.Status.DISTRIBUTOR_FAILED
    KidsPush.onPushActiveChanged(wasActive)
  }

  override fun onUnregistered(context: Context, instance: String) {
    // called when this application is unregistered from receiving push messages
    // isPushAvailable becomes false => The websocket starts
    Log.i(TAG, "onUnregistered($instance)")
    UnifiedPush.removeDistributor(context)
    refreshEndpoint(null)
  }

  override fun onMessage(context: Context, message: PushMessage, instance: String) {
    val msg = message.content.toString(Charsets.UTF_8)
    KidsPushState.lastPushAt = System.currentTimeMillis()

    when {
      msg.contains("\"test\":true") -> {
        Log.d(TAG, "Test message received.")
        KidsPush.refresh(fromNewEndpoint = true)
      }

      else -> {
        if (SignalStore.account.isRegistered && KidsSettings.pushMethod == KidsPush.Method.UNIFIEDPUSH) {
          Log.d(TAG, "New message")
          executor.enqueue { handleReceivedNotification(context) }
        }
      }
    }
  }

  /** What Signal's (private) FcmReceiveService.handleReceivedNotification does for a high-priority push. */
  private fun handleReceivedNotification(context: Context) {
    try {
      FcmFetchManager.startForegroundService(context)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to start service.", e)
    }
    FcmFetchManager.enqueueFetch(context, true)
  }

  /**
   * Update UnifiedPush endpoint
   *
   * @return `true` if the endpoint has been updated, `false` if the endpoint is the same
   */
  private fun refreshEndpoint(endpoint: String?): Boolean {
    val stored = KidsPushState.endpoint
    return if (endpoint != stored) {
      val wasActive = KidsPush.isPushActive()
      KidsPushState.endpoint = endpoint
      KidsPush.onPushActiveChanged(wasActive)
      KidsPush.refresh(fromNewEndpoint = true)
      true
    } else {
      false
    }
  }
}
