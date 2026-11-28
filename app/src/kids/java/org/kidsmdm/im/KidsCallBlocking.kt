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
import org.signal.ringrtc.CallException
import org.signal.ringrtc.CallManager
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
   * Whether any call video may flow. Hooks:
   *  - SignalCallManager.acceptCall: a call can't be answered with the camera on;
   *  - SignalCallManager's remote video / screen share events: the other person is shown as
   *    audio-only;
   *  - BroadcastVideoSink.onFrame: no call video frame is ever drawn. Runs for every frame, so it
   *    must stay cheap.
   */
  @JvmStatic
  fun isVideoAllowed(): Boolean = !isBlocked(isVideo = true)

  /**
   * Hooks: SignalCallManager.setEnableVideo and startScreenShare, the ways to start sending video
   * during a call that was allowed (a voice call). Returns true (and explains) if video must stay off.
   */
  @JvmStatic
  fun blockOutgoingVideo(context: Context): Boolean {
    if (isVideoAllowed()) return false
    Toast.makeText(context, R.string.kidsmdm_video_blocked, Toast.LENGTH_LONG).show()
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

  /**
   * Hook: IdleActionProcessor.handleGroupCallRingUpdate, for a new ring. Signal's group calls are
   * video calls (starting one goes through [blockOutgoingCall] as video), so the video switch
   * applies. Returns true if the phone must not ring: the ring is declined and journaled.
   */
  @JvmStatic
  fun blockIncomingGroupRing(callManager: CallManager, group: RecipientId, groupId: ByteArray, ringId: Long): Boolean {
    if (!isBlocked(isVideo = true)) return false
    Log.w(TAG, "Incoming group call ring blocked by parental settings.")
    try {
      callManager.cancelGroupRing(groupId, ringId, CallManager.RingCancelReason.DeclinedByUser)
    } catch (e: CallException) {
      Log.w(TAG, "Couldn't cancel the blocked ring", e)
    }
    KidsJournal.onIncomingGroupCallBlocked(group, System.currentTimeMillis())
    return true
  }
}
