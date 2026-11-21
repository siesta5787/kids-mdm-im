/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;

public final class CameraPosition {
  @NonNull public final LatLng target;
  public final float zoom;

  public CameraPosition(@NonNull LatLng target, float zoom) {
    this.target = target;
    this.zoom   = zoom;
  }
}
