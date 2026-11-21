/*
 * Kids MDM IM: fixed policy decisions that Signal's code asks about through one-line hooks.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import org.thoughtcrime.securesms.conversation.AttachmentKeyboardButton

object KidsPolicy {
  /**
   * Location sharing is off: there are no maps without Google, and a child's phone shouldn't
   * hand out its location. The location permissions are also removed (app/src/kids/AndroidManifest.xml).
   */
  const val LOCATION_SHARING_ENABLED = false

  /** Hook: AttachmentKeyboardButtonAdapter.setButtons. */
  @JvmStatic
  fun filterAttachmentButtons(buttons: List<AttachmentKeyboardButton>): List<AttachmentKeyboardButton> {
    return buttons.filter { button ->
      when (button) {
        AttachmentKeyboardButton.LOCATION -> LOCATION_SHARING_ENABLED
        else -> true
      }
    }
  }
}
