/*
 * Kids MDM IM: parental voice/video call blocking.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.content.Context
import android.widget.Toast
import org.kidsmdm.im.journal.KidsJournal
import org.signal.core.models.recipients.RecipientId
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R

/** Independent voice and video call blocking, both directions. Set in [settings.KidsSettingsActivity]. */
object KidsCallBlocking {
  private val TAG = Log.tag(KidsCallBlocking::class.java)

  private fun isBlocked(isVideo: Boolean): Boolean {
    return if (isVideo) KidsSettings.blockVideoCalls else KidsSettings.blockVoiceCalls
  }

  /** Hook: CommunicationActions.startCallInternal. Returns true (and explains) if the call must not start. */
  @JvmStatic
  fun blockOutgoingCall(context: Context, isVideo: Boolean): Boolean {
    if (!isBlocked(isVideo)) return false
    Toast.makeText(context, R.string.kidsmdm_call_blocked, Toast.LENGTH_LONG).show()
    return true
  }

  /**
   * Hook: WebRtcActionProcessor.handleReceivedOffer. Returns true if the incoming call must be
   * hung up. The call is journaled as MISSED_CALL_BLOCKING and doesn't appear in the kid's call log.
   */
  @JvmStatic
  fun blockIncomingCall(peer: RecipientId, timestamp: Long, isVideo: Boolean): Boolean {
    if (!isBlocked(isVideo)) return false
    Log.w(TAG, "Incoming ${if (isVideo) "video" else "voice"} call blocked by parental settings.")
    KidsJournal.onIncomingCallBlocked(peer, timestamp, isVideo)
    return true
  }
}
