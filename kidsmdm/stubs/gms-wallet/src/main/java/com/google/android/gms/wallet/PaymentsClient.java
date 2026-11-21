/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.wallet;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class PaymentsClient {
  PaymentsClient() {}

  @NonNull
  public Task<Boolean> isReadyToPay(@NonNull IsReadyToPayRequest request) {
    return Tasks.forResult(false);
  }

  @NonNull
  public Task<PaymentData> loadPaymentData(@NonNull PaymentDataRequest request) {
    return Tasks.forException(ApiException.unavailable());
  }
}
