plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room3)
}

android {
    namespace = "com.cookbookanything.app"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }

    defaultConfig {
        applicationId = "com.cookbookanything.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // Shared, non-secret debug key so every debug build (CI or local) can update the same install
        // without wiping the user's recipes. Never use it for a release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // shortcut: no code shrinking yet, turn on optimization with keep rules before the Play release
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room3 {
    // Committed so future schema changes can ship migrations instead of losing recipes.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.room3.compiler)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
