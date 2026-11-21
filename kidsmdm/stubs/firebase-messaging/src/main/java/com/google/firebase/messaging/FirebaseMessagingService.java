/*
 * Kids MDM IM FOSS stand-in for a Google library: only the API Signal uses, and it always
 * behaves as if Google Play services are missing. See kidsmdm/settings.gradle.kts.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.google.firebase.messaging;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Base class for Signal's FcmReceiveService. Nothing ever starts it: there is no FCM. */
public abstract class FirebaseMessagingService extends Service {
  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
    stopSelf(startId);
    return START_NOT_STICKY;
  }

  public void onMessageReceived(@NonNull RemoteMessage message) {}

  public void onDeletedMessages() {}

  public void onMessageSent(@NonNull String msgId) {}

  public void onSendError(@NonNull String msgId, @NonNull Exception exception) {}

  public void onNewToken(@NonNull String token) {}
}
