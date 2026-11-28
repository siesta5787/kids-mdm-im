/*
 * Kids MDM IM: only parent-approved people can be messaged or called, in both directions.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.signal.core.models.recipients.RecipientId
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.GroupTable
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.profiles.manage.UsernameRepository
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientRepository

/**
 * The parent's list of approved people (Parental controls → Approved contacts). While
 * [KidsSettings.approvedContactsOnly] is on:
 *  - a 1:1 chat is allowed only with an approved person;
 *  - a group is allowed only if **every** member is approved, so adding a stranger closes it;
 *  - incoming content is dropped by [KidsContactFilter]; outgoing messages, reactions, typing,
 *    read receipts and calls are refused by the hooks below, and unapproved members are left out of
 *    every group send ([isEligibleRecipient]).
 *
 * People are stored as the Signal account found for the number or username the parent typed, not as
 * the text: incoming messages often carry no phone number and never a username.
 *
 * Everything returns "allowed" without touching the database while the switch is off.
 */
object KidsApprovedContacts {
  private val TAG = Log.tag(KidsApprovedContacts::class.java)

  val enabled: Boolean get() = KidsSettings.approvedContactsOnly

  /** True if [person] may exchange messages and calls with the kid. */
  fun isPersonApproved(person: Recipient): Boolean {
    if (!enabled || person.isSelf || person.isReleaseNotes) return true
    // Recipient.resolved follows Signal's remapping when two records for one account are merged.
    return KidsSettings.approvedContacts.any { it.id == person.id || Recipient.resolved(it.id).id == person.id }
  }

  /** True if the conversation with [recipient] (a person or a group) may be used. */
  fun isConversationAllowed(recipient: Recipient): Boolean {
    if (!enabled) return true
    return when {
      // Stories are off by default; their targets are filtered by isEligibleRecipient.
      recipient.isDistributionList -> true
      recipient.isGroup -> {
        val members = SignalDatabase.groups.getGroupMembers(recipient.requireGroupId(), GroupTable.MemberSet.FULL_MEMBERS_INCLUDING_SELF)
        members.isNotEmpty() && members.all { isPersonApproved(it) }
      }
      else -> isPersonApproved(recipient)
    }
  }

  /** Hooks: ReactionSendJob, TypingSendJob, SendReadReceiptJob. True if nothing may be sent to this conversation. */
  @JvmStatic
  fun isBlocked(conversationId: RecipientId): Boolean {
    return enabled && !isConversationAllowed(Recipient.resolved(conversationId))
  }

  /** Hook: RecipientUtil.getEligibleForSending. Leaves unapproved people out of every send that goes to a list of members. */
  @JvmStatic
  fun isEligibleRecipient(recipient: Recipient): Boolean = isPersonApproved(recipient)

  /**
   * Hooks: IndividualSendJobV2 and PushGroupSendJob, before anything is sent. For a message to a
   * conversation that isn't allowed: marks it failed, explains, and returns true.
   *
   * Group updates (leaving or changing a group) go through; [isEligibleRecipient] still keeps them
   * from unapproved members.
   */
  @JvmStatic
  fun refuseSend(messageId: Long): Boolean {
    if (!enabled) return false
    val message = SignalDatabase.messages.getOutgoingMessageOrNull(messageId) ?: return false
    if (message.isGroupUpdate) return false
    val recipient = message.threadRecipient.resolve()
    if (isConversationAllowed(recipient)) return false

    Log.w(TAG, "Not sending message $messageId: the conversation isn't approved.")
    SignalDatabase.messages.markAsSentFailed(messageId)
    showNotApproved(recipient)
    return true
  }

  fun showNotApproved(recipient: Recipient) {
    val text = if (recipient.isGroup) R.string.kidsmdm_group_not_approved else R.string.kidsmdm_not_approved
    Handler(Looper.getMainLooper()).post { Toast.makeText(AppDependencies.application, text, Toast.LENGTH_LONG).show() }
  }

  sealed interface LookupResult {
    data class Found(val contact: ApprovedContact) : LookupResult
    data object NotFound : LookupResult
    data object InvalidNumber : LookupResult
    data object NetworkError : LookupResult
  }

  /** Finds the Signal account for a phone number or username the parent typed. Blocks on the network. */
  @Suppress("DEPRECATION")
  fun lookUp(input: String): LookupResult {
    val text = input.trim()
    return if (text.all { it.isDigit() || it in "+()-. " }) {
      when (val result = RecipientRepository.lookupNewE164(text)) {
        is RecipientRepository.PhoneLookupResult.Found -> LookupResult.Found(ApprovedContact(result.recipient.id, result.phone.value))
        is RecipientRepository.PhoneLookupResult.NotFound -> LookupResult.NotFound
        is RecipientRepository.PhoneLookupResult.InvalidPhone -> LookupResult.InvalidNumber
        RecipientRepository.LookupResult.NetworkError -> LookupResult.NetworkError
      }
    } else {
      val username = text.removePrefix("@")
      when (val result = UsernameRepository.fetchAciForUsername(username)) {
        is UsernameRepository.UsernameAciFetchResult.Success -> {
          LookupResult.Found(ApprovedContact(SignalDatabase.recipients.getOrInsertFromServiceId(result.aci), username))
        }
        UsernameRepository.UsernameAciFetchResult.NotFound -> LookupResult.NotFound
        UsernameRepository.UsernameAciFetchResult.NetworkError -> LookupResult.NetworkError
      }
    }
  }
}

/** An approved person: their Signal account and the number or username the parent typed. */
data class ApprovedContact(val id: RecipientId, val label: String)
