/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** An empty fragment. getMapAsync never calls back. */
public class SupportMapFragment extends Fragment {
  public SupportMapFragment() {}

  public void getMapAsync(@NonNull OnMapReadyCallback callback) {}
}
