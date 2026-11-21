/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall;

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SplitInstallManagerFactory {
  private SplitInstallManagerFactory() {}

  /** A manager that can't install anything: every request fails with API_NOT_AVAILABLE, which Signal treats as permanent. */
  @NonNull
  public static SplitInstallManager create(@NonNull Context context) {
    return new UnavailableSplitInstallManager();
  }

  private static final class UnavailableSplitInstallManager implements SplitInstallManager {
    private static <T> Task<T> unavailable() {
      return Tasks.forException(new SplitInstallException(SplitInstallErrorCode.API_NOT_AVAILABLE));
    }

    @NonNull
    @Override
    public Task<Integer> startInstall(@NonNull SplitInstallRequest request) {
      return unavailable();
    }

    @NonNull
    @Override
    public Task<Void> cancelInstall(int sessionId) {
      return unavailable();
    }

    @NonNull
    @Override
    public Task<List<SplitInstallSessionState>> getSessionStates() {
      return Tasks.forResult(Collections.emptyList());
    }

    @NonNull
    @Override
    public Task<Void> deferredLanguageInstall(@NonNull List<Locale> languages) {
      return unavailable();
    }

    @NonNull
    @Override
    public Set<String> getInstalledLanguages() {
      return Collections.emptySet();
    }

    @NonNull
    @Override
    public Set<String> getInstalledModules() {
      return Collections.emptySet();
    }

    @Override
    public boolean startConfirmationDialogForResult(@NonNull SplitInstallSessionState state, @NonNull ActivityResultLauncher<IntentSenderRequest> launcher) {
      return false;
    }

    @Override
    public void registerListener(@NonNull SplitInstallStateUpdatedListener listener) {}

    @Override
    public void unregisterListener(@NonNull SplitInstallStateUpdatedListener listener) {}
  }
}
