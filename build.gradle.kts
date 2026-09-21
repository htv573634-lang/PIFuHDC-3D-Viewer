plugins {
    // Android Application plugin (keep your version, e.g., 8.3.3 or 9.x.x)
    id("com.android.application") version "8.3.3" apply false 
    
    // Kotlin plugin declared here with 'apply false' makes it available globally 
    // without triggering the "no longer required in app module" error.
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false 
}
