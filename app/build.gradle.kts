plugins {
    alias(libs.plugins.android.application)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Load credentials as data only. Environment variables can override .env in CI.
val movieDbEnv = rootProject.file(".env").takeIf { it.isFile }
    ?.readLines()?.mapNotNull { line ->
        val entry = line.trim().removePrefix("export ")
        if (entry.startsWith("#") || !entry.contains("=")) null else {
            val (key, value) = entry.split("=", limit = 2)
            key.trim() to value.trim().removeSurrounding("\"").removeSurrounding("'")
        }
    }?.toMap().orEmpty()

fun movieDbCredential(name: String): String {
    val value = providers.environmentVariable(name).orNull ?: movieDbEnv[name].orEmpty()
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\""
}

android {
    namespace = "com.truongngo.moviedb"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.truongngo.moviedb"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MOVIEDB_ACCESS_TOKEN", movieDbCredential("MOVIEDB_ACCESS_TOKEN"))
        buildConfigField("String", "MOVIEDB_API_KEY", movieDbCredential("MOVIEDB_API_KEY"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                // AGP 9.3+: optimize code and remove unused resources with R8.
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.coil)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.androidx.hilt.nav.fragment)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
