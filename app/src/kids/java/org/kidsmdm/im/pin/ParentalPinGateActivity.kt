/*
 * Kids MDM IM: parental PIN prompt.
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
 * Shown by [KidsPinGate] on top of a locked screen. A correct PIN reveals the screen underneath;
 * backing out closes it. Verification is local and offline.
 */
class ParentalPinGateActivity : BaseActivity() {

  companion object {
    fun intent(context: Context): Intent = Intent(context, ParentalPinGateActivity::class.java)
  }

  private var decided = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.kidsmdm_activity_pin_gate)

    val input = findViewById<TextInputEditText>(R.id.parental_pin_input)
    val error = findViewById<TextView>(R.id.parental_pin_error)

    findViewById<View>(R.id.parental_pin_submit).setOnClickListener {
      verify(input.text?.toString().orEmpty(), error, thenChangePin = false)
    }
    findViewById<View>(R.id.parental_pin_change).setOnClickListener {
      verify(input.text?.toString().orEmpty(), error, thenChangePin = true)
    }
    input.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_GO) {
        verify(input.text?.toString().orEmpty(), error, thenChangePin = false)
        true
      } else {
        false
      }
    }

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          cancel()
        }
      }
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    // Closed some other way (e.g. the task was removed) without a decision: treat as cancelled.
    if (!decided && isFinishing) {
      KidsPinGate.onPinCancelled()
    }
  }

  private fun verify(candidate: String, error: TextView, thenChangePin: Boolean) {
    lifecycleScope.launch {
      val correct = withContext(Dispatchers.Default) { KidsPin.verify(candidate) }

      if (correct) {
        decided = true
        KidsPinGate.onPinAccepted()
        if (thenChangePin) {
          startActivity(ParentalPinSetupActivity.intentForChange(this@ParentalPinGateActivity))
        }
        finish()
      } else {
        error.text = getString(R.string.parental_pin_gate__incorrect_pin)
        error.visibility = View.VISIBLE
      }
    }
  }

  private fun cancel() {
    decided = true
    KidsPinGate.onPinCancelled()
    finish()
  }
}
