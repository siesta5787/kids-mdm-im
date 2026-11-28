/*
 * Kids MDM IM: one-time defaults for Signal's own settings.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.stories.Stories

/**
 * Defaults applied once per install, after registration. The parent can change them afterwards in
 * Settings (behind the PIN); they're never forced back.
 */
object KidsDefaults {
  private val TAG = Log.tag(KidsDefaults::class.java)

  /** Called when the main screen opens. */
  fun applyOnce() {
    if (KidsSettings.storiesDefaultApplied || !SignalStore.account.isRegistered) return
    KidsSettings.storiesDefaultApplied = true

    // Stories off: they aren't journaled. Same steps as Signal's "Turn off stories".
    SignalExecutors.BOUNDED.execute {
      Log.i(TAG, "Turning stories off by default.")
      SignalStore.story.isFeatureDisabled = true
      Stories.onStorySettingsChanged(Recipient.self().id)
      AppDependencies.resetNetwork()
      AppDependencies.startNetwork()
    }
  }
}
