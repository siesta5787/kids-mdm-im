/*
 * Kids MDM IM: sets up or tears down UnifiedPush (based on Molly's UnifiedPushRefreshJob, AGPL-3.0).
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.Util
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.jobmanager.JsonJobData
import org.thoughtcrime.securesms.jobmanager.impl.NetworkConstraint
import org.thoughtcrime.securesms.jobs.BaseJob
import org.thoughtcrime.securesms.keyvalue.SignalStore
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Brings the UnifiedPush registration in line with [KidsSettings.pushMethod]:
 *  1. register with the distributor (only when there's no endpoint yet: the launcher hands out
 *     a new endpoint on every registration, and an endpoint MollySocket doesn't know loses pushes);
 *  2. make sure MollySocket's linked device exists;
 *  3. register device + endpoint with the MollySocket server.
 * In websocket mode it unregisters and unlinks MollySocket's device instead.
 *
 * Registered in Signal's JobManagerFactories under [KEY] (one-line hook).
 */
class KidsPushRefreshJob private constructor(
  parameters: Parameters,
  private val fromNewEndpoint: Boolean,
  private val testPing: Boolean
) : BaseJob(parameters) {

  constructor(fromNewEndpoint: Boolean, testPing: Boolean) : this(
    Parameters.Builder()
      .setQueue(KEY)
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(3)
      .setLifespan(TimeUnit.HOURS.toMillis(6))
      .setMaxInstancesForFactory(2)
      .build(),
    fromNewEndpoint,
    testPing
  )

  companion object {
    private val TAG = Log.tag(KidsPushRefreshJob::class.java)

    const val KEY = "KidsPushRefreshJob"
    private const val KEY_FROM_NEW_ENDPOINT = "from_new_endpoint"
    private const val KEY_TEST_PING = "test_ping"
  }

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putBoolean(KEY_FROM_NEW_ENDPOINT, fromNewEndpoint)
      .putBoolean(KEY_TEST_PING, testPing)
      .serialize()
  }

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onShouldRetry(e: Exception): Boolean = e is IOException

  public override fun onRun() {
    if (!SignalStore.account.isRegistered || SignalStore.account.isLinkedDevice) return

    val wasActive = KidsPush.isPushActive()
    try {
      if (KidsSettings.pushMethod == KidsPush.Method.UNIFIEDPUSH) setUp() else tearDown()
    } finally {
      KidsPush.onPushActiveChanged(wasActive)
    }
  }

  private fun setUp() {
    val distributor = UnifiedPushClient.pickDistributor(context)
    if (distributor == null) {
      Log.w(TAG, "No UnifiedPush distributor. Is the launcher's distributor turned on?")
      KidsPushState.status = KidsPush.Status.NO_DISTRIBUTOR
      return
    }

    if (KidsPushState.token == null || KidsPushState.distributor != distributor) {
      KidsPushState.token = Util.getSecret(24)
      KidsPushState.distributor = distributor
      KidsPushState.endpoint = null
    }

    val endpoint = KidsPushState.endpoint
    if (endpoint == null) {
      if (!fromNewEndpoint) UnifiedPushClient.register(context, distributor, KidsPushState.token!!)
      if (KidsPushState.status != KidsPush.Status.REGISTERED) KidsPushState.status = KidsPush.Status.WAITING
      Log.i(TAG, "Waiting for an endpoint from $distributor")
      return
    }

    val url = KidsSettings.mollySocketUrl?.toHttpUrlOrNull()
    if (url == null) {
      Log.w(TAG, "No MollySocket server address")
      KidsPushState.status = KidsPush.Status.SERVER_ERROR
      return
    }

    var device = KidsPushState.device
    if (device != null && !MollySocketClient.isProbablyLinked(device)) {
      Log.w(TAG, "$device is no longer linked; linking a new one.")
      KidsPushState.device = null
      device = null
    }
    if (device == null) {
      device = try {
        MollySocketClient.createDevice()
      } catch (e: MollySocketClient.DeviceLimitException) {
        Log.w(TAG, "Device limit reached")
        KidsPushState.status = KidsPush.Status.DEVICE_LIMIT
        return
      }
      KidsPushState.device = device
    }

    val result = MollySocketClient.register(url, device, endpoint, ping = testPing)
    KidsPushState.status = if (result == MollySocketClient.Result.OK) KidsPush.Status.REGISTERED else KidsPush.Status.SERVER_ERROR
  }

  private fun tearDown() {
    val token = KidsPushState.token
    val distributor = KidsPushState.distributor
    if (token != null && distributor != null) {
      UnifiedPushClient.unregister(context, distributor, token)
    }
    KidsPushState.device?.let { device ->
      MollySocketClient.removeDevice(device)
      Log.i(TAG, "Unlinked $device")
    }
    KidsPushState.clear()
  }

  class Factory : Job.Factory<KidsPushRefreshJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): KidsPushRefreshJob {
      val data = JsonJobData.deserialize(serializedData)
      return KidsPushRefreshJob(parameters, data.getBoolean(KEY_FROM_NEW_ENDPOINT), data.getBoolean(KEY_TEST_PING))
    }
  }
}
