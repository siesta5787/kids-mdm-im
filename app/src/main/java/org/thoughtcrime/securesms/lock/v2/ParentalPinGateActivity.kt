package org.thoughtcrime.securesms.lock.v2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
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
 * KIDS MDM IM: modal gate shown before Settings is reachable. Verifies
 * the locally-stored parental PIN (see ParentalPinValues/PinHashUtil) —
 * entirely offline, independent of Signal's account PIN. Finishes with
 * RESULT_OK on a correct PIN, RESULT_CANCELED on back/failure to leave.
 */
class ParentalPinGateActivity : BaseActivity() {

  companion object {
    fun intent(context: Context): Intent = Intent(context, ParentalPinGateActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_parental_pin_gate)

    val input = findViewById<TextInputEditText>(R.id.parental_pin_input)
    val error = findViewById<android.widget.TextView>(R.id.parental_pin_error)

    findViewById<android.view.View>(R.id.parental_pin_submit).setOnClickListener {
      verify(input.text?.toString().orEmpty(), error)
    }

    findViewById<android.view.View>(R.id.parental_pin_change).setOnClickListener {
      verify(input.text?.toString().orEmpty(), error, thenLaunchChange = true)
    }

    input.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_GO) {
        verify(input.text?.toString().orEmpty(), error)
        true
      } else {
        false
      }
    }
  }

  private fun verify(candidate: String, error: android.widget.TextView, thenLaunchChange: Boolean = false) {
    val storedHash = SignalStore.parentalPin.pinHash
    if (storedHash == null) {
      // Shouldn't happen (AppSettingsActivity only launches this when a PIN is enabled), fail closed.
      setResult(Activity.RESULT_CANCELED)
      finish()
      return
    }

    lifecycleScope.launch {
      val correct = withContext(Dispatchers.Default) {
        PinHashUtil.verifyLocalPinHash(storedHash, candidate)
      }

      if (correct) {
        if (thenLaunchChange) {
          startActivity(ParentalPinSetupActivity.intentForChange(this@ParentalPinGateActivity))
        }
        setResult(Activity.RESULT_OK)
        finish()
      } else {
        error.text = getString(R.string.parental_pin_gate__incorrect_pin)
        error.visibility = android.view.View.VISIBLE
      }
    }
  }
}
