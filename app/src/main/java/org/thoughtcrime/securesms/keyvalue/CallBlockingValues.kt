package org.thoughtcrime.securesms.keyvalue

/**
 * KIDS MDM IM: independent toggles for blocking voice and video calls
 * (both outgoing and incoming), editable only from the PIN-gated
 * Settings screen. See CommunicationActions (outgoing) and
 * WebRtcActionProcessor (incoming) for the enforcement points.
 */
class CallBlockingValues internal constructor(store: KeyValueStore) : SignalStoreValues(store) {
  companion object {
    const val BLOCK_VOICE_CALLS = "call_blocking.block_voice"
    const val BLOCK_VIDEO_CALLS = "call_blocking.block_video"
  }

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): List<String> = emptyList()

  var blockVoiceCalls: Boolean by booleanValue(BLOCK_VOICE_CALLS, false)

  var blockVideoCalls: Boolean by booleanValue(BLOCK_VIDEO_CALLS, false)
}
