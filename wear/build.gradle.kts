plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.runtracker.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.runtracker.app"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Debug signing for release builds (enables testing release perf without a keystore)
        create("release") {
            storeFile = signingConfigs.getByName("debug").storeFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.runtime.ExperimentalComposeApi",
            "-opt-in=androidx.wear.compose.material.ExperimentalWearMaterialApi",
            // Enable strong skipping mode — allows Compose to skip recomposition
            // of composables with unstable parameters when their values haven't changed
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Force kotlin-stdlib to match our Kotlin compiler version (1.9.20)
// Prevents transitive dependencies from pulling in incompatible 2.x stdlib
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
    }
}

dependencies {
    implementation(project(":shared"))

    val wearComposeVersion = rootProject.extra["wear_compose_version"] as String
    val roomVersion = rootProject.extra["room_version"] as String

    // Wear OS
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-material:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-navigation:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-material-core:$wearComposeVersion")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.1")

    // ProfileInstaller — enables Baseline Profile / ART profile optimization
    // This allows the watch to pre-compile critical code paths for faster startup & scroll
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // Health Services (1.1.0-alpha04 — last version before Kotlin 2.x requirement)
    implementation("androidx.health:health-services-client:1.1.0-alpha04")

    // Ongoing Activity
    implementation("androidx.wear:wear-ongoing:1.0.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Wearable Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
