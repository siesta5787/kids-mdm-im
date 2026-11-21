/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.common.api;

public class CommonStatusCodes {
  public static final int SUCCESS_CACHE = -1;
  public static final int SUCCESS = 0;
  public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
  public static final int SERVICE_DISABLED = 3;
  public static final int SIGN_IN_REQUIRED = 4;
  public static final int INVALID_ACCOUNT = 5;
  public static final int RESOLUTION_REQUIRED = 6;
  public static final int NETWORK_ERROR = 7;
  public static final int INTERNAL_ERROR = 8;
  public static final int DEVELOPER_ERROR = 10;
  public static final int ERROR = 13;
  public static final int INTERRUPTED = 14;
  public static final int TIMEOUT = 15;
  public static final int CANCELED = 16;
  public static final int API_NOT_CONNECTED = 17;
  public static final int REMOTE_EXCEPTION = 19;
  public static final int CONNECTION_SUSPENDED_DURING_CALL = 20;
  public static final int RECONNECTION_TIMED_OUT_DURING_UPDATE = 21;
  public static final int RECONNECTION_TIMED_OUT = 22;

  protected CommonStatusCodes() {}
}
