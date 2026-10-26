plugins {
    // No versions: all declared with versions in the root project's plugins block, which
    // is what keeps them in a single classloader scope so KGP can see AGP's types.
    id("com.android.application")
    // No kotlin-android: AGP 9 compiles Kotlin itself and rejects that plugin.
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.zakyymuh.timebox"

    // 37 is not optional: androidx requires libraries depending on it to compile
    // against 37 or later, enforced by :app:checkDebugAarMetadata.
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.zakyymuh.timebox"
        // The target phone is a Redmi Note 8, which shipped on Android 9 (API 28) and
        // may never have been updated past it.
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}


dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
}
