package org.thoughtcrime.securesms.keyvalue

/**
 * KIDS MDM IM: a dedicated, local-only PIN gating access to Settings and
 * the independent voice/video call-blocking toggles, separate from
 * Signal's own account Registration Lock/SVR PIN. Verified entirely
 * offline via PinHashUtil.localPinHash/verifyLocalPinHash (no network,
 * no dependency on the account recovery secret).
 *
 * Deliberately excluded from backup: this PIN is meant to stay tied to
 * this specific installation, not travel with a restored backup.
 */
class ParentalPinValues internal constructor(store: KeyValueStore) : SignalStoreValues(store) {
  companion object {
    const val PIN_HASH = "parental_pin.hash"
    const val PIN_ENABLED = "parental_pin.enabled"
  }

  public override fun onFirstEverAppLaunch() = Unit

  public override fun getKeysToIncludeInBackup(): List<String> = emptyList()

  var pinHash: String? by stringValue(PIN_HASH, null)

  var isEnabled: Boolean by booleanValue(PIN_ENABLED, false)
}
