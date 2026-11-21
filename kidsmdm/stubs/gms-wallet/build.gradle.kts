// Kids MDM IM: FOSS stand-in, see kidsmdm/settings.gradle.kts
plugins {
  id("signal-library")
}

android {
  namespace = "org.kidsmdm.stubs.gms.wallet"
}

dependencies {
  api(project(":kidsmdm-stubs:gms-base"))
}
