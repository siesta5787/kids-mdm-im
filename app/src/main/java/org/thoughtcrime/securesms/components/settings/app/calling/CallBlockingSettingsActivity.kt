package org.thoughtcrime.securesms.components.settings.app.calling

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.materialswitch.MaterialSwitch
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.keyvalue.SignalStore

/**
 * KIDS MDM IM: lets a parent independently block voice and/or video
 * calls. Only reachable from within the (PIN-gated) Settings screen —
 * see AppSettingsFragment's "Call blocking" row.
 */
class CallBlockingSettingsActivity : BaseActivity() {

  companion object {
    fun intent(context: Context): Intent = Intent(context, CallBlockingSettingsActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call_blocking_settings)

    val voiceSwitch = findViewById<MaterialSwitch>(R.id.call_blocking_voice_switch)
    val videoSwitch = findViewById<MaterialSwitch>(R.id.call_blocking_video_switch)

    voiceSwitch.isChecked = SignalStore.callBlocking.blockVoiceCalls
    videoSwitch.isChecked = SignalStore.callBlocking.blockVideoCalls

    voiceSwitch.setOnCheckedChangeListener { _, isChecked ->
      SignalStore.callBlocking.blockVoiceCalls = isChecked
    }

    videoSwitch.setOnCheckedChangeListener { _, isChecked ->
      SignalStore.callBlocking.blockVideoCalls = isChecked
    }
  }
}
