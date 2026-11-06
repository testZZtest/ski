plugins {
    id("com.android.application")
}

// Version lives in BuildInfo.java so the Gradle and on-device (build.sh) builds always agree.
val buildInfo = file("src/main/java/ski/agent/BuildInfo.java").readText()
val appVersionName = Regex("""VERSION = "([^"]+)"""").find(buildInfo)!!.groupValues[1]
val appVersionCode = Regex("""VERSION_CODE = (\d+)""").find(buildInfo)!!.groupValues[1].toInt()

android {
    // proot for the built-in Linux ships as native libraries and must be extracted to disk to run
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    namespace = "ski.agent"
    compileSdk = 36

    defaultConfig {
        applicationId = "ski.agent"
        minSdk = 30
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // Release signing comes from environment variables (set as GitHub Actions secrets).
    // Without them, assembleRelease produces an unsigned APK.
    val keystorePath = System.getenv("SKI_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SKI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SKI_KEY_ALIAS") ?: "ski"
                keyPassword = System.getenv("SKI_KEY_PASSWORD") ?: System.getenv("SKI_KEYSTORE_PASSWORD")
            }
        }
    }

    // github: the release on GitHub (self-updates, APK installs, developer mode).
    // play: the Google Play build, without those (Play's policies). Same package, same key.
    flavorDimensions += "store"
    productFlavors {
        create("github") { dimension = "store" }
        create("play") { dimension = "store" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = false
    }
}
