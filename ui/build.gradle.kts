plugins {
    // No versions: declared once in the root project's plugins block (see the comment there).
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

// What every app shares: the Fresh theme, the Inter font, the soft cards and fields, the
// navigation guards and the daily alarm. Everything the apps use from here is `api`,
// so an app depending on :ui sees Compose, navigation and :core without restating them.
android {
    namespace = "dev.zakyymuh.timebox.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core"))

    api(libs.androidx.core.ktx)
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.material3)
    api(libs.androidx.navigation.compose)
    api(libs.androidx.lifecycle.runtime.ktx)
}
