pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "timebox"

// :core is the rule set — pure Kotlin, builds and tests anywhere.
include(":core")

// :app needs an Android SDK. Applying the Android plugin without one fails
// configuration for EVERY project in the build, which would make even :core
// untestable on a machine that has no SDK. So :app joins the build only where
// an SDK exists: CI runners (ANDROID_HOME is preset) and any dev machine with
// a local.properties pointing at one.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").exists()

if (hasAndroidSdk) {
    include(":app")
} else {
    logger.lifecycle("No Android SDK found — building :core only. See docs/BUILDING.md.")
}
