/*
 * Kids MDM IM: hook points for Signal's library modules, which can't see the app's kids code.
 * Each Signal call site is a single line; the app (app/src/kids, org.kidsmdm.im.KidsMdm)
 * installs the implementations at startup. Unset hooks do nothing, so Signal behaves as stock.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.hooks

import androidx.compose.foundation.lazy.LazyListScope

object KidsHooks {
  /** Extra rows for Signal's main settings list (feature/app-settings AppSettingsScreen). */
  @Volatile
  var appSettingsContent: (LazyListScope.() -> Unit)? = null

  /** Hook: AppSettingsScreen's AppSettingsLazyColumn, right after the Account row. */
  @JvmStatic
  fun appSettingsItems(scope: LazyListScope) {
    appSettingsContent?.invoke(scope)
  }

  /**
   * Remote-config values forced on, keyed like Signal's service keys ("global.gifSearch"), as the
   * raw strings the service would send. They win over whatever Signal's server sends.
   */
  @Volatile
  var forcedRemoteConfig: Map<String, String> = emptyMap()

  /** Hook: lib/remote-config RemoteConfig.effectiveRawValue. */
  @JvmStatic
  fun remoteConfigOverride(key: String): String? = forcedRemoteConfig[key]
}
