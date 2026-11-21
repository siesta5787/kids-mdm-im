/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.maps;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Never handed out: MapView/SupportMapFragment never call onMapReady. Only here so Signal's map
 * code compiles. Location sharing is disabled in Kids MDM IM.
 */
public final class GoogleMap {
  public static final int MAP_TYPE_NONE = 0;
  public static final int MAP_TYPE_NORMAL = 1;
  public static final int MAP_TYPE_SATELLITE = 2;
  public static final int MAP_TYPE_TERRAIN = 3;
  public static final int MAP_TYPE_HYBRID = 4;

  private GoogleMap() {}

  public void moveCamera(@NonNull CameraUpdate update) {}

  @Nullable
  public Marker addMarker(@NonNull MarkerOptions options) {
    return null;
  }

  public void setBuildingsEnabled(boolean enabled) {}

  public void setMapType(int type) {}

  public boolean setMapStyle(@Nullable MapStyleOptions style) {
    return false;
  }

  public void setMyLocationEnabled(boolean enabled) {}

  public void setPadding(int left, int top, int right, int bottom) {}

  @NonNull
  public UiSettings getUiSettings() {
    return new UiSettings();
  }

  @NonNull
  public CameraPosition getCameraPosition() {
    return new CameraPosition(new LatLng(0, 0), 0);
  }

  public void setOnCameraIdleListener(@Nullable OnCameraIdleListener listener) {}

  public void setOnCameraMoveStartedListener(@Nullable OnCameraMoveStartedListener listener) {}

  public void setOnMapLoadedCallback(@Nullable OnMapLoadedCallback callback) {}

  public void snapshot(@NonNull SnapshotReadyCallback callback) {}

  public interface OnCameraIdleListener {
    void onCameraIdle();
  }

  public interface OnCameraMoveStartedListener {
    int REASON_GESTURE = 1;
    int REASON_API_ANIMATION = 2;
    int REASON_DEVELOPER_ANIMATION = 3;

    void onCameraMoveStarted(int reason);
  }

  public interface OnMapLoadedCallback {
    void onMapLoaded();
  }

  public interface SnapshotReadyCallback {
    void onSnapshotReady(@Nullable Bitmap snapshot);
  }
}
