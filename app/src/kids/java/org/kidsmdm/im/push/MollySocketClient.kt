/*
 * Kids MDM IM: MollySocket client (ported from Molly's MollySocketRepository, AGPL-3.0).
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import org.signal.core.util.Base64
import org.signal.core.util.Util
import org.signal.core.util.crypto.DeviceNameCipher
import org.signal.core.util.logging.Log
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.util.KeyHelper
import org.signal.network.NetworkResult
import org.thoughtcrime.securesms.AppCapabilities
import org.thoughtcrime.securesms.crypto.PreKeyUtil
import org.thoughtcrime.securesms.crypto.storage.PreKeyMetadataStore
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.linkdevice.LinkDeviceRepository
import org.thoughtcrime.securesms.push.AccountManagerFactory
import org.whispersystems.signalservice.api.account.DeviceAttributes
import org.whispersystems.signalservice.api.account.PreKeyCollection
import java.io.IOException

/**
 * MollySocket (github.com/mollyim/mollysocket) watches the account through its own linked device
 * and posts a small "wake up" push to our UnifiedPush endpoint whenever a message is waiting. It
 * never sees message content: the linked device has no keys to decrypt anything.
 *
 * Only MollySocket's "webserver" mode is supported: the app registers itself by posting to the
 * server's address (on the Pi: `https://<pi>.<tailnet>.ts.net:8443`).
 */
internal object MollySocketClient {
  private val TAG = Log.tag(MollySocketClient::class.java)

  private const val DEVICE_NAME = "MollySocket"
  private const val HTTP_DEVICE_LIMIT = 411
  private val JSON = "application/json; charset=utf-8".toMediaType()

  data class Device(val id: Int, val password: String) {
    override fun toString(): String = "MollySocketDevice($id)"
  }

  enum class Result { OK, FORBIDDEN, INVALID_UUID, INVALID_ENDPOINT, INTERNAL_ERROR, BAD_RESPONSE }

  class DeviceLimitException : Exception("The account has the maximum number of linked devices")

  /** True if [url] answers like a MollySocket server. Throws [IOException] if it can't be reached. */
  @Throws(IOException::class)
  fun isMollySocketServer(url: HttpUrl): Boolean {
    val request = Request.Builder().url(url).build()
    AppDependencies.okHttpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return false
      return try {
        JSONObject(response.body.string()).has("mollysocket")
      } catch (e: JSONException) {
        false
      }
    }
  }

  /** Links a new "MollySocket" device to the account and returns its credentials. */
  @Throws(IOException::class, DeviceLimitException::class)
  fun createDevice(): Device {
    Log.i(TAG, "Linking a device for MollySocket")
    val password = Util.getSecret(18)
    val verificationCode = AppDependencies.linkDeviceApi.getDeviceVerificationCode().successOrThrow().verificationCode

    val aciIdentity = SignalStore.account.aciIdentityKey
    val attributes = DeviceAttributes(
      fetchesMessages = true,
      registrationId = KeyHelper.generateRegistrationId(false),
      pniRegistrationId = SignalStore.account.pniRegistrationId,
      name = Base64.encodeWithPadding(DeviceNameCipher.encryptDeviceName(DEVICE_NAME.toByteArray(), aciIdentity)),
      capabilities = AppCapabilities.getCapabilities(false)
    )

    val result = AccountManagerFactory.getInstance()
      .createUnauthenticated(AppDependencies.application, SignalStore.account.requireE164(), -1, password)
      .registrationApi
      .registerAsSecondaryDevice(
        verificationCode,
        attributes,
        preKeys(aciIdentity, SignalStore.account.aciPreKeys),
        preKeys(SignalStore.account.pniIdentityKey, SignalStore.account.pniPreKeys),
        null
      )

    val deviceId = when (result) {
      is NetworkResult.Success -> result.result.deviceId.toInt()
      is NetworkResult.StatusCodeError -> if (result.code == HTTP_DEVICE_LIMIT) throw DeviceLimitException() else throw result.exception
      is NetworkResult.NetworkError -> throw result.exception
      is NetworkResult.ApplicationError -> throw IOException(result.throwable)
    }

    SignalStore.account.isMultiDevice = true
    return Device(deviceId, password).also { Log.i(TAG, "Linked $it") }
  }

  /** The new device's own keys. Signed with our identity; MollySocket never uses them. */
  private fun preKeys(identity: IdentityKeyPair, metadata: PreKeyMetadataStore): PreKeyCollection {
    return PreKeyCollection(
      identity.publicKey,
      PreKeyUtil.generateSignedPreKey(metadata.nextSignedPreKeyId, identity.privateKey),
      PreKeyUtil.generateLastResortKyberPreKey(metadata.nextKyberPreKeyId, identity.privateKey)
    )
  }

  /** False if [device] is no longer linked (e.g. removed in Linked devices). True if linked or unknown (offline). */
  fun isProbablyLinked(device: Device): Boolean {
    val devices = try {
      runBlocking { LinkDeviceRepository.loadDevices() }
    } catch (e: Exception) {
      Log.w(TAG, "Couldn't load linked devices", e)
      null
    } ?: return true
    return devices.any { it.id == device.id }
  }

  /** Throws [IOException] on network errors (worth retrying). Other errors (e.g. already removed) are logged and ignored. */
  @Throws(IOException::class)
  fun removeDevice(device: Device) {
    val removed = try {
      runBlocking { LinkDeviceRepository.removeDevice(device.id) }
    } catch (e: IOException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Couldn't remove $device; assuming it's already gone", e)
      return
    }
    if (!removed) throw IOException("Couldn't remove $device")
  }

  /** Tells the MollySocket server at [url] to watch [device] and push to [endpoint]. */
  @Throws(IOException::class)
  fun register(url: HttpUrl, device: Device, endpoint: String, ping: Boolean): Result {
    val body = JSONObject()
      .put("uuid", SignalStore.account.requireAci().toString())
      .put("device_id", device.id)
      .put("password", device.password)
      .put("endpoint", endpoint)
      .put("ping", ping)
      .toString()
      .toRequestBody(JSON)

    val request = Request.Builder().url(url).post(body).build()
    AppDependencies.okHttpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        Log.w(TAG, "MollySocket answered HTTP ${response.code}")
        return Result.BAD_RESPONSE
      }
      val status = try {
        JSONObject(response.body.string()).getJSONObject("mollysocket").optString("status")
      } catch (e: JSONException) {
        return Result.BAD_RESPONSE
      }
      Log.i(TAG, "MollySocket registration status: $status")
      return when (status) {
        "ok" -> Result.OK
        "forbidden" -> Result.FORBIDDEN
        "invalid_uuid" -> Result.INVALID_UUID
        "invalid_endpoint" -> Result.INVALID_ENDPOINT
        "internal_error" -> Result.INTERNAL_ERROR
        else -> Result.BAD_RESPONSE
      }
    }
  }
}
