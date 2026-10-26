// The Android plugins are deliberately NOT declared here. Plugin resolution happens
// per-project, so naming them at the root would make Gradle resolve them even on a
// machine with no Android SDK, where :app is not part of the build at all.
// app/build.gradle.kts declares its own.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
