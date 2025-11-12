// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

buildscript {
    dependencies {
        // Nếu cần, thêm plugin google-services ở đây (cũ)
        classpath("com.google.gms:google-services:4.4.4")
    }
}
