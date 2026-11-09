plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace   = "com.example.workmanagerdemo"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.example.workmanagerdemo"
        minSdk          = 24
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── AndroidX Core ────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Jetpack Compose BOM (Bill of Materials) ───────────────────────────
    // A BOM ensures all Compose library versions are compatible with each other.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── Compose UI ────────────────────────────────────────────────────────
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ── Lifecycle / ViewModel ─────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── WorkManager KTX ──────────────────────────────────────────────────
    // Core WorkManager library with Kotlin extensions and coroutine support.
    // Provides CoroutineWorker, Flow-based observation, and WorkRequest builders.
    implementation(libs.androidx.work.runtime.ktx)

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Hilt Dependency Injection ──────────────────────────────────────────
    // Used in Phase 5 (Custom WorkerFactory + @HiltWorker) but set up here
    // so the project compiles cleanly from day one.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    // ── Debug / Tooling ───────────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── Unit Testing ──────────────────────────────────────────────────────
    testImplementation(libs.junit)

    // ── WorkManager Testing ───────────────────────────────────────────────
    // Provides WorkManagerTestInitHelper and TestDriver for synchronous
    // constraint simulation in both unit and instrumented tests (Phase 6).
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.work.testing)

    // ── Instrumented / UI Testing ─────────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
