/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.common.api;

import androidx.annotation.NonNull;

public class ApiException extends Exception {
  @NonNull protected final Status mStatus;

  public ApiException(@NonNull Status status) {
    super(status.getStatusCode() + ": " + (status.getStatusMessage() != null ? status.getStatusMessage() : ""));
    this.mStatus = status;
  }

  @NonNull
  public Status getStatus() {
    return mStatus;
  }

  public int getStatusCode() {
    return mStatus.getStatusCode();
  }

  /** The service unavailable error every stub uses. */
  @NonNull
  public static ApiException unavailable() {
    return new ApiException(new Status(CommonStatusCodes.API_NOT_CONNECTED, "Google Play services are not used by this app"));
  }
}
