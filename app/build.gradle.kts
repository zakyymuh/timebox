import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // No versions: both are declared with versions in the root project's plugins block,
    // which is what keeps them in a single classloader scope so KGP can see AGP's types.
    id("com.android.application")
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
