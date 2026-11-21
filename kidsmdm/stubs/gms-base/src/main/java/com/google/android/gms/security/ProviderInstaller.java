/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.security;

import android.content.Context;

import androidx.annotation.NonNull;

/** Signal only calls this to update the TLS provider when censorship circumvention is on; it ships Conscrypt anyway. */
public class ProviderInstaller {
  public static void installIfNeeded(@NonNull Context context) {
    throw new UnsupportedOperationException("Google Play services are not used by this app");
  }
}
