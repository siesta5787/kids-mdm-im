/*
 * Kids MDM IM: how new messages reach the phone.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.push

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.kidsmdm.im.KidsDefaults
import org.kidsmdm.im.KidsSettings
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.messages.IncomingMessageObserver

/**
 * Two ways for new messages to arrive, chosen by the parent (first run, then Parental controls):
 *  - [Method.WEBSOCKET]: Signal's own connection stays open in the background (Signal's normal
 *    behaviour without push). Works everywhere.
 *  - [Method.UNIFIEDPUSH]: the connection closes in the background; MollySocket on the Pi wakes
 *    the app through the launcher's UnifiedPush distributor when a message is waiting.
 *
 * Until UnifiedPush is actually working ([isPushActive]), the app keeps the websocket open, so a
 * half-finished setup never loses messages. One hook in Signal's
 * SettingsValues.shouldStayConnectedInBackground treats an active UnifiedPush like FCM.
 */
object KidsPush {
  private val TAG = Log.tag(KidsPush::class.java)

  enum class Method { WEBSOCKET, UNIFIEDPUSH }

  enum class Status {
    /** Websocket mode, or not set up. */
    OFF,

    /** Waiting for the distributor or the MollySocket server. */
    WAITING,

    /** No UnifiedPush distributor on the phone (the launcher's distributor is off). */
    NO_DISTRIBUTOR,

    /** Pushes are set up and the websocket closes in the background. */
    REGISTERED,

    /** The MollySocket server refused the registration or didn't answer like MollySocket. */
    SERVER_ERROR,

    /** The account already has the maximum number of linked devices. */
    DEVICE_LIMIT,

    /** The distributor refused the registration. */
    DISTRIBUTOR_FAILED
  }

  fun init(application: Application) {
    KidsPushState.init(application)
    application.registerActivityLifecycleCallbacks(FirstRunPrompt)
  }

  /** Hook: SettingsValues.shouldStayConnectedInBackground. Safe before init (false). */
  @JvmStatic
  fun isPushActive(): Boolean {
    return KidsSettings.pushMethod == Method.UNIFIEDPUSH && KidsPushState.status == Status.REGISTERED && KidsPushState.endpoint != null
  }

  val status: Status get() = KidsPushState.status

  val lastPushAt: Long get() = KidsPushState.lastPushAt

  /** Saves the parent's choice and applies it in the background. */
  fun setMethod(method: Method, mollySocketUrl: String?) {
    Log.i(TAG, "Delivery method set to $method")
    KidsSettings.mollySocketUrl = mollySocketUrl
    KidsSettings.pushMethod = method
    if (method == Method.UNIFIEDPUSH) {
      KidsPushState.status = Status.WAITING
    }
    // Like Molly's settings screen: a new server registration asks MollySocket for a test push.
    refresh(testPing = method == Method.UNIFIEDPUSH)
  }

  fun refresh(fromNewEndpoint: Boolean = false, testPing: Boolean = false) {
    AppDependencies.jobManager.add(KidsPushRefreshJob(fromNewEndpoint, testPing))
  }

  /** Called once per process, after Signal's Application.onCreate. */
  fun onAppStarted() {
    if (KidsSettings.pushMethod == Method.UNIFIEDPUSH && SignalStore.account.isRegistered) {
      refresh()
    }
  }

  /**
   * Restarts Signal's connection when UnifiedPush starts or stops working, so the background
   * websocket (and its notification) stops or starts. Same steps as Signal's own "stay connected"
   * setting (DataAndStorageSettingsRepository).
   */
  internal fun onPushActiveChanged(wasActive: Boolean) {
    val active = isPushActive()
    if (active == wasActive) return
    Log.i(TAG, if (active) "UnifiedPush is working; closing the background connection." else "UnifiedPush stopped; keeping the connection open.")
    SignalExecutors.BOUNDED.execute {
      if (!SignalStore.settings.shouldStayConnectedInBackground()) {
        IncomingMessageObserver.stopForegroundService(AppDependencies.application)
      }
      AppDependencies.resetNetwork()
      AppDependencies.startNetwork()
    }
  }

  /** Asks how messages should arrive the first time the main screen opens after registration. */
  private object FirstRunPrompt : Application.ActivityLifecycleCallbacks {
    private const val MAIN_ACTIVITY = "org.thoughtcrime.securesms.MainActivity"
    private var shownThisProcess = false

    override fun onActivityResumed(activity: Activity) {
      if (activity.javaClass.name != MAIN_ACTIVITY) return
      KidsDefaults.applyOnce()
      if (shownThisProcess) return
      if (KidsSettings.pushMethod != null || !SignalStore.account.isRegistered) return
      shownThisProcess = true
      activity.startActivity(PushChoiceActivity.intent(activity, firstRun = true))
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
  }
}
