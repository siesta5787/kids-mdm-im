/*
 * Kids MDM IM: app-wide parental PIN gate.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im.pin

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import org.signal.core.util.logging.Log
import java.lang.ref.WeakReference

/**
 * Puts every settings-type screen behind the parental PIN, whichever way it was opened: the
 * settings menu, a megaphone, a notification, a deep link or a "set up backups" prompt. This is
 * done by watching activity lifecycles, so it needs no hook in Signal's code, and new screens
 * Signal adds in these packages are locked automatically.
 *
 * After a correct PIN, locked screens stay open while the parent moves between them. The gate
 * re-locks [RELOCK_GRACE_MS] after the last locked screen is left, and immediately when the app
 * goes to the background.
 *
 * Signal's own settings split pane on tablets lives inside AppSettingsActivity, so it is covered too.
 */
object KidsPinGate : Application.ActivityLifecycleCallbacks {
  private val TAG = Log.tag(KidsPinGate::class.java)

  private const val RELOCK_GRACE_MS = 15_000L

  private const val S = "org.thoughtcrime.securesms."

  /** Every activity in these packages needs the PIN. */
  private val GATED_PACKAGES = listOf(
    "${S}components.settings.app.", // all of Settings (incl. donations, username, 2FA, change number)
    "${S}backup.", // backup sign-up and key verification
    "${S}payments.",
    "${S}logsubmit.", // debug log upload
    "${S}preferences.", // proxy
    "${S}devicetransfer.", // moving the account to another phone
    "${S}registration.olddevice.", // quick transfer to a new phone
    "${S}profiles.manage.", // profile name, username, about
    "${S}stories.settings.", // story privacy
    "${S}blocked.", // blocked users (no unblocking someone a parent blocked)
    "org.kidsmdm.im.settings."
  )

  /** Individual activities outside those packages that need the PIN. */
  private val GATED_ACTIVITIES = setOf(
    "${S}DeviceProvisioningActivity", // linking a new device (a desktop could chat outside the journal's view)
    "${S}lock.v2.CreateSvrPinActivity" // changing the Signal account PIN
  )

  private var unlocked = false
  private var startedGatedActivities = 0
  private var relockAt = 0L
  private var promptShowing = false
  private val waiting = mutableListOf<WeakReference<Activity>>()

  fun install(application: Application) {
    application.registerActivityLifecycleCallbacks(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(
      LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) {
          lock()
        }
      }
    )
  }

  fun isGated(activity: Activity): Boolean {
    val name = activity.javaClass.name
    return name in GATED_ACTIVITIES || GATED_PACKAGES.any { name.startsWith(it) }
  }

  private fun lock() {
    unlocked = false
    relockAt = 0
  }

  override fun onActivityStarted(activity: Activity) {
    if (!isGated(activity)) return

    if (startedGatedActivities == 0 && SystemClock.elapsedRealtime() >= relockAt) {
      unlocked = false
    }
    startedGatedActivities++

    if (!unlocked) {
      requirePin(activity)
    }
  }

  override fun onActivityStopped(activity: Activity) {
    if (!isGated(activity)) return

    startedGatedActivities = (startedGatedActivities - 1).coerceAtLeast(0)
    if (startedGatedActivities == 0) {
      relockAt = SystemClock.elapsedRealtime() + RELOCK_GRACE_MS
    }
  }

  private fun requirePin(activity: Activity) {
    waiting += WeakReference(activity)
    if (promptShowing) return

    promptShowing = true
    Log.i(TAG, "PIN required for ${activity.javaClass.simpleName}")
    activity.startActivity(if (KidsPin.isSet) ParentalPinGateActivity.intent(activity) else ParentalPinSetupActivity.intentForCreate(activity))
  }

  /** Called by the PIN screens after a correct PIN (or a newly created one). */
  fun onPinAccepted() {
    unlocked = true
    relockAt = SystemClock.elapsedRealtime() + RELOCK_GRACE_MS
    promptShowing = false
    waiting.clear()
  }

  /** Called by the PIN screens when the parent backs out: close what the PIN was protecting. */
  fun onPinCancelled() {
    promptShowing = false
    waiting.mapNotNull { it.get() }.forEach { it.finish() }
    waiting.clear()
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
  override fun onActivityResumed(activity: Activity) = Unit
  override fun onActivityPaused(activity: Activity) = Unit
  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
  override fun onActivityDestroyed(activity: Activity) = Unit
}
