plugins {
    id("com.android.application")
    // REMOVED: id("org.jetbrains.kotlin.android") 
    // AGP now handles Kotlin automatically for app modules.
}

android {
    namespace = "com.pifuhdc.viewer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pifuhdc.viewer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Ensure this matches your Kotlin version
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // SceneView (includes Filament rendering engine)
    implementation("io.github.sceneview:sceneview:4.37.0")
    
    // Jetpack Compose & Core dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
}
