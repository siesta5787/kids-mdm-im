/*
 * Kids MDM IM: fixed policy decisions that Signal's code asks about through one-line hooks.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.content.Context
import android.widget.Toast
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.conversation.AttachmentKeyboardButton

object KidsPolicy {
  /**
   * Location sharing is off: there are no maps without Google, and a child's phone shouldn't
   * hand out its location. The location permissions are also removed (app/src/kids/AndroidManifest.xml).
   */
  const val LOCATION_SHARING_ENABLED = false

  /** No "invite to Signal" anywhere (entry points hidden; [blockInvite] catches the rest). */
  const val INVITES_ENABLED = false

  /**
   * No megaphones: the cards above the chat list (invite friends, donate, backups upsells,
   * PIN/username reminders, ...). Several of them open settings-type screens directly.
   */
  const val MEGAPHONES_ENABLED = false

  /** No "submit a debug log?" prompts (they upload logs to Signal's servers). */
  const val DEBUG_LOG_PROMPTS_ENABLED = false

  /** No release-notes/announcements chat from Signal (it also carries donation requests). */
  const val RELEASE_NOTES_ENABLED = false

  /** Signal settings rows that are hidden, by their AppSettingsTestTags tag. */
  val HIDDEN_APP_SETTINGS_ROWS = setOf("row-donate", "row-invite")

  /** Hook: AttachmentKeyboardButtonAdapter.setButtons. */
  @JvmStatic
  fun filterAttachmentButtons(buttons: List<AttachmentKeyboardButton>): List<AttachmentKeyboardButton> {
    return buttons.filter { button ->
      when (button) {
        AttachmentKeyboardButton.LOCATION -> LOCATION_SHARING_ENABLED
        AttachmentKeyboardButton.PAYMENT -> false
        else -> true
      }
    }
  }

  /**
   * Hook: InviteActions.inviteUserToSignal, which every remaining invite button calls (shared
   * contacts, find-by-number, old SMS threads). Returns true, with a short explanation, if the
   * invite must not be sent.
   */
  @JvmStatic
  fun blockInvite(context: Context): Boolean {
    if (INVITES_ENABLED) return false
    Toast.makeText(context, R.string.kidsmdm_invites_off, Toast.LENGTH_SHORT).show()
    return true
  }
}
