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
    // :core and the Android framework, nothing else. The spike has no androidx
    // dependency at all — not one of its sources imports androidx — and adding
    // core-ktx cost two CI runs: core-ktx 1.19.1 demands compileSdk 37 and AGP 9.1+,
    // which it drags in through the AAR metadata check.
    //
    // When Tasks 2+ add Compose, Room and Glance this has to be faced properly:
    // that means AGP 9.1+, compileSdk 37, and Gradle 9.6+ (AGP 9 does not have AGP 8's
    // InternalProblems restriction). The classloader scoping that made AGP 9 look
    // broken earlier is fixed, so that route should now be open.
    implementation(project(":core"))
}
