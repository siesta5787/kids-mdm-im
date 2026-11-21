/*
 * Kids MDM IM: startup hook.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.kidsmdm.im

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Runs [KidsMdm.init] at process start without touching Signal's Application class: Android creates
 * manifest providers before Application.onCreate. Not exported, and it serves no data.
 */
class KidsInitProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    KidsMdm.init(context!!.applicationContext as Application)
    return true
  }

  override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
  override fun getType(uri: Uri): String? = null
  override fun insert(uri: Uri, values: ContentValues?): Uri? = null
  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
