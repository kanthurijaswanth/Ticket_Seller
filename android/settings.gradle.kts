pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
  plugins {
    id("com.android.application") version "8.6.0"
    id("org.jetbrains.kotlin.android") version "2.0.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.10"
    id("org.jetbrains.kotlin.kapt") version "2.0.10"
    id("com.google.dagger.hilt.android") version "2.51.1"
    id("com.google.gms.google-services") version "4.4.2"
    // NOTE: we are NOT using KSP now; if you add it later, also declare its version here
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "TicketSeller"
include(":app")
