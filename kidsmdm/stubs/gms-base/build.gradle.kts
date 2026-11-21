// Kids MDM IM: FOSS stand-in, see kidsmdm/settings.gradle.kts
plugins {
  id("signal-library")
}

android {
  namespace = "org.kidsmdm.stubs.gms.base"
}

dependencies {
  api(project(":kidsmdm-stubs:gms-tasks"))
}
