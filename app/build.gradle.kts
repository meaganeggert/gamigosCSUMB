plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gamigosjava"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gamigosjava"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.drawerlayout)

    // Firebase (BoM first)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Credentials
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)

    // TikXml
    implementation(libs.tikxml.core)
    implementation(libs.tikxml.annotation)
    implementation(libs.tikxml.converter)
    annotationProcessor(libs.tikxml.processor)

    // Picasso (optional)
    implementation(libs.picasso)

    // Algolia
    implementation(libs.algolia) {
        exclude(group = "com.android.support")
    }

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
