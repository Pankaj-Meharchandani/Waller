plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.waller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.waller"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "4.1.0"

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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.kizitonwose.colorpreference:support:1.1.0")
    implementation("com.github.kizitonwose.colorpreference:core:1.1.0")
    implementation("com.github.eltos:simpledialogfragments:v3.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("io.github.eltos:simpledialogfragments:3.8.3")
    implementation("com.google.android.material:material")
    implementation("androidx.palette:palette:1.0.0")
    // App Center dependencies
    val appCenterSdkVersion = "5.0.4"
    implementation("com.microsoft.appcenter:appcenter-analytics:$appCenterSdkVersion")
    implementation("com.microsoft.appcenter:appcenter-crashes:$appCenterSdkVersion")
}
