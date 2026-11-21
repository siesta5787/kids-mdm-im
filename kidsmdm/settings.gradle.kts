/*
 * Kids MDM IM: replace every Google library with a small FOSS stand-in.
 *
 * Applied from the last line of Signal's settings.gradle.kts. Kids MDM IM uses no Google services
 * at all (no FCM; push is UnifiedPush or Signal's own websocket), so each Google artifact is
 * substituted with a stub module under kidsmdm/stubs/ that exposes only the API Signal calls and
 * reports "Google Play services missing" / fails every request. Signal's code then takes the
 * same paths it takes on a phone without Google Play.
 *
 * The substitution applies to *every* project and configuration, so Signal's modules also compile
 * against the stubs: a Google API that Signal starts using is a compile error here, not a runtime
 * crash on the phone.
 *
 * Signal's lib/billing (Google Play Billing) is swapped as a whole project for a stub BillingFactory.
 */

val kidsStubs = listOf(
  "gms-tasks",
  "gms-base",
  "gms-auth",
  "gms-maps",
  "gms-wallet",
  "play-feature-delivery",
  "firebase-messaging",
  "billing",
  "empty"
)

kidsStubs.forEach { name ->
  include(":kidsmdm-stubs:$name")
  project(":kidsmdm-stubs:$name").projectDir = settings.rootDir.resolve("kidsmdm/stubs/$name")
}
project(":kidsmdm-stubs").projectDir = settings.rootDir.resolve("kidsmdm/stubs")

// Hook points that Signal's modules call (kidsmdm/hooks). The dependency is added here, so
// Signal's own build files stay untouched.
include(":kidsmdm-hooks")
project(":kidsmdm-hooks").projectDir = settings.rootDir.resolve("kidsmdm/hooks")
registerKidsHooksDependency(gradle, setOf(":Signal-Android", ":feature:app-settings", ":lib:remote-config"))

/** Google artifact (group:name, any version) to the stub project that replaces it. */
val kidsSubstitutions = mapOf(
  "com.google.android.gms:play-services-tasks" to "gms-tasks",
  "com.google.android.gms:play-services-base" to "gms-base",
  "com.google.android.gms:play-services-basement" to "gms-base",
  "com.google.android.gms:play-services-auth" to "gms-auth",
  "com.google.android.gms:play-services-auth-api-phone" to "gms-auth",
  "com.google.android.gms:play-services-maps" to "gms-maps",
  "com.google.android.gms:play-services-wallet" to "gms-wallet",
  "com.google.android.play:feature-delivery" to "play-feature-delivery",
  "com.google.android.play:core-common" to "play-feature-delivery",
  "com.google.firebase:firebase-messaging" to "firebase-messaging",
  // Only needed for Credential Manager on Android 13 and older; release builds are minSdk 34.
  "androidx.credentials:credentials-play-services-auth" to "empty",
  // Sign in with Google (unused by Signal's code, pulled in for the credentials library).
  "com.google.android.libraries.identity.googleid:googleid" to "empty"
)

registerKidsSubstitutions(gradle, kidsSubstitutions.toMap())

// A plain function so the lifecycle action captures only its arguments, not this script
// (the configuration cache can't serialize script object references).
fun registerKidsSubstitutions(gradle: Gradle, substitutions: Map<String, String>) {
  gradle.lifecycle.beforeProject {
    if (path.startsWith(":kidsmdm-")) return@beforeProject

    configurations.configureEach {
      resolutionStrategy.dependencySubstitution {
        substitutions.forEach { (artifact, stub) ->
          substitute(module(artifact)).using(project(":kidsmdm-stubs:$stub")).because("Kids MDM IM ships no Google libraries")
        }
        // Google Play Billing; the stub BillingFactory returns Signal's own Empty implementations.
        substitute(project(":lib:billing")).using(project(":kidsmdm-stubs:billing")).because("Kids MDM IM ships no Google libraries")
      }
    }
  }
}

/** Adds `implementation(project(":kidsmdm-hooks"))` to the given Signal projects. */
fun registerKidsHooksDependency(gradle: Gradle, projectPaths: Set<String>) {
  gradle.lifecycle.beforeProject {
    if (path !in projectPaths) return@beforeProject
    val hooks = project(":kidsmdm-hooks")
    listOf("com.android.application", "com.android.library").forEach { pluginId ->
      pluginManager.withPlugin(pluginId) {
        dependencies.add("implementation", hooks)
      }
    }
  }
}
