/*
 * Kids MDM IM build customizations, applied to :Signal-Android via `id("kidsmdm-app")`.
 *
 * Everything Kids-MDM-specific about the build lives here (plus the `kids` flavor source set
 * at app/src/kids/) so that Signal's own build.gradle.kts stays untouched except for the one
 * `id("kidsmdm-app")` line, keeping upstream merges conflict-free.
 *
 * DSL changes are made in `finalizeDsl`, which runs after Signal's own `android { }` block, so
 * our values win without editing Signal's.
 */

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

object KidsMdm {
  const val FLAVOR = "kids"
  const val APPLICATION_ID = "com.kidsmdm.im"

  /**
   * Launcher label, shown under the app icon. Signal's in-app wording ("Signal") is kept on
   * purpose; only the label changes. (A "Signal" -> "Messenger" rewrite was tried in Phase 1 and
   * dropped: the fork is privately used, so there's no trademark reason to rebrand the text.)
   */
  const val APP_LABEL = "Kids MDM IM"

  const val MIN_SDK = 34
  val ABIS = listOf("arm64-v8a")

  /**
   * Release tags look like v0.07 / v1.00. versionCode is derived from the tag so no commit is
   * needed per release; the 200_000 base keeps it above every Molly-based build (last: 171_915).
   * versionName deliberately stays Signal's own X.Y.Z: Signal's servers read it from the
   * user agent and RemoteDeprecation parses it strictly as semver.
   */
  const val VERSION_CODE_BASE = 200_000
}

val kidsVersion: String? = providers.gradleProperty("kidsVersion").orNull?.removePrefix("v")?.takeIf { it.isNotBlank() }

fun kidsVersionCode(version: String?): Int {
  if (version == null) return KidsMdm.VERSION_CODE_BASE
  val match = Regex("""^(\d+)\.(\d{2})$""").matchEntire(version)
    ?: throw GradleException("kidsVersion must look like 0.07 or 1.00 (got '$version')")
  val (major, minor) = match.destructured
  return KidsMdm.VERSION_CODE_BASE + major.toInt() * 100 + minor.toInt()
}

val kidsVersionCodeValue = kidsVersionCode(kidsVersion)

val androidComponents = extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

androidComponents.finalizeDsl { android: ApplicationExtension ->
  val keystorePath = System.getenv("CI_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
  val kidsSigning = keystorePath?.let { path ->
    android.signingConfigs.create("kids") {
      storeFile = file(path)
      storePassword = System.getenv("CI_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("CI_KEYSTORE_ALIAS")
      keyPassword = System.getenv("CI_KEYSTORE_PASSWORD")
      enableV4Signing = false
    }
  }

  android.productFlavors.create(KidsMdm.FLAVOR) {
    dimension = "distribution"
    buildConfigField("boolean", "MANAGES_APP_UPDATES", "false")
    buildConfigField("String", "APK_UPDATE_MANIFEST_URL", "null")
    buildConfigField("String", "BUILD_DISTRIBUTION_TYPE", "\"kids\"")
    buildConfigField("String", "KIDS_VERSION", "\"${kidsVersion ?: "dev"}\"")
    if (kidsSigning != null) {
      signingConfig = kidsSigning
    }
  }

  // Single arm64 APK: the phones are all arm64, and kid-phone-server installs the first .apk
  // asset of a release, so there must be exactly one.
  android.defaultConfig.ndk.abiFilters.clear()
  android.defaultConfig.ndk.abiFilters.addAll(KidsMdm.ABIS)
  android.splits.abi.isEnable = false

  // Registered here (after Signal's own beforeVariants) so it runs last and re-enables our
  // variants, which Signal's hard-coded selectableVariants list would otherwise disable.
  androidComponents.beforeVariants { variant ->
    if (variant.productFlavors.contains("distribution" to KidsMdm.FLAVOR) && variant.productFlavors.contains("environment" to "prod")) {
      variant.enable = variant.buildType in setOf("debug", "release")
      if (variant.enable) {
        (variant as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = true
      }
      // Only the shipped release APK gets our higher minSdk. Debug keeps Signal's, because the
      // unit tests run on it and Signal's Robolectric config pins SDK 28, which can't run an
      // app whose minSdk is 34 (the Molly-era fork had to edit Signal's test files for this).
      if (variant.buildType == "release") {
        variant.minSdk = KidsMdm.MIN_SDK
      }
    }
  }
}

val appLabelTask = tasks.register<KidsAppLabelTask>("generateKidsAppLabel") {
  appLabel.set(KidsMdm.APP_LABEL)
  outputDir.set(layout.buildDirectory.dir("generated/kidsmdm/app-label"))
}

androidComponents.onVariants(androidComponents.selector().withFlavor("distribution" to KidsMdm.FLAVOR)) { variant ->
  // Kids identity (package + launcher label) on release only. kidsProdDebug is the variant
  // Signal's unit tests run on, and those tests hard-code Signal's identity (UriUtilTest expects
  // org.thoughtcrime.securesms URIs to count as internal). Keeping debug as Signal lets every test
  // pass unmodified. Debug builds never ship.
  if (variant.buildType == "release") {
    variant.applicationId.set(KidsMdm.APPLICATION_ID)
    variant.sources.res?.addGeneratedSourceDirectory(appLabelTask, KidsAppLabelTask::outputDir)
  }

  // Compress native libs like Signal does for its self-distributed APKs (website/github).
  variant.packaging.jniLibs.useLegacyPackaging.set(true)

  variant.outputs.forEach { output ->
    output.versionCode.set(kidsVersionCodeValue)
  }
}

/**
 * Generates a resource override for Signal's app_name (the launcher label). A generated directory
 * is used rather than a file in app/src/kids/res so it applies to release variants only.
 */
@CacheableTask
abstract class KidsAppLabelTask : DefaultTask() {
  @get:Input
  abstract val appLabel: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val out = outputDir.get().asFile
    out.deleteRecursively()
    val values = File(out, "values").apply { mkdirs() }
    val label = appLabel.get().replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'")
    File(values, "kidsmdm_app_label.xml").writeText(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<resources xmlns:tools="http://schemas.android.com/tools">
      |    <string name="app_name" translatable="false" tools:ignore="UnusedResources">$label</string>
      |</resources>
      |""".trimMargin()
    )
  }
}
