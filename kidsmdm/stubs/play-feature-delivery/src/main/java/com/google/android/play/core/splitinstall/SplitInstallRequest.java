/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.play.core.splitinstall;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SplitInstallRequest {
  private final List<Locale> languages;
  private final List<String> moduleNames;

  private SplitInstallRequest(List<Locale> languages, List<String> moduleNames) {
    this.languages   = Collections.unmodifiableList(languages);
    this.moduleNames = Collections.unmodifiableList(moduleNames);
  }

  @NonNull
  public static Builder newBuilder() {
    return new Builder();
  }

  @NonNull
  public List<Locale> getLanguages() {
    return languages;
  }

  @NonNull
  public List<String> getModuleNames() {
    return moduleNames;
  }

  public static class Builder {
    private final List<Locale> languages   = new ArrayList<>();
    private final List<String> moduleNames = new ArrayList<>();

    Builder() {}

    @NonNull
    public Builder addLanguage(@NonNull Locale locale) {
      languages.add(locale);
      return this;
    }

    @NonNull
    public Builder addModule(@NonNull String moduleName) {
      moduleNames.add(moduleName);
      return this;
    }

    @NonNull
    public SplitInstallRequest build() {
      return new SplitInstallRequest(new ArrayList<>(languages), new ArrayList<>(moduleNames));
    }
  }
}
