/*
 * Kids MDM IM FOSS stand-in for Signal's lib/billing (Google Play Billing): same API, always
 * returns Signal's own Empty implementations, as Signal does for builds without Play billing.
 * See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.billing

import android.content.Context
import org.signal.core.util.billing.BillingApi
import org.signal.core.util.billing.BillingDependencies
import org.signal.core.util.billing.OneTimePurchaseApi

object BillingFactory {
  @Suppress("UNUSED_PARAMETER")
  @JvmStatic
  fun create(billingDependencies: BillingDependencies, isBackupsAvailable: Boolean): BillingApi = BillingApi.Empty

  @Suppress("UNUSED_PARAMETER")
  fun createOneTimePurchaseApi(context: Context, isAvailable: Boolean): OneTimePurchaseApi = OneTimePurchaseApi.Empty
}
