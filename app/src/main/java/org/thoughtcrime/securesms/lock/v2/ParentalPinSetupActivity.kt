package org.thoughtcrime.securesms.lock.v2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.whispersystems.signalservice.api.kbs.PinHashUtil

/**
 * KIDS MDM IM: two-step "enter, then confirm" flow for creating or
 * changing the local parental PIN. Used both for mandatory first-time
 * setup (launched by AppSettingsActivity when no PIN exists yet — see
 * intentForCreate) and for changing an existing PIN (launched by
 * ParentalPinGateActivity only after the current PIN was re-verified —
 * see intentForChange).
 */
class ParentalPinSetupActivity : BaseActivity() {

  companion object {
    private const val EXTRA_IS_CREATE_FLOW = "is_create_flow"
    private const val MIN_PIN_LENGTH = 4

    fun intentForCreate(context: Context): Intent {
      return Intent(context, ParentalPinSetupActivity::class.java).putExtra(EXTRA_IS_CREATE_FLOW, true)
    }

    fun intentForChange(context: Context): Intent {
      return Intent(context, ParentalPinSetupActivity::class.java).putExtra(EXTRA_IS_CREATE_FLOW, false)
    }
  }

  private var firstEntry: String? = null
  private var isCreateFlow: Boolean = false

  private lateinit var title: TextView
  private lateinit var subtitle: TextView
  private lateinit var input: TextInputEditText
  private lateinit var error: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_parental_pin_setup)

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
  }

  private fun onSubmit() {
    val candidate = input.text?.toString().orEmpty()

    if (candidate.length < MIN_PIN_LENGTH) {
      showError(getString(R.string.parental_pin_setup__pin_too_short, MIN_PIN_LENGTH))
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

    savePin(candidate)
  }

  private fun savePin(pin: String) {
    lifecycleScope.launch {
      val hash = withContext(Dispatchers.Default) {
        PinHashUtil.localPinHash(pin)
      }

      SignalStore.parentalPin.pinHash = hash
      SignalStore.parentalPin.isEnabled = true

      if (isCreateFlow) {
        setResult(Activity.RESULT_OK)
      }
      finish()
    }
  }

  private fun showError(message: String) {
    error.text = message
    error.visibility = View.VISIBLE
  }
}
