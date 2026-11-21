/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall.model;

public final class SplitInstallErrorCode {
  public static final int NO_ERROR = 0;
  public static final int ACTIVE_SESSIONS_LIMIT_EXCEEDED = -1;
  public static final int MODULE_UNAVAILABLE = -2;
  public static final int INVALID_REQUEST = -3;
  public static final int SESSION_NOT_FOUND = -4;
  public static final int API_NOT_AVAILABLE = -5;
  public static final int NETWORK_ERROR = -6;
  public static final int ACCESS_DENIED = -7;
  public static final int INCOMPATIBLE_WITH_EXISTING_SESSION = -8;
  public static final int SERVICE_DIED = -9;
  public static final int INSUFFICIENT_STORAGE = -10;
  public static final int SPLITCOMPAT_VERIFICATION_ERROR = -11;
  public static final int SPLITCOMPAT_EMULATION_ERROR = -12;
  public static final int SPLITCOMPAT_COPY_ERROR = -13;
  public static final int PLAY_STORE_NOT_FOUND = -14;
  public static final int APP_NOT_OWNED = -15;
  public static final int INTERNAL_ERROR = -100;

  private SplitInstallErrorCode() {}
}
