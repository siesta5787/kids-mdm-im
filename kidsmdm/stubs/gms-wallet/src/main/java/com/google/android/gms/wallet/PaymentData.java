/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.wallet;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Never produced: Google Pay is never available. */
public final class PaymentData {
  private PaymentData() {}

  @Nullable
  public static PaymentData getFromIntent(@NonNull Intent data) {
    return null;
  }

  @NonNull
  public String toJson() {
    return "{}";
  }
}
