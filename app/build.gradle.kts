plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dct.qr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dct.qr"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("androidx.compose:compose-bom:2024.09.00")) // Alebo najnovší BOM
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core") // Základné ikony
    implementation("androidx.compose.material:material-icons-extended") // Rozšírené ikony
    implementation("androidx.compose.material:material-icons-core:1.7.0") // Použite najnovšiu verziu


    // Pre CameraX (ak budete implementovať skener)
    // val cameraxVersion = "1.3.4" // Alebo najnovšia
    // implementation("androidx.camera:camera-core:${cameraxVersion}")
    // implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    // implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    // implementation("androidx.camera:camera-view:${cameraxVersion}")
    // implementation("androidx.camera:camera-extensions:${cameraxVersion}")
}
