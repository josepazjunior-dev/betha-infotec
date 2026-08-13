plugins {
    id("com.android.application")
    if (file("google-services.json").exists()) {
        id("com.google.gms.google-services")
    }
}

android {
    namespace = "online.meusuporte.painel"
    compileSdk = 36

    defaultConfig {
        applicationId = "online.meusuporte.painel"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
}
