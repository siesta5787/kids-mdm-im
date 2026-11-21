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
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object KidsMdm {
  const val FLAVOR = "kids"
  const val APPLICATION_ID = "com.kidsmdm.im"

  /** Launcher label, shown under the app icon. */
  const val APP_LABEL = "Kids MDM IM"

  /** Replaces "Signal" in user-visible text. */
  const val BRAND = "Messenger"

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

val brandingTask = tasks.register<KidsBrandingTask>("generateKidsBrandingResources") {
  val root = rootProject.layout.projectDirectory
  // App first so its strings win if a library defines the same name.
  val resDirs = listOf(root.dir("app/src/main/res")) +
    listOf("core", "feature", "lib").flatMap { group ->
      root.dir(group).asFile.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.map { root.dir("$group/${it.name}/src/main/res") }
    }
  resDirs.forEach { dir -> sourceFiles.from(fileTree(dir) { include("values*/strings.xml") }) }
  resRoots.set(resDirs.map { it.asFile.absolutePath })
  brand.set(KidsMdm.BRAND)
  appLabel.set(KidsMdm.APP_LABEL)
  outputDir.set(layout.buildDirectory.dir("generated/kidsmdm/branding"))
}

androidComponents.onVariants(androidComponents.selector().withFlavor("distribution" to KidsMdm.FLAVOR)) { variant ->
  // Kids identity (package + branding) on release only. kidsProdDebug is the variant Signal's
  // unit tests run on, and those tests hard-code Signal's identity (UriUtilTest expects
  // org.thoughtcrime.securesms URIs to count as internal; settings tests look for "Signal Secure
  // Backups"). Keeping debug as Signal lets every test pass unmodified. Debug builds never ship.
  if (variant.buildType == "release") {
    variant.applicationId.set(KidsMdm.APPLICATION_ID)
    variant.sources.res?.addGeneratedSourceDirectory(brandingTask, KidsBrandingTask::outputDir)
  }

  // Compress native libs like Signal does for its self-distributed APKs (website/github).
  variant.packaging.jniLibs.useLegacyPackaging.set(true)

  variant.outputs.forEach { output ->
    output.versionCode.set(kidsVersionCodeValue)
  }
}

/**
 * Generates resource overrides that rebrand Signal's user-visible strings, so the strings in
 * Signal's own resource files never have to be edited (which would conflict on every upstream
 * translation update). Output is one kidsmdm_branding.xml per values-qualifier directory.
 */
@CacheableTask
abstract class KidsBrandingTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceFiles: ConfigurableFileCollection

  @get:Input
  abstract val resRoots: ListProperty<String>

  @get:Input
  abstract val brand: Property<String>

  @get:Input
  abstract val appLabel: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  private companion object {
    /** "Signal" as a word, except where it names the company/foundation or another product. */
    val BRAND_WORD = Regex("""\bSignal\b(?!\s+(Messenger|Technology|Foundation|Protocol|Login)\b)""")

    /** Strings where "Signal" means the real network/company (inviting people, donations, legal). */
    val KEEP_ORIGINAL = Regex("(?i)invite|donat|receipt|sustainer|badge|boost|gift|license|legal|terms|privacy_policy|copyright|tax")

    const val TOOLS_NS = "http://schemas.android.com/tools"
  }

  @TaskAction
  fun generate() {
    val out = outputDir.get().asFile
    out.deleteRecursively()
    out.mkdirs()

    val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
      setOutputProperty(OutputKeys.INDENT, "yes")
      setOutputProperty(OutputKeys.ENCODING, "utf-8")
    }

    // values-qualifier -> (resource name -> element); first module to define a name wins.
    val byQualifier = sortedMapOf<String, LinkedHashMap<String, Element>>()

    for (root in resRoots.get()) {
      val rootDir = File(root)
      val valueDirs = rootDir.listFiles { f -> f.isDirectory && f.name.startsWith("values") }?.sortedBy { it.name } ?: continue
      for (dir in valueDirs) {
        val file = File(dir, "strings.xml")
        if (!file.exists()) continue
        val doc = factory.newDocumentBuilder().parse(file)
        val target = byQualifier.getOrPut(dir.name) { LinkedHashMap() }
        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
          val el = children.item(i) as? Element ?: continue
          if (el.tagName != "string" && el.tagName != "plurals") continue
          val name = el.getAttribute("name")
          if (name.isEmpty() || target.containsKey(name)) continue

          if (name == "app_name") {
            target[name] = el
            continue
          }
          if (KEEP_ORIGINAL.containsMatchIn(name)) continue
          if (BRAND_WORD.containsMatchIn(el.textContent)) {
            target[name] = el
          }
        }
      }
    }

    for ((qualifier, elements) in byQualifier) {
      if (elements.isEmpty()) continue
      val doc = factory.newDocumentBuilder().newDocument()
      val resources = doc.createElement("resources")
      resources.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tools", TOOLS_NS)
      resources.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xliff", "urn:oasis:names:tc:xliff:document:1.2")
      doc.appendChild(resources)

      for ((name, original) in elements) {
        val copy = doc.importNode(original, true) as Element
        if (name == "app_name") {
          copy.textContent = appLabel.get()
        } else {
          rebrand(copy)
        }
        // Overriding a resource defined in another module would otherwise trip lint/aapt warnings.
        copy.setAttributeNS(TOOLS_NS, "tools:ignore", "UnusedResources,MissingTranslation")
        resources.appendChild(copy)
      }

      val dir = File(out, qualifier).apply { mkdirs() }
      transformer.transform(DOMSource(doc), StreamResult(File(dir, "kidsmdm_branding.xml")))
    }
  }

  private fun rebrand(node: Node) {
    if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
      node.nodeValue = BRAND_WORD.replace(node.nodeValue, brand.get())
      return
    }
    // Don't touch placeholder examples inside <xliff:g>.
    if (node is Element && node.localName == "g") return
    val children = node.childNodes
    for (i in 0 until children.length) rebrand(children.item(i))
  }
}
