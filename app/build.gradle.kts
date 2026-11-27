plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yg.timeanchor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yg.timeanchor"
        minSdk = 26
        targetSdk = 35          // 见 README：上架才需要 36，本地用 35 更少限制
        versionCode = 1
        versionName = "0.1-phase1"
    }

    buildTypes {
        release { isMinifyEnabled = false }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // 阶段 1 不引第三方依赖，纯 framework，编译最快
}
