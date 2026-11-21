// Kids MDM IM: FOSS stand-in, see kidsmdm/settings.gradle.kts
plugins {
  id("signal-library")
}

android {
  namespace = "org.signal.billing"
}

dependencies {
  implementation(project(":core:util"))
}
