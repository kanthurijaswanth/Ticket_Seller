import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.compose") // Corrected ID
  id("kotlin-kapt")
  id("com.google.dagger.hilt.android")
  id("com.google.gms.google-services")
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

    // Kotlin DSL string literal
    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  // ✅ Align Java/Kotlin toolchains (fixes 17 vs 21 mismatches)
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

// ✅ ensure every Kotlin task (incl. KAPT stubs) targets JVM 17
tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

// ✅ Room’s processor sometimes needs sqlite native on Windows; feed it via kapt
kapt {
  correctErrorTypes = true
  // (No special javacOptions needed; toolchain above keeps it on 17)
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

  // Retrofit/OkHttp + Moshi
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

  // Room
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  kapt("androidx.room:room-compiler:2.6.1")

  // Sqlite driver for Room (Windows fix)
  kapt("org.xerial:sqlite-jdbc:3.45.1.0")

  // DataStore
  implementation("androidx.datastore:datastore-preferences:1.0.0")

  // Firebase (BOM)
  implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
  implementation("com.google.firebase:firebase-analytics")
  implementation("com.google.firebase:firebase-messaging")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-auth")

  // AdMob + UMP
  implementation("com.google.android.gms:play-services-ads:23.0.0")
  implementation("com.google.android.ump:user-messaging-platform:2.2.0")

  // ZXing
  implementation("com.google.zxing:core:3.5.2")

  // Debug tools
  debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}