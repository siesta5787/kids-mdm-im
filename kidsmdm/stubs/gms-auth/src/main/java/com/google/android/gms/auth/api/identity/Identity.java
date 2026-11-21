/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.auth.api.identity;

import android.content.Context;

import androidx.annotation.NonNull;

public final class Identity {
  private Identity() {}

  @NonNull
  public static SignInClient getSignInClient(@NonNull Context context) {
    return new SignInClient();
  }
}
