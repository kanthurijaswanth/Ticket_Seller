import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("kotlin-kapt")
  id("com.google.dagger.hilt.android")
}

android {
  namespace = "com.ticketseller.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.ticketseller.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  kotlin { jvmToolchain(17) }

  packaging {
    resources { excludes += "/META-INF/*" }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

kapt {
  correctErrorTypes = true
}

dependencies {
  val composeVersion = "1.6.2"

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.8.2")
  implementation("androidx.compose.ui:ui:$composeVersion")
  implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
  implementation("androidx.compose.material3:material3:1.2.1")
  implementation("androidx.navigation:navigation-compose:2.7.7")
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("com.google.android.material:material:1.11.0")

  // Hilt
  implementation("com.google.dagger:hilt-android:2.51")
  kapt("com.google.dagger:hilt-android-compiler:2.51")
  implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

  // Coroutines + serialization
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  // ML Kit returns Google Play Services Task — bridge with await()
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

  // Lifecycle ViewModel Compose + StateFlow
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

  // ── Supabase (replaces Firebase) ──────────────────────────────
  implementation(platform("io.github.jan-tennert.supabase:bom:2.5.4"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:gotrue-kt")
  implementation("io.github.jan-tennert.supabase:realtime-kt")
  implementation("io.github.jan-tennert.supabase:storage-kt")
  // HTTP client engine that the Supabase SDK uses
  implementation("io.ktor:ktor-client-okhttp:2.3.7")

  // ML Kit Text Recognition (on-device, FREE)
  implementation("com.google.mlkit:text-recognition:16.0.0")

  // Accompanist permissions
  implementation("com.google.accompanist:accompanist-permissions:0.32.0")

  // Material icons extended
  implementation("androidx.compose.material:material-icons-extended:1.6.2")

  // AdMob + UMP (independent of backend)
  implementation("com.google.android.gms:play-services-ads:23.0.0")
  implementation("com.google.android.ump:user-messaging-platform:2.2.0")

  // Debug tools
  debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}
