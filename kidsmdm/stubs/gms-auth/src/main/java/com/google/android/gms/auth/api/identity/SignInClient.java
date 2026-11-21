/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/** No phone-number hint: registration falls back to typing the number in. */
public class SignInClient {
  SignInClient() {}

  @NonNull
  public Task<PendingIntent> getPhoneNumberHintIntent(@NonNull GetPhoneNumberHintIntentRequest request) {
    return Tasks.forException(ApiException.unavailable());
  }

  @NonNull
  public String getPhoneNumberFromIntent(@Nullable Intent data) throws ApiException {
    throw ApiException.unavailable();
  }
}
