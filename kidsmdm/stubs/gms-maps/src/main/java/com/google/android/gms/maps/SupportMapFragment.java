/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * An empty fragment. getMapAsync never calls back. It must still create a view: Signal's place
 * picker declares it with a layout <fragment> tag, and Android throws "did not create a view"
 * for a tag fragment without one.
 */
public class SupportMapFragment extends Fragment {
  public SupportMapFragment() {}

  @NonNull
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return new FrameLayout(inflater.getContext());
  }

  public void getMapAsync(@NonNull OnMapReadyCallback callback) {}
}
