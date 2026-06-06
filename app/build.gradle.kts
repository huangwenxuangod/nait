plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nailit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nailit.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        val openAiBaseUrl = System.getenv("NAILIT_OPENAI_BASE_URL") ?: "https://api.openai.com/v1/"
        val openAiModel = System.getenv("NAILIT_OPENAI_MODEL") ?: "gpt-4.1-mini"
        val openAiApiKey = System.getenv("NAILIT_OPENAI_API_KEY") ?: ""
        val qwenRealtimeWsUrl = System.getenv("NAILIT_QWEN_REALTIME_WS_URL")
            ?: "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
        val qwenRealtimeModel = System.getenv("NAILIT_QWEN_REALTIME_MODEL")
            ?: "qwen3.5-omni-plus-realtime"
        val supabaseUrl = System.getenv("NAILIT_SUPABASE_URL")
            ?: "http://129.204.200.38:8000"
        val supabaseAnonKey = System.getenv("NAILIT_SUPABASE_ANON_KEY")
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyAgCiAgICAicm9sZSI6ICJhbm9uIiwKICAgICJpc3MiOiAic3VwYWJhc2UtZGVtbyIsCiAgICAiaWF0IjogMTY0MTc2OTIwMCwKICAgICJleHAiOiAxNzk5NTM1NjAwCn0.dc_X5iR_VP_qT0zsiyj_I_OZ2T9FtRU2BBNWN8Bu4GE"

        buildConfigField("String", "OPENAI_BASE_URL", "\"$openAiBaseUrl\"")
        buildConfigField("String", "OPENAI_MODEL", "\"$openAiModel\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
        buildConfigField("String", "QWEN_REALTIME_WS_URL", "\"$qwenRealtimeWsUrl\"")
        buildConfigField("String", "QWEN_REALTIME_MODEL", "\"$qwenRealtimeModel\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../nailit.keystore")
            storePassword = "nailit123"
            keyAlias = "nailit"
            keyPassword = "nailit123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.activity:activity-compose:1.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")

    implementation("com.google.mediapipe:tasks-vision:0.20230731")

    implementation("io.ktor:ktor-client-core:3.2.1")
    implementation("io.ktor:ktor-client-okhttp:3.2.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.1")
    implementation("io.ktor:ktor-client-logging:3.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.2.4")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.2.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
