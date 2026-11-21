/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.auth.api.phone;

import android.content.Context;

import androidx.annotation.NonNull;

/** No SMS auto-read: registration falls back to typing the code in. */
public final class SmsRetriever {
  public static final String SMS_RETRIEVED_ACTION = "com.google.android.gms.auth.api.phone.SMS_RETRIEVED";
  public static final String SEND_PERMISSION = "com.google.android.gms.auth.api.phone.permission.SEND";
  public static final String EXTRA_STATUS = "com.google.android.gms.auth.api.phone.EXTRA_STATUS";
  public static final String EXTRA_SMS_MESSAGE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_MESSAGE";

  private SmsRetriever() {}

  @NonNull
  public static SmsRetrieverClient getClient(@NonNull Context context) {
    return new SmsRetrieverClient();
  }
}
