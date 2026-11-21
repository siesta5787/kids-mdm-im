/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.common.api;

import androidx.annotation.Nullable;

public final class Status {
  private final int statusCode;
  @Nullable private final String statusMessage;

  public Status(int statusCode) {
    this(statusCode, null);
  }

  public Status(int statusCode, @Nullable String statusMessage) {
    this.statusCode    = statusCode;
    this.statusMessage = statusMessage;
  }

  public int getStatusCode() {
    return statusCode;
  }

  @Nullable
  public String getStatusMessage() {
    return statusMessage;
  }

  public boolean isSuccess() {
    return statusCode <= CommonStatusCodes.SUCCESS;
  }
}
