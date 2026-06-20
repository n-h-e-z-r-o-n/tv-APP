plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.onyx"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.onyx"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.1"

        buildConfigField( "String", "TM_K", "\"eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw\"")
        buildConfigField( "String", "PA_K", "\"ISSecretKey_live_e9d3162e-95cb-42a4-b64b-ee378525ca5a\"")
        buildConfigField( "String", "A_K", "\"https://echo-anime.vercel.app\"")
        //buildConfigField( "String", "A_K", "\"http://192.168.100.22:4000\"")
        buildConfigField( "String", "APPV_J", "\"https://github.com/n-h-e-z-r-o-n/pulseflix-stream-tv/raw/refs/heads/main/App/version.json\"")


    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Onyx Debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources  = true
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
        viewBinding = true
        buildConfig = true
    }


}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation("androidx.media3:media3-exoplayer-dash:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.8.0")
    implementation("androidx.media3:media3-datasource:1.8.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.8.0")
    implementation("androidx.media3:media3-common:1.8.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")

    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0") //youtube
    implementation("androidx.webkit:webkit:1.8.0")
    
    //implementation("org.mozilla.geckoview:geckoview-omni:130.0.20240913135723")//https://mvnrepository.com/artifact/org.mozilla.geckoview/geckoview/versions

    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.graphics)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
