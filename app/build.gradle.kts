import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.vemestael.archeryshotcounter"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.vemestael.archeryshotcounter"
        minSdk = 30
        targetSdk = 36
        versionCode = 8
        versionName = "1.6"

    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }

    // "debug"/"release" (buildTypes, below) controls signing + minification only — optimized and
    // production-signed, or unoptimized and debug-signed. Whether the app installs *alongside* the
    // production one is a separate question, answered by the flavor dimension further down. Keeping
    // these two axes apart (they used to be conflated: applicationIdSuffix lived inside the "debug"
    // buildType, so "side-by-side install" and "debug-signed/unoptimized" looked like the same
    // thing) is what lets `local` + `release` exist: a production-signed, optimized build that still
    // installs next to the Play release for testing.
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // No applicationIdSuffix here — see the flavor dimension below.
        }
    }

    flavorDimensions += "install"
    productFlavors {
        // The Play-distributed shape: no suffix, same applicationId as the production app. This
        // is what `store` + `release` assembles into the AAB/APK that actually gets uploaded.
        create("store") {
            dimension = "install"
        }
        // Installs side by side with the production app (own applicationId, own data) for local
        // testing, whether or not it's also debug-signed/unoptimized — that's the `debug`/`release`
        // buildType's call, independent of this flavor.
        create("local") {
            dimension = "install"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            resValue("string", "app_name", "Archery Shot Counter (Local)")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        resValues = true
    }

}

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("ArcheryShotCounter-${output.versionName.get()}.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.play.services.wearable)
    implementation(libs.wear)
    implementation(libs.fragment)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}