/*
 * Kids MDM IM: the parent's approved contacts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kidsmdm.im.ApprovedContact
import org.kidsmdm.im.KidsApprovedContacts
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.recipients.Recipient

/**
 * Add people by phone number or username, or remove them. In the org.kidsmdm.im.settings
 * package, so the PIN gate locks it. Opened from Parental controls.
 */
class ApprovedContactsActivity : BaseActivity() {

  companion object {
    private val TAG = Log.tag(ApprovedContactsActivity::class.java)

    fun intent(context: Context): Intent = Intent(context, ApprovedContactsActivity::class.java)
  }

  private lateinit var input: EditText
  private lateinit var add: Button
  private lateinit var message: TextView
  private lateinit var list: LinearLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.kidsmdm_activity_approved_contacts)

    input = findViewById(R.id.kidsmdm_approved_input)
    add = findViewById(R.id.kidsmdm_approved_add)
    message = findViewById(R.id.kidsmdm_approved_message)
    list = findViewById(R.id.kidsmdm_approved_list)

    add.setOnClickListener { onAdd() }
    input.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) onAdd()
      actionId == EditorInfo.IME_ACTION_DONE
    }
    findViewById<View>(R.id.kidsmdm_approved_back).setOnClickListener { finish() }

    showList()
  }

  private fun onAdd() {
    val text = input.text.toString().trim()
    if (text.isEmpty() || !add.isEnabled) return

    add.isEnabled = false
    showMessage(getString(R.string.kidsmdm_approved__looking))
    lifecycleScope.launch {
      val result = withContext(Dispatchers.IO) {
        try {
          KidsApprovedContacts.lookUp(text)
        } catch (e: Exception) {
          Log.w(TAG, "Lookup failed", e)
          KidsApprovedContacts.LookupResult.NetworkError
        }
      }
      add.isEnabled = true
      when (result) {
        is KidsApprovedContacts.LookupResult.Found -> approve(result.contact)
        KidsApprovedContacts.LookupResult.NotFound -> showMessage(getString(R.string.kidsmdm_approved__not_found))
        KidsApprovedContacts.LookupResult.InvalidNumber -> showMessage(getString(R.string.kidsmdm_approved__invalid_number))
        KidsApprovedContacts.LookupResult.NetworkError -> showMessage(getString(R.string.kidsmdm_approved__network_error))
      }
    }
  }

  private fun approve(contact: ApprovedContact) {
    // Adding someone again replaces their entry.
    KidsSettings.approvedContacts = KidsSettings.approvedContacts.filter { it.id != contact.id } + contact
    input.setText("")
    showMessage(getString(R.string.kidsmdm_approved__added, contact.label))
    showList()
  }

  private fun showList() {
    lifecycleScope.launch {
      val rows = withContext(Dispatchers.IO) {
        KidsSettings.approvedContacts.map { it to Recipient.resolved(it.id).getDisplayName(this@ApprovedContactsActivity) }
      }
      list.removeAllViews()
      findViewById<View>(R.id.kidsmdm_approved_empty).visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
      rows.forEach { (contact, name) ->
        val row = layoutInflater.inflate(R.layout.kidsmdm_item_approved_contact, list, false)
        row.findViewById<TextView>(R.id.kidsmdm_approved_name).text = name
        row.findViewById<TextView>(R.id.kidsmdm_approved_label).text = contact.label
        row.findViewById<View>(R.id.kidsmdm_approved_remove).setOnClickListener {
          KidsSettings.approvedContacts = KidsSettings.approvedContacts.filter { it.id != contact.id }
          message.visibility = View.GONE
          showList()
        }
        list.addView(row)
      }
    }
  }

  private fun showMessage(text: String) {
    message.text = text
    message.visibility = View.VISIBLE
  }
}
