plugins {
    alias(libs.plugins.android.application) // Aplikuje sa tu, bez 'apply false'
    alias(libs.plugins.kotlin.android)      // Aplikuje sa tu, bez 'apply false'
    alias(libs.plugins.kotlin.compose)      // Aplikuje sa tu, bez 'apply false'

    // KSP pre Kotlin 2.0.21.
    // Overte si najnovšiu stabilnú verziu KSP pre Kotlin 2.0.x na GitHub KSP releases.
    // Príklad: Ak je Kotlin verzia z libs.versions.toml "2.0.21",
    // a najnovšia KSP pre Kotlin 2.0.0 (čo by malo byť kompatibilné) je napr. "2.0.0-1.0.21"
    // Ak chcete presne pre 2.0.21 a existuje, použite tú.
    // Tu používam hypotetickú verziu, NAHRAĎTE JU AKTUÁLNOU SPRÁVNOU VERZIOU KSP PRE KOTLIN 2.0.21!
    // Napríklad, ak je najnovšia stabilná KSP pre Kotlin 2.0.x: "2.0.0-1.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" // <<<<<< DÔLEŽITÉ: SKONTROLUJTE A AKTUALIZUJTE TÚTO VERZIU!
}

android {
    namespace = "com.dct.qr"
    compileSdk = 36 // Zvážte presun do libs.versions.toml ako napr. `compileSdk = libs.versions.compileSdk.get().toInt()`

    defaultConfig {
        applicationId = "com.dct.qr"
        minSdk = 26 // Zvážte presun do libs.versions.toml
        targetSdk = 36 // Zvážte presun do libs.versions.toml
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
    // Ak potrebujete špecifickú verziu Compose Compiler, môžete ju pridať do composeOptions
    // napr. Ak máte verziu v libs.versions.toml:
    // composeOptions {
    //     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    // }
}

dependencies {
    // Verzia CameraX
    val cameraxVersion = "1.3.3" // Môžete zvážiť presun do libs.versions.toml
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    // implementation("androidx.camera:camera-extensions:${cameraxVersion}") // Voliteľné pre efekty

    // ML Kit Barcode Scanning
    git  // Môžete zvážiť presun do libs.versions.toml

    // Základné AndroidX a Compose knižnice z libs.versions.toml
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Používa BOM definovaný v libs.versions.toml
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testovacie závislosti
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Aj pre testy používame BOM
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Ikony (Môžete zjednodušiť, ak máte BOM, ktorý spravuje verzie Material)
    // Ak váš Compose BOM (libs.androidx.compose.bom) už obsahuje material-icons,
    // možno nebudete musieť špecifikovať verziu pre material-icons-core explicitne.
    // Avšak, ponechanie pre jasnosť nie je na škodu.
    implementation(libs.androidx.material.icons.core) // Ak máte definované v libs.versions.toml
    implementation(libs.androidx.material.icons.extended) // Ak máte definované v libs.versions.toml
    // Ak nemáte androidx.material.icons.core a extended v libs.versions.toml, použite priame verzie:
    // implementation("androidx.compose.material:material-icons-core:1.7.0") // Použite najnovšiu verziu
    // implementation("androidx.compose.material:material-icons-extended:1.7.0") // Použite najnovšiu verziu

    // Room
    val roomVersion = "2.6.1" // Môžete zvážiť presun do libs.versions.toml
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion") // Použite ksp namiesto annotationProcessor
    implementation("androidx.room:room-ktx:$roomVersion") // Pre Coroutines & Flow podporu

    // ViewModel
    // Tieto by mali byť ideálne tiež v libs.versions.toml
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-core:1.7.0-beta01") // Alebo najnovšia
    implementation("androidx.compose.material:material-icons-extended:1.7.0-beta01") // Alebo najnovšia

    implementation(libs.zxing.core)


}
