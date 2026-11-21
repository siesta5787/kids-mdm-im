/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.wallet;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

public final class AutoResolveHelper {
  public static final int RESULT_ERROR = 1;

  private AutoResolveHelper() {}

  public static <T> void resolveTask(@NonNull Task<T> task, @NonNull Activity activity, int requestCode) {}

  @Nullable
  public static Status getStatusFromIntent(@Nullable Intent data) {
    return null;
  }
}
