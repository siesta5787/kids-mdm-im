// Kids MDM IM: hook points that Signal's library modules call with one line each, implemented
// by the app's kids source set at startup. See kidsmdm/settings.gradle.kts for how the
// dependency is added to Signal's modules without editing their build files.
plugins {
  id("signal-library")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "org.kidsmdm.hooks"

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
}
