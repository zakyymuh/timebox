// Every plugin is declared here, once, with its version and `apply false`; subprojects
// apply them by id with no version.
//
// This is not stylistic. A plugin requested in a subproject's own `plugins {}` block gets
// a child classloader scope that a parent scope cannot see into, which under AGP 8 left
// the Kotlin plugin unable to find AGP's own BaseExtension.
//
// Note there is no kotlin-android plugin here: AGP 9 compiles Kotlin itself, and applying
// org.jetbrains.kotlin.android alongside it is rejected outright. kotlin-jvm stays, for
// :core, which is a plain JVM library AGP knows nothing about.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}
