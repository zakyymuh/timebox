import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// :core holds every decision Timebox makes and must never depend on Android.
// Keep this dependency list free of anything with `android` in its coordinates.

// Bytecode is pinned to 17 rather than requested through a toolchain, so this builds
// on a JDK 17 dev machine and on CI's JDK 21 without provisioning a second JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    testLogging {
        events("passed", "failed", "skipped")
    }
}
