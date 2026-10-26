import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP carries its version here: it is not otherwise on the build classpath, and
    // naming it only in this file keeps it unresolved on machines with no SDK, where
    // :app is not part of the build at all.
    alias(libs.plugins.android.application)

    // No version on purpose. The Kotlin Gradle plugin is already on the build classpath
    // from the root project's `kotlin.jvm apply false`, and re-requesting it with a
    // version fails with "already on the classpath with an unknown version".
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.zakyymuh.timebox"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.zakyymuh.timebox"
        // Redmi Note 8 shipped on Android 9 (API 28) and may never have been
        // updated past it, so the floor goes below 29. Nothing in v1 needs more.
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-spike"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
}
