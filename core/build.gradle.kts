plugins {
    alias(libs.plugins.kotlin.jvm)
}

// :core holds every decision Timebox makes and must never depend on Android.
// Keep this dependency list free of anything with `android` in its coordinates.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    testLogging {
        events("passed", "failed", "skipped")
    }
}
