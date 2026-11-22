/*
 * Kids MDM IM: parental settings screen.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import org.kidsmdm.im.KidsSettings
import org.kidsmdm.im.pin.ParentalPinSetupActivity
import org.kidsmdm.im.push.KidsPush
import org.kidsmdm.im.push.PushChoiceActivity
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R

/**
 * Parental controls. In the org.kidsmdm.im.settings package, so the PIN gate locks it. Opened
 * from the "Parental controls" row that KidsMdm adds to Signal's settings list.
 */
class KidsSettingsActivity : BaseActivity() {

  companion object {
    fun intent(context: Context): Intent = Intent(context, KidsSettingsActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.kidsmdm_activity_settings)

    findViewById<MaterialSwitch>(R.id.kidsmdm_block_unknown_contacts).apply {
      isChecked = KidsSettings.blockUnknownContacts
      setOnCheckedChangeListener { _, checked -> KidsSettings.blockUnknownContacts = checked }
    }

    findViewById<MaterialSwitch>(R.id.kidsmdm_block_voice_calls).apply {
      isChecked = KidsSettings.blockVoiceCalls
      setOnCheckedChangeListener { _, checked -> KidsSettings.blockVoiceCalls = checked }
    }

    findViewById<MaterialSwitch>(R.id.kidsmdm_block_video_calls).apply {
      isChecked = KidsSettings.blockVideoCalls
      setOnCheckedChangeListener { _, checked -> KidsSettings.blockVideoCalls = checked }
    }

    findViewById<View>(R.id.kidsmdm_push_change).setOnClickListener {
      startActivity(PushChoiceActivity.intent(this, firstRun = false))
    }

    findViewById<View>(R.id.kidsmdm_change_pin).setOnClickListener {
      startActivity(ParentalPinSetupActivity.intentForChange(this))
    }

    findViewById<View>(R.id.kidsmdm_settings_back).setOnClickListener { finish() }
  }

  override fun onResume() {
    super.onResume()
    findViewById<TextView>(R.id.kidsmdm_push_status).text = pushStatusText()
  }

  private fun pushStatusText(): String {
    if (KidsSettings.pushMethod != KidsPush.Method.UNIFIEDPUSH) {
      return getString(if (KidsSettings.pushMethod == null) R.string.kidsmdm_push_status__not_chosen else R.string.kidsmdm_push_status__websocket)
    }
    val status = when (KidsPush.status) {
      KidsPush.Status.REGISTERED -> getString(R.string.kidsmdm_push_status__working)
      KidsPush.Status.NO_DISTRIBUTOR -> getString(R.string.kidsmdm_push__no_distributor)
      KidsPush.Status.SERVER_ERROR -> getString(R.string.kidsmdm_push_status__server_error)
      KidsPush.Status.DEVICE_LIMIT -> getString(R.string.kidsmdm_push_status__device_limit)
      KidsPush.Status.WAITING, KidsPush.Status.OFF -> getString(R.string.kidsmdm_push_status__waiting)
    }
    val lastPush = KidsPush.lastPushAt
    val last = if (lastPush > 0) "\n" + getString(R.string.kidsmdm_push_status__last_push, DateUtils.getRelativeTimeSpanString(lastPush)) else ""
    return getString(R.string.kidsmdm_push_status__unifiedpush, status) + last
  }
}
