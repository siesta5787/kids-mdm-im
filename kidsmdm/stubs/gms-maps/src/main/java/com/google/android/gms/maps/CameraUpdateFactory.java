/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

public final class CameraUpdateFactory {
  private CameraUpdateFactory() {}

  @NonNull
  public static CameraUpdate newLatLngZoom(@NonNull LatLng latLng, float zoom) {
    return new CameraUpdate();
  }
}
