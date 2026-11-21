/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;

public final class LatLng {
  public final double latitude;
  public final double longitude;

  public LatLng(double latitude, double longitude) {
    this.latitude  = latitude;
    this.longitude = longitude;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LatLng)) return false;
    LatLng other = (LatLng) o;
    return Double.compare(latitude, other.latitude) == 0 && Double.compare(longitude, other.longitude) == 0;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
  }

  @NonNull
  @Override
  public String toString() {
    return "lat/lng: (" + latitude + "," + longitude + ")";
  }
}
