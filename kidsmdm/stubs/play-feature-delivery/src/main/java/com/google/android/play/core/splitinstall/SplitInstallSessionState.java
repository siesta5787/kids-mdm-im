/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall;

import androidx.annotation.NonNull;

import java.util.List;

/** Never produced: no install session ever starts. */
public abstract class SplitInstallSessionState {
  public abstract int sessionId();

  public abstract int status();

  public abstract int errorCode();

  public abstract long bytesDownloaded();

  public abstract long totalBytesToDownload();

  @NonNull
  public abstract List<String> languages();

  @NonNull
  public abstract List<String> moduleNames();
}
