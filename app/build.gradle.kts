plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.cr4sh.astrolair"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cr4sh.astrolair"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // abilita desugaring per usare java.time su minSdk < 26
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Coil per immagini + GIF animate
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
}