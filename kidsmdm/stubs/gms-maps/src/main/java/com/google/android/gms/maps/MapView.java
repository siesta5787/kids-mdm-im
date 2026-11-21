/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** An empty view. getMapAsync never calls back, so Signal shows the address text without a map. */
public class MapView extends FrameLayout {
  public MapView(@NonNull Context context) {
    super(context);
  }

  public MapView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public MapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void getMapAsync(@NonNull OnMapReadyCallback callback) {}

  public void onCreate(@Nullable Bundle savedInstanceState) {}

  public void onStart() {}

  public void onResume() {}

  public void onPause() {}

  public void onStop() {}

  public void onDestroy() {}

  public void onLowMemory() {}

  public void onSaveInstanceState(@NonNull Bundle outState) {}
}
