import java.util.Properties

plugins {
    alias(libs.plugins.escapelauncher.android.application)
    alias(libs.plugins.escapelauncher.android.compose)
    alias(libs.plugins.escapelauncher.android.composeui)
    alias(libs.plugins.escapelauncher.android.hilt)
    alias(libs.plugins.escapelauncher.android.testing)
    alias(libs.plugins.escapelauncher.android.room)
    alias(libs.plugins.kotlin.serialization)
}

// Load release signing credentials from an optional, git-ignored properties file.
// When absent (e.g. CI / F-Droid builds), release builds fall back to debug signing.
val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        keystoreFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystoreProperties.isNotEmpty()

val baseVersionCode = "3.0"

android {
    namespace = "com.geecee.escapelauncher"

    defaultConfig {
        applicationId = "io.carruc.pondlauncher"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = baseVersionCode
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "app_version", baseVersionCode)
        resValue("string", "app_name", "Pond Launcher")
        resValue("string", "app_flavour", "Unknown Flavor")

        buildConfigField("String", "APP_VERSION", "\"$baseVersionCode\"")
        buildConfigField("String", "APP_NAME", "\"Pond Launcher\"")
        buildConfigField("String", "APP_FLAVOUR", "\"Unknown Flavor\"")
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Pond Launcher (Debug)")
            buildConfigField("String", "APP_NAME", "\"Pond Launcher (Debug)\"")
        }
        release {
            resValue("string", "app_name", "Pond Launcher")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.navigation3)
    implementation(libs.bundles.hilt)

    // WorkManager (with Hilt-injected workers)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // JSON Parsing
    implementation(libs.gson)

    // Testing Libraries
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.test.manifest)

    // Modules
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:theme"))
    implementation(project(":core:analytics"))
    implementation(project(":core:cloudmessaging"))
    implementation(project(":feature:homescreen"))
    implementation(project(":feature:workapps"))
    implementation(project(":feature:privatespace"))
    implementation(project(":feature:securefolder"))
    implementation(project(":feature:weather"))
    implementation(project(":feature:screentime"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:appslist"))
    implementation(project(":feature:newwidgets"))
    implementation(project(":feature:onboarding"))
}