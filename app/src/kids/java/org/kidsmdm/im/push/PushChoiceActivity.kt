/*
 * Kids MDM IM: choose how new messages arrive.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R
import java.io.IOException

/**
 * Websocket ("always connected") or UnifiedPush through the launcher and MollySocket. Shown once
 * on first run (KidsPush's first-run prompt) and from Parental controls.
 */
class PushChoiceActivity : BaseActivity() {

  companion object {
    private val TAG = Log.tag(PushChoiceActivity::class.java)
    private const val EXTRA_FIRST_RUN = "first_run"

    fun intent(context: Context, firstRun: Boolean): Intent {
      return Intent(context, PushChoiceActivity::class.java).putExtra(EXTRA_FIRST_RUN, firstRun)
    }
  }

  private lateinit var websocket: RadioButton
  private lateinit var unifiedPush: RadioButton
  private lateinit var unifiedPushDetails: View
  private lateinit var url: EditText
  private lateinit var error: TextView
  private lateinit var save: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.kidsmdm_activity_push_choice)

    websocket = findViewById(R.id.kidsmdm_push_websocket)
    unifiedPush = findViewById(R.id.kidsmdm_push_unifiedpush)
    unifiedPushDetails = findViewById(R.id.kidsmdm_push_unifiedpush_details)
    url = findViewById(R.id.kidsmdm_push_mollysocket_url)
    error = findViewById(R.id.kidsmdm_push_error)
    save = findViewById(R.id.kidsmdm_push_save)

    val firstRun = intent.getBooleanExtra(EXTRA_FIRST_RUN, false)
    findViewById<View>(R.id.kidsmdm_push_back).visibility = if (firstRun) View.GONE else View.VISIBLE
    findViewById<View>(R.id.kidsmdm_push_back).setOnClickListener { finish() }

    val current = KidsSettings.pushMethod ?: KidsPush.Method.WEBSOCKET
    websocket.isChecked = current == KidsPush.Method.WEBSOCKET
    unifiedPush.isChecked = current == KidsPush.Method.UNIFIEDPUSH
    url.setText(KidsSettings.mollySocketUrl.orEmpty())

    websocket.setOnClickListener { select(KidsPush.Method.WEBSOCKET) }
    unifiedPush.setOnClickListener { select(KidsPush.Method.UNIFIEDPUSH) }
    findViewById<View>(R.id.kidsmdm_push_websocket_row).setOnClickListener { select(KidsPush.Method.WEBSOCKET) }
    findViewById<View>(R.id.kidsmdm_push_unifiedpush_row).setOnClickListener { select(KidsPush.Method.UNIFIEDPUSH) }
    updateDetails()

    save.setOnClickListener { onSave() }
  }

  private fun select(method: KidsPush.Method) {
    websocket.isChecked = method == KidsPush.Method.WEBSOCKET
    unifiedPush.isChecked = method == KidsPush.Method.UNIFIEDPUSH
    updateDetails()
  }

  private fun updateDetails() {
    unifiedPushDetails.visibility = if (unifiedPush.isChecked) View.VISIBLE else View.GONE
    findViewById<View>(R.id.kidsmdm_push_no_distributor).visibility =
      if (unifiedPush.isChecked && !UnifiedPushDistributor.hasDistributor(this)) View.VISIBLE else View.GONE
    error.visibility = View.GONE
  }

  private fun onSave() {
    if (websocket.isChecked) {
      KidsPush.setMethod(KidsPush.Method.WEBSOCKET, KidsSettings.mollySocketUrl)
      finish()
      return
    }

    val input = url.text.toString().trim().trimEnd('/')
    val address = if (input.contains("://")) input else "https://$input"
    val httpUrl = address.toHttpUrlOrNull()
    if (input.isEmpty() || httpUrl == null || !httpUrl.isHttps) {
      showError(R.string.kidsmdm_push__error_address)
      return
    }

    save.isEnabled = false
    save.setText(R.string.kidsmdm_push__checking)
    lifecycleScope.launch {
      val reachable: Boolean? = withContext(Dispatchers.IO) {
        try {
          MollySocketClient.isMollySocketServer(httpUrl)
        } catch (e: IOException) {
          Log.w(TAG, "Couldn't reach the MollySocket server", e)
          null
        }
      }
      save.isEnabled = true
      save.setText(R.string.kidsmdm_push__save)
      when (reachable) {
        true -> {
          KidsPush.setMethod(KidsPush.Method.UNIFIEDPUSH, httpUrl.toString())
          Toast.makeText(this@PushChoiceActivity, R.string.kidsmdm_push__setting_up, Toast.LENGTH_LONG).show()
          finish()
        }
        false -> showError(R.string.kidsmdm_push__error_not_mollysocket)
        null -> showError(R.string.kidsmdm_push__error_unreachable)
      }
    }
  }

  private fun showError(message: Int) {
    error.setText(message)
    error.visibility = View.VISIBLE
  }
}
