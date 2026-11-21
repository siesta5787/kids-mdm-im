/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.android.gms.auth.api.identity;

import androidx.annotation.NonNull;

public class GetPhoneNumberHintIntentRequest {
  private GetPhoneNumberHintIntentRequest() {}

  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    Builder() {}

    @NonNull
    public GetPhoneNumberHintIntentRequest build() {
      return new GetPhoneNumberHintIntentRequest();
    }
  }
}
