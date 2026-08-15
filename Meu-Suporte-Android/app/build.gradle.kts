plugins {
    id("com.android.application")
}

if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "online.meusuporte.painel"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/meusuporte-dev.keystore")
            storePassword = "android"
            keyAlias = "meusuportedev"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "online.meusuporte.painel"
        minSdk = 23
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.5"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.credentials:credentials:1.6.0-beta02")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-beta02")
}
