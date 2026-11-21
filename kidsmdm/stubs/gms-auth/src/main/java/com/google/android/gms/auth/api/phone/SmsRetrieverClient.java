/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.auth.api.phone;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class SmsRetrieverClient {
  SmsRetrieverClient() {}

  @NonNull
  public Task<Void> startSmsRetriever() {
    return Tasks.forException(ApiException.unavailable());
  }
}
