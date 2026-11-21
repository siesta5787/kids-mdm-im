/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitcompat;

import android.content.Context;

import androidx.annotation.NonNull;

/** The APK already contains every language, so there's never anything to install. */
public class SplitCompat {
  private SplitCompat() {}

  public static boolean install(@NonNull Context context) {
    return false;
  }

  public static boolean installActivity(@NonNull Context context) {
    return false;
  }
}
