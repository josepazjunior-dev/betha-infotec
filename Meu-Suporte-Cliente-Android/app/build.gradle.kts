plugins { id("com.android.application") }

if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "online.meusuporte.junioriptv"
    compileSdk = 36
    defaultConfig {
        applicationId = "online.meusuporte.junioriptv"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
}
