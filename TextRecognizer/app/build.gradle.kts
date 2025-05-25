plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.textrecognizer"
    compileSdk = 34 // Latest stable SDK

    defaultConfig {
        applicationId = "com.example.textrecognizer"
        minSdk = 21 // Android 5.0 Lollipop
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        // For App Bundles
        bundle {
            density.enableSplit = true
            abi.enableSplit = true
            language.enableSplit = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0") // AndroidX Core KTX
    implementation("androidx.appcompat:appcompat:1.6.1") // AndroidX AppCompat
    implementation("com.google.android.material:material:1.11.0") // Material Components
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // ConstraintLayout
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ML Kit Text Recognition dependencies
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0") // Google Play Services version of ML Kit
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0") // Chinese language pack

    // CameraX core library using the camera2 implementation
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    // CameraX Lifecycle Library
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    // CameraX View class
    implementation("androidx.camera:camera-view:${cameraxVersion}")
}
