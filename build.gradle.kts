// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}