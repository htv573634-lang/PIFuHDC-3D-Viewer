plugins {
    id("com.android.application")
}

android {
    namespace = "com.pifuhdc.viewer"

    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.pifuhdc.viewer"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
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

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(
        "io.github.sceneview:sceneview:4.37.0"
    )
}
