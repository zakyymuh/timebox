// Every plugin is declared here, once, with its version and `apply false`.
//
// This is not stylistic. A plugin requested in a subproject's own `plugins {}` block
// gets a child classloader scope, and a parent scope cannot see a child's classes. With
// AGP requested down in :app, the Kotlin plugin — loaded up here — could not see
// com.android.build.gradle.BaseExtension even though AGP ships it, and the Android
// build died with a bare NoClassDefFoundError. Subprojects apply these by id, no version.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}
