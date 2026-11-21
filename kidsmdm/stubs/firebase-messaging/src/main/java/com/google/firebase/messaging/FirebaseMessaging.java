/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.firebase.messaging;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/** No FCM: getToken always fails, so Signal registers without a push token and uses its websocket. */
public final class FirebaseMessaging {
  private static final FirebaseMessaging INSTANCE = new FirebaseMessaging();

  private FirebaseMessaging() {}

  @NonNull
  public static FirebaseMessaging getInstance() {
    return INSTANCE;
  }

  @NonNull
  public Task<String> getToken() {
    return Tasks.forException(new UnsupportedOperationException("Kids MDM IM does not use Firebase Cloud Messaging"));
  }

  @NonNull
  public Task<Void> deleteToken() {
    return Tasks.forResult(null);
  }

  public void setAutoInitEnabled(boolean enabled) {}
}
