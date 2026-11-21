/*
 * Kids MDM IM: Signal remote-config values that are fixed for kids.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

/**
 * Signal remote-config values forced for Kids MDM IM, as the raw strings Signal's service would
 * send. Applied through KidsHooks.remoteConfigOverride, so Signal's server can't turn them back on.
 * Keys are Signal's service keys from lib/remote-config RemoteConfig.kt.
 */
object KidsForcedConfig {
  val VALUES: Map<String, String> = mapOf(
    // No GIF search (third-party GIF content).
    "global.gifSearch" to "false"
  )
}
