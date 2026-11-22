/*
 * Kids MDM IM: the app side of the UnifiedPush Android protocol.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.app.BroadcastOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.signal.core.util.logging.Log

/**
 * Talks to a UnifiedPush distributor with plain broadcasts (unifiedpush.org/developers/spec/android),
 * instead of pulling in the connector library and its dependencies. The distributor is normally
 * the Kids MDM launcher ("Push notifications for other apps" in its settings), which identifies
 * us with Android 14's shared broadcast identity, so no PendingIntent handshake is needed.
 */
internal object UnifiedPushClient {
  private val TAG = Log.tag(UnifiedPushClient::class.java)

  const val LAUNCHER_PACKAGE = "com.kidslauncher.mdm"

  const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
  const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
  const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"
  const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
  const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
  const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
  const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"

  const val EXTRA_TOKEN = "token"
  const val EXTRA_ENDPOINT = "endpoint"
  const val EXTRA_BYTES_MESSAGE = "bytesMessage"
  const val EXTRA_MESSAGE_ID = "id"
  private const val EXTRA_APPLICATION = "application"
  private const val EXTRA_MESSAGE = "message"

  /** Installed distributors whose registration receiver is enabled. The launcher only enables its receiver while the distributor toggle is on. */
  fun distributors(context: Context): List<String> {
    val intent = Intent(ACTION_REGISTER)
    val receivers = if (Build.VERSION.SDK_INT >= 33) {
      context.packageManager.queryBroadcastReceivers(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.queryBroadcastReceivers(intent, 0)
    }
    return receivers.map { it.activityInfo.packageName }.filter { it != context.packageName }.distinct()
  }

  /** The Kids MDM launcher if it's a distributor right now, otherwise any other distributor. */
  fun pickDistributor(context: Context): String? {
    val distributors = distributors(context)
    return distributors.firstOrNull { it == LAUNCHER_PACKAGE } ?: distributors.firstOrNull()
  }

  fun register(context: Context, distributor: String, token: String) {
    Log.i(TAG, "Registering with distributor $distributor")
    send(
      context,
      Intent(ACTION_REGISTER)
        .setPackage(distributor)
        .putExtra(EXTRA_TOKEN, token)
        .putExtra(EXTRA_APPLICATION, context.packageName)
        .putExtra(EXTRA_MESSAGE, "Kids MDM IM")
    )
  }

  fun unregister(context: Context, distributor: String, token: String) {
    Log.i(TAG, "Unregistering from distributor $distributor")
    send(context, Intent(ACTION_UNREGISTER).setPackage(distributor).putExtra(EXTRA_TOKEN, token))
  }

  fun acknowledge(context: Context, distributor: String, token: String, messageId: String) {
    send(context, Intent(ACTION_MESSAGE_ACK).setPackage(distributor).putExtra(EXTRA_TOKEN, token).putExtra(EXTRA_MESSAGE_ID, messageId))
  }

  private fun send(context: Context, intent: Intent) {
    if (Build.VERSION.SDK_INT >= 34) {
      context.sendBroadcast(intent, null, BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle())
    } else {
      context.sendBroadcast(intent)
    }
  }
}
