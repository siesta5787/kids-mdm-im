/*
 * Kids MDM IM: startup wiring.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.kidsmdm.hooks.KidsHooks
import org.kidsmdm.im.pin.KidsPinGate
import org.kidsmdm.im.settings.KidsSettingsActivity
import org.signal.core.ui.compose.Rows
import org.thoughtcrime.securesms.R

/**
 * Installs everything Kids MDM adds that doesn't need a hook in Signal's code. Started by
 * [KidsInitProvider] before Signal's Application.onCreate.
 */
object KidsMdm {
  fun init(application: Application) {
    KidsSettings.init(application)
    KidsPinGate.install(application)
    KidsHooks.forcedRemoteConfig = KidsForcedConfig.VALUES
    KidsHooks.hiddenAppSettingsRows = KidsPolicy.HIDDEN_APP_SETTINGS_ROWS

    KidsHooks.appSettingsContent = {
      item {
        val context = LocalContext.current
        Rows.TextRow(
          text = stringResource(R.string.kidsmdm_settings__row),
          icon = painterResource(org.signal.core.ui.R.drawable.symbol_lock_24),
          onClick = { context.startActivity(KidsSettingsActivity.intent(context)) }
        )
      }
    }
  }
}
