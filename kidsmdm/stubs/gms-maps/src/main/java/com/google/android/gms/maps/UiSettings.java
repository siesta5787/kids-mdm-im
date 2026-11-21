/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

public final class UiSettings {
  UiSettings() {}

  public void setAllGesturesEnabled(boolean enabled) {}

  public void setMyLocationButtonEnabled(boolean enabled) {}

  public void setZoomControlsEnabled(boolean enabled) {}
}
