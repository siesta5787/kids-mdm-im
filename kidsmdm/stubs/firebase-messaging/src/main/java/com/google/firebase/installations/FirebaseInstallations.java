/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.firebase.installations;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public final class FirebaseInstallations {
  private static final FirebaseInstallations INSTANCE = new FirebaseInstallations();

  private FirebaseInstallations() {}

  @NonNull
  public static FirebaseInstallations getInstance() {
    return INSTANCE;
  }

  /** Nothing to delete. */
  @NonNull
  public Task<Void> delete() {
    return Tasks.forResult(null);
  }
}
