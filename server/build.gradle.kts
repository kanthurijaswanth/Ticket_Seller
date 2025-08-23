plugins {
  kotlin("jvm") version "2.0.0"
  kotlin("plugin.serialization") version "2.0.0"   // ← needed for @Serializable
  application
}

repositories { mavenCentral() }

dependencies {
  implementation("io.ktor:ktor-server-netty:3.0.0")
  implementation("io.ktor:ktor-server-content-negotiation:3.0.0")
  implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

  // (Optional) logging and nice 500s; you can add later if you like.
  // implementation("io.ktor:ktor-server-call-logging:3.0.0")
  // implementation("io.ktor:ktor-server-status-pages:3.0.0")

  implementation("ch.qos.logback:logback-classic:1.5.8")
}

application {
  mainClass.set("com.ticketseller.server.ApplicationKt")
}

kotlin { jvmToolchain(17) }
