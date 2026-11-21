/*
 * Kids MDM IM: create or change the parental PIN.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.pin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R

/**
 * Two-step "enter, then confirm" flow. In create mode it is shown by [KidsPinGate] the first time a
 * locked screen opens, and the new PIN unlocks that screen. In change mode it is only reachable
 * after the current PIN was verified.
 */
class ParentalPinSetupActivity : BaseActivity() {

  companion object {
    private const val EXTRA_IS_CREATE_FLOW = "is_create_flow"

    fun intentForCreate(context: Context): Intent {
      return Intent(context, ParentalPinSetupActivity::class.java).putExtra(EXTRA_IS_CREATE_FLOW, true)
    }

    fun intentForChange(context: Context): Intent {
      return Intent(context, ParentalPinSetupActivity::class.java).putExtra(EXTRA_IS_CREATE_FLOW, false)
    }
  }

  private var firstEntry: String? = null
  private var isCreateFlow = false
  private var decided = false

  private lateinit var title: TextView
  private lateinit var subtitle: TextView
  private lateinit var input: TextInputEditText
  private lateinit var error: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.kidsmdm_activity_pin_setup)

    isCreateFlow = intent.getBooleanExtra(EXTRA_IS_CREATE_FLOW, true)

    title = findViewById(R.id.parental_pin_setup_title)
    subtitle = findViewById(R.id.parental_pin_setup_subtitle)
    input = findViewById(R.id.parental_pin_setup_input)
    error = findViewById(R.id.parental_pin_setup_error)

    findViewById<View>(R.id.parental_pin_setup_submit).setOnClickListener { onSubmit() }
    input.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_GO) {
        onSubmit()
        true
      } else {
        false
      }
    }

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          decided = true
          if (isCreateFlow) KidsPinGate.onPinCancelled()
          finish()
        }
      }
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isCreateFlow && !decided && isFinishing) {
      KidsPinGate.onPinCancelled()
    }
  }

  private fun onSubmit() {
    val candidate = input.text?.toString().orEmpty()

    if (candidate.length < KidsPin.MIN_LENGTH) {
      showError(getString(R.string.parental_pin_setup__pin_too_short, KidsPin.MIN_LENGTH))
      return
    }

    val pending = firstEntry
    if (pending == null) {
      firstEntry = candidate
      title.setText(R.string.parental_pin_setup__confirm_your_parental_pin)
      subtitle.visibility = View.GONE
      input.setText("")
      error.visibility = View.GONE
      return
    }

    if (candidate != pending) {
      showError(getString(R.string.parental_pin_setup__pins_did_not_match))
      firstEntry = null
      title.setText(R.string.parental_pin_setup__create_a_parental_pin)
      subtitle.visibility = View.VISIBLE
      input.setText("")
      return
    }

    lifecycleScope.launch {
      withContext(Dispatchers.Default) { KidsPin.set(candidate) }
      decided = true
      if (isCreateFlow) KidsPinGate.onPinAccepted()
      finish()
    }
  }

  private fun showError(message: String) {
    error.text = message
    error.visibility = View.VISIBLE
  }
}
