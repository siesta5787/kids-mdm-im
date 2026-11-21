/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.firebase.messaging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

/** Never constructed: there is no FCM. */
public final class RemoteMessage {
  public static final int PRIORITY_UNKNOWN = 0;
  public static final int PRIORITY_HIGH = 1;
  public static final int PRIORITY_NORMAL = 2;

  private RemoteMessage() {}

  @NonNull
  public Map<String, String> getData() {
    return Collections.emptyMap();
  }

  @Nullable
  public String getMessageId() {
    return null;
  }

  public int getPriority() {
    return PRIORITY_UNKNOWN;
  }

  public int getOriginalPriority() {
    return PRIORITY_UNKNOWN;
  }

  public long getSentTime() {
    return 0;
  }
}
