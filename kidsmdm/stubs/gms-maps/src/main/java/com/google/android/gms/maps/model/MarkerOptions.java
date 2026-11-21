/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;

public final class MarkerOptions {
  @NonNull
  public MarkerOptions position(@NonNull LatLng latLng) {
    return this;
  }
}
