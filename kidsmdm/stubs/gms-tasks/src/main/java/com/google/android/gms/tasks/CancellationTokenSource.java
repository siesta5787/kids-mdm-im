/*
 * Kids MDM IM addition to the microG-derived Tasks implementation (see kidsmdm/stubs/README.md).
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tasks;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.microg.gms.tasks.CancellationTokenImpl;

/** Used by kotlinx-coroutines-play-services' Deferred.asTask(). */
@Keep
public class CancellationTokenSource {
  private final CancellationTokenImpl token = new CancellationTokenImpl();

  @NonNull
  public CancellationToken getToken() {
    return token;
  }

  public void cancel() {
    token.cancel();
  }
}
