plugins {
    id("com.android.application")
}

android {
    namespace = "com.pifuhdc.viewer"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.pifuhdc.viewer"

        minSdk = 26
        targetSdk = 35

        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }

        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.version"
            )
        }
    }
}

dependencies {
    implementation(
        "com.google.android.filament:filament-android:1.76.1"
    )

    implementation(
        "com.google.android.filament:gltfio-android:1.76.1"
    )

    implementation(
        "com.google.android.filament:filament-utils-android:1.76.1"
    )
}
