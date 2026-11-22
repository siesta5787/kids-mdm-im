/*
 * Kids MDM IM: UnifiedPush distributor helpers (from Molly's im.molly.unifiedpush.UnifiedPushDistributor, AGPL-3.0).
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.content.Context
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Molly's helper around the UnifiedPush connector library. Differences from Molly: the
 * distributor is picked automatically (the Kids MDM launcher if it's a distributor, instead of
 * Molly's selection dialog), and the endpoint is kept in [KidsPushState] instead of SignalStore.
 */
internal object UnifiedPushDistributor {

  const val LAUNCHER_PACKAGE = "com.kidslauncher.mdm"

  fun registerApp(vapid: String?) {
    UnifiedPush.register(AppDependencies.application, vapid = vapid)
  }

  fun unregisterApp() {
    UnifiedPush.unregister(AppDependencies.application)
    // MessagingReceiver.onUnregistered won't be called after the unregistration request
    KidsPushState.endpoint = null
  }

  /** Molly's selectFirstDistributor, preferring the launcher. False if there's no distributor. */
  fun selectDistributor(): Boolean {
    val context = AppDependencies.application
    val distributors = UnifiedPush.getDistributors(context)
    val distributor = distributors.firstOrNull { it == LAUNCHER_PACKAGE } ?: distributors.firstOrNull() ?: return false
    UnifiedPush.saveDistributor(context, distributor)
    return true
  }

  fun hasDistributor(context: Context): Boolean = UnifiedPush.getDistributors(context).isNotEmpty()

  fun checkIfActive(): Boolean {
    return UnifiedPush.getAckDistributor(AppDependencies.application) != null
  }
}
