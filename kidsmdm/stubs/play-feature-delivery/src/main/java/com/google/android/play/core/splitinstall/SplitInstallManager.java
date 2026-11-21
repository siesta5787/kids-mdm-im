/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public interface SplitInstallManager {
  @NonNull
  Task<Integer> startInstall(@NonNull SplitInstallRequest request);

  @NonNull
  Task<Void> cancelInstall(int sessionId);

  @NonNull
  Task<List<SplitInstallSessionState>> getSessionStates();

  @NonNull
  Task<Void> deferredLanguageInstall(@NonNull List<Locale> languages);

  @NonNull
  Set<String> getInstalledLanguages();

  @NonNull
  Set<String> getInstalledModules();

  boolean startConfirmationDialogForResult(@NonNull SplitInstallSessionState state, @NonNull ActivityResultLauncher<IntentSenderRequest> launcher);

  void registerListener(@NonNull SplitInstallStateUpdatedListener listener);

  void unregisterListener(@NonNull SplitInstallStateUpdatedListener listener);
}
