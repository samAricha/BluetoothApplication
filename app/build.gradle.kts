plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id ("dagger.hilt.android.plugin")
    id ("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.teka.bluetoothapplication"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.teka.bluetoothapplication"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    // (Optional) Lifecycle components for easier integration with lifecycle-aware components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")


    // Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    // adding activity ktx dependency for using view model in activity
    implementation ("androidx.activity:activity-ktx:1.9.2")
    implementation ("androidx.fragment:fragment-ktx:1.8.4")

    implementation("org.greenrobot:eventbus:3.3.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.core:core-splashscreen:1.0.1")

}