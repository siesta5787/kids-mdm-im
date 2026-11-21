/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall.model;

public final class SplitInstallSessionStatus {
  public static final int UNKNOWN = 0;
  public static final int PENDING = 1;
  public static final int DOWNLOADING = 2;
  public static final int DOWNLOADED = 3;
  public static final int INSTALLING = 4;
  public static final int INSTALLED = 5;
  public static final int FAILED = 6;
  public static final int CANCELED = 7;
  public static final int REQUIRES_USER_CONFIRMATION = 8;
  public static final int CANCELING = 9;

  private SplitInstallSessionStatus() {}
}
