package org.thoughtcrime.securesms.stories

import android.app.Application
import android.graphics.drawable.Drawable
import android.os.Looper.getMainLooper
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.signal.blurhash.BlurHash
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.visible

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StoryFirstTimeNavigationViewTest {
  private val testSubject =
    StoryFirstTimeNavigationView(ContextThemeWrapper(getApplicationContext(), R.style.Signal_DayNight))

  private val requestBuilder = mockk<RequestBuilder<Drawable>>(relaxed = true) {
    every { addListener(any()) } returns this@mockk
  }
  private val requestManager = mockk<RequestManager>(relaxUnitFun = true) {
    every { load(any<BlurHash>()) } returns requestBuilder
  }

  @Before
  fun setUp() {
    mockkStatic(Glide::class)
    every { Glide.with(any<View>()) } returns requestManager
  }

  @Test
  @Config(sdk = [34])
  fun `Given sdk 31, when I create testSubject, then I expect overlay visible and blur hash not visible`() {
    shadowOf(getMainLooper()).idle()

    assertTrue(testSubject.findViewById<View>(R.id.edu_overlay).visible)
    assertFalse(testSubject.findViewById<View>(R.id.edu_blur_hash).visible)
  }

  // KIDS MDM IM: dropped the sdk=30 variants of these tests (pre-31 blur-hash-preview
  // behavior) since minSdk is 34 here, so that code path can never run on this fork.

  @Test
  @Config(sdk = [34])
  fun `Given sdk 31 when I set blur hash, then blur has is visible`() {
    shadowOf(getMainLooper()).idle()

    testSubject.setBlurHash(BlurHash.parseOrNull("0000")!!)

    assertFalse(testSubject.findViewById<View>(R.id.edu_blur_hash).visible)
  }

  @Test
  @Config
  fun `When I hide then I expect not to be visible`() {
    testSubject.hide()

    assertFalse(testSubject.visible)
  }

  @Test
  @Config
  fun `Given I hide, when I show, then I expect to be visible`() {
    testSubject.hide()

    testSubject.show()

    assertTrue(testSubject.visible)
  }

  @Test
  @Config
  fun `Given I hide and user has seen overlay, when I show, then I expect to not be visible`() {
    testSubject.hide()
    testSubject.callback = object : StoryFirstTimeNavigationView.Callback {
      override fun userHasSeenFirstNavigationView(): Boolean = true
      override fun onGotItClicked() = error("Unused")
      override fun onCloseClicked() = error("Unused")
    }

    testSubject.show()

    assertFalse(testSubject.visible)
  }
}
