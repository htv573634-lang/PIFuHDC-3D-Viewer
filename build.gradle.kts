buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20"
        )
    }
}

plugins {
    id("com.android.application") version "9.3.3" apply false
}
