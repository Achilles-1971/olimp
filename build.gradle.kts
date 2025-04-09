buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
}

plugins {
    // твои плагины через Version Catalog
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
