/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.wallet;

public final class WalletConstants {
  public static final int ENVIRONMENT_PRODUCTION = 1;
  public static final int ENVIRONMENT_TEST = 3;
  public static final int THEME_DARK = 0;
  public static final int THEME_LIGHT = 1;

  private WalletConstants() {}
}
