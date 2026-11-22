/*
 * Kids MDM IM: UnifiedPush registration (from Molly's UnifiedPushRefreshJob, AGPL-3.0).
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.jobmanager.JsonJobData
import org.thoughtcrime.securesms.jobmanager.impl.NetworkConstraint
import org.thoughtcrime.securesms.jobs.BaseJob
import org.thoughtcrime.securesms.keyvalue.SignalStore
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Molly's UnifiedPushRefreshJob: handles UnifiedPush registration and keeps the MollySocket status
 * up to date; unregisters when the parent picks "Always connected". Differences from Molly:
 * settings come from [KidsSettings]/[KidsPushState] instead of SignalStore, no VAPID key is
 * required (we don't scan MollySocket's QR code; the launcher's distributor doesn't use VAPID),
 * only MollySocket's webserver mode, and status problems show in Parental controls instead of
 * notifications. Registered in Signal's JobManagerFactories under [KEY] (one-line hook).
 *
 * @param testPing Send a ping notification with the registration.
 * @param fromNewEndpoint Avoid registration cycle: if the Job is run from the receiver
 *   "onNewEndpoint", it will not register to the distributor.
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

  public override fun onRun() {
    if (!SignalStore.account.isRegistered || SignalStore.account.isLinkedDevice) {
      Log.w(TAG, "User is not registered. Skipping.")
      return
    }

    val wasActive = KidsPush.isPushActive()

    if (KidsSettings.pushMethod != KidsPush.Method.UNIFIEDPUSH) {
      Log.d(TAG, "UnifiedPush is disabled.")
      try {
        if (KidsPushState.endpoint != null) UnifiedPushDistributor.unregisterApp()
        cleanupMollySocketDevice()
        KidsPushState.clear()
      } finally {
        KidsPush.onPushActiveChanged(wasActive)
      }
      return
    }

    val currentStatus = KidsPushState.status
    Log.d(TAG, "Current registration status: $currentStatus")

    try {
      validateMollySocketDevice()

      val newStatus = checkRegistrationStatus()
      if (currentStatus == newStatus) {
        Log.d(TAG, "Registration status unchanged.")
      } else {
        Log.d(TAG, "Updated registration status: $newStatus")
        KidsPushState.status = newStatus
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Error checking registration status", t)

      if (currentStatus != KidsPush.Status.REGISTERED) {
        KidsPushState.status = KidsPush.Status.WAITING
      }

      // Re-throw the exception as IOException for retry
      when (t) {
        is IOException -> throw t
        else -> throw IOException(t)
      }
    } finally {
      KidsPush.onPushActiveChanged(wasActive)
    }
  }

  @Throws(IOException::class)
  private fun checkRegistrationStatus(): KidsPush.Status {
    val mollySocketUrl = KidsSettings.mollySocketUrl?.toHttpUrlOrNull()
    Log.d(TAG, "Last notification received at: ${KidsPushState.lastPushAt}")

    if (!fromNewEndpoint) {
      if (!UnifiedPushDistributor.selectDistributor()) {
        Log.e(TAG, "No UnifiedPush distributor. Is the launcher's distributor turned on?")
        return KidsPush.Status.NO_DISTRIBUTOR
      }
      UnifiedPushDistributor.registerApp(null)
    }

    val endpoint = KidsPushState.endpoint
    if (!UnifiedPushDistributor.checkIfActive() || endpoint == null) {
      Log.e(TAG, "Distributor is not active or endpoint is missing.")
      return KidsPush.Status.WAITING
    }

    if (mollySocketUrl == null) {
      Log.e(TAG, "Missing MollySocket URL.")
      return KidsPush.Status.SERVER_ERROR
    }

    val device = KidsPushState.device ?: try {
      MollySocketClient.createDevice().also { newDevice ->
        KidsPushState.device = newDevice
        Log.d(TAG, "Created new MollySocket device: $newDevice")
      }
    } catch (e: MollySocketClient.DeviceLimitException) {
      Log.e(TAG, "Device limit exceeded.")
      return KidsPush.Status.DEVICE_LIMIT
    }

    Log.d(TAG, "Re-registering $device on MollySocket server...")
    val result = MollySocketClient.register(url = mollySocketUrl, device = device, endpoint = endpoint, ping = testPing)
    Log.d(TAG, "Registration result: $result")

    return if (result == MollySocketClient.Result.OK) KidsPush.Status.REGISTERED else KidsPush.Status.SERVER_ERROR
  }

  private fun validateMollySocketDevice() {
    val device = KidsPushState.device ?: return
    if (!MollySocketClient.isProbablyLinked(device)) {
      Log.w(TAG, "$device no longer linked, will be recreated.")
      KidsPushState.device = null
    }
  }

  @Throws(IOException::class)
  private fun cleanupMollySocketDevice() {
    val device = KidsPushState.device ?: return
    Log.d(TAG, "Cleaning up $device...")
    MollySocketClient.removeDevice(device)
    KidsPushState.device = null
  }

  override fun onFailure() = Unit

  override fun onShouldRetry(e: Exception): Boolean = e is IOException

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putBoolean(KEY_FROM_NEW_ENDPOINT, fromNewEndpoint)
      .putBoolean(KEY_TEST_PING, testPing)
      .serialize()
  }

  override fun getFactoryKey(): String = KEY

  class Factory : Job.Factory<KidsPushRefreshJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): KidsPushRefreshJob {
      val data = JsonJobData.deserialize(serializedData)
      return KidsPushRefreshJob(parameters, data.getBoolean(KEY_FROM_NEW_ENDPOINT), data.getBoolean(KEY_TEST_PING))
    }
  }
}
