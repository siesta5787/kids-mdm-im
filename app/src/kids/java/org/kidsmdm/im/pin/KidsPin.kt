/*
 * Kids MDM IM: the local parental PIN.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.pin

import org.kidsmdm.im.KidsSettings
import org.signal.core.util.PinHashUtil

/**
 * A local-only PIN, independent of the Signal account PIN, verified offline with Signal's own
 * local PIN hashing. Stored hashed in [KidsSettings].
 */
object KidsPin {
  const val MIN_LENGTH = 4

  val isSet: Boolean
    get() = KidsSettings.pinHash != null

  /** Slow (key stretching): call off the main thread. */
  fun verify(candidate: String): Boolean {
    val hash = KidsSettings.pinHash ?: return false
    return PinHashUtil.verifyLocalPinHash(hash, candidate)
  }

  /** Slow (key stretching): call off the main thread. */
  fun set(pin: String) {
    KidsSettings.pinHash = PinHashUtil.localPinHash(pin)
  }
}
