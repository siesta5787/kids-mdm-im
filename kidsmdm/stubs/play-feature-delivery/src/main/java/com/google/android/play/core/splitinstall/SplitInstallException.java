/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall;

public class SplitInstallException extends RuntimeException {
  private final int errorCode;

  public SplitInstallException(int errorCode) {
    super("Split install error " + errorCode + ": Google Play is not used by this app");
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }
}
