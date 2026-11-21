/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.wallet;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

/** Google Pay is never available. */
public final class Wallet {
  private Wallet() {}

  @NonNull
  public static PaymentsClient getPaymentsClient(@NonNull Activity activity, @NonNull WalletOptions options) {
    return new PaymentsClient();
  }

  @NonNull
  public static PaymentsClient getPaymentsClient(@NonNull Context context, @NonNull WalletOptions options) {
    return new PaymentsClient();
  }

  public static final class WalletOptions {
    private WalletOptions() {}

    public static final class Builder {
      @NonNull
      public Builder setEnvironment(int environment) {
        return this;
      }

      @NonNull
      public Builder setTheme(int theme) {
        return this;
      }

      @NonNull
      public WalletOptions build() {
        return new WalletOptions();
      }
    }
  }
}
