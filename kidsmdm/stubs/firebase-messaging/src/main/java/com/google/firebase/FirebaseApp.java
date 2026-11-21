/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.firebase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class FirebaseApp {
  private FirebaseApp() {}

  /** There is no Firebase in Kids MDM IM; always null, like a build without google-services config. */
  @Nullable
  public static FirebaseApp initializeApp(@NonNull Context context) {
    return null;
  }
}
