/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/** Always reports Google Play services as missing, whether or not they're installed. */
public class GoogleApiAvailability {
  public static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";
  public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = 0;

  private static final GoogleApiAvailability INSTANCE = new GoogleApiAvailability();

  @NonNull
  public static GoogleApiAvailability getInstance() {
    return INSTANCE;
  }

  public int isGooglePlayServicesAvailable(@NonNull Context context) {
    return ConnectionResult.SERVICE_MISSING;
  }

  public int isGooglePlayServicesAvailable(@NonNull Context context, int minApkVersion) {
    return ConnectionResult.SERVICE_MISSING;
  }

  public boolean isUserResolvableError(int errorCode) {
    return false;
  }

  @Nullable
  public Dialog getErrorDialog(@NonNull Activity activity, int errorCode, int requestCode) {
    return new AlertDialog.Builder(activity)
        .setMessage("This app doesn't use Google Play services.")
        .setPositiveButton(android.R.string.ok, null)
        .create();
  }

  @NonNull
  public Task<Void> makeGooglePlayServicesAvailable(@NonNull Activity activity) {
    return Tasks.forException(new UnsupportedOperationException("Google Play services are not used"));
  }
}
