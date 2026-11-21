/*
 * Kids MDM IM: parental settings screen.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.materialswitch.MaterialSwitch
import org.kidsmdm.im.KidsSettings
import org.kidsmdm.im.pin.ParentalPinSetupActivity
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

    findViewById<View>(R.id.kidsmdm_change_pin).setOnClickListener {
      startActivity(ParentalPinSetupActivity.intentForChange(this))
    }

    findViewById<View>(R.id.kidsmdm_settings_back).setOnClickListener { finish() }
  }
}
