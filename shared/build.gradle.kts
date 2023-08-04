buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        //maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    kotlin("multiplatform") version "1.8.21"
    id("com.android.library") version "8.1.0"
    id("org.jetbrains.compose") version "1.4.3"
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    android()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }

    ios()
    iosSimulatorArm64 {
        // TODO: remove after 1.5 release
        binaries.forEach {
            it.freeCompilerArgs += listOf(
                "-linker-option", "-framework", "-linker-option", "Metal",
                "-linker-option", "-framework", "-linker-option", "CoreText",
                "-linker-option", "-framework", "-linker-option", "CoreGraphics",
            )
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(compose.ui)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val composeVersion = "1.4.3"
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
            }
        }
        val iosMain by getting
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    namespace = "com.example.minesweeper_kmp"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = "1.4.3"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
