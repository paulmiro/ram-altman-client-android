plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Bumping this on main triggers a GitHub release (see .github/workflows/release.yml).
val appVersion = "1.0.0"

android {
    namespace = "de.paulmiro.ramaltman"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.paulmiro.ramaltman"
        minSdk = 31
        targetSdk = 37
        versionName = appVersion
        // "1.2.3" -> 10203
        versionCode = appVersion.split(".").fold(0) { acc, part -> acc * 100 + part.toInt() }
    }

    // Release signing is configured via environment variables (see README / release workflow).
    val keystore = System.getenv("KEYSTORE_FILE")
    if (keystore != null) {
        signingConfigs.create("release") {
            storeFile = file(keystore)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
}
