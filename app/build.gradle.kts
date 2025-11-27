plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // --- APP IDENTITY ---
    namespace = "myedu.oshsu.kg"
    compileSdk = 34

    defaultConfig {
        applicationId = "myedu.oshsu.kg"
        minSdk = 24
        targetSdk = 34
        versionCode = 110
        versionName = "1.1"
    }

    // --- SIGNING CONFIGURATION ---
    signingConfigs {
        create("release") {
            // The keystore will be decoded by GitHub Actions to this path
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "release-key.jks"
            
            // Only configure if the file exists (prevents local build errors if keys are missing)
            if (file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    // --- BUILD TYPES ---
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false // Set to true if you have configured ProGuard rules
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // --- BUILD FEATURES ---
    buildFeatures { compose = true }
    
    // --- KOTLIN OPTIONS ---
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    // --- ANDROID CORE ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // --- COMPOSE UI (Material 3) ---
    implementation(platform("androidx.compose:compose-bom:2024.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // --- NETWORK & WEB ---
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // --- IMAGE LOADING (Coil) ---
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-svg:2.4.0") 
    
    // --- LIFECYCLE ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // --- WORK MANAGER (BACKGROUND TASKS) ---
    implementation("androidx.work:work-runtime-ktx:2.9.0") 
}
