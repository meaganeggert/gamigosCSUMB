plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
    //    id("com.android.application")
}

android {
    namespace = "com.example.gamigosjava"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gamigosjava"
        minSdk = 34
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Dependencies for API conversion
    // Retrofit / OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // TikXml (all of these)
    implementation("com.tickaroo.tikxml:core:0.8.13")
    implementation("com.tickaroo.tikxml:annotation:0.8.13")
    implementation("com.tickaroo.tikxml:retrofit-converter:0.8.13")
    annotationProcessor("com.tickaroo.tikxml:processor:0.8.13")

    implementation ("com.squareup.picasso:picasso:2.8")

    implementation("com.google.firebase:firebase-firestore:26.0.2")


}