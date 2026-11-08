plugins {
    id("com.android.application")
}

android {
    namespace = "com.meomuc.youtubeintablet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meomuc.youtubeintablet"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Chưa có keystore phát hành: ký bằng debug key để APK cài được (quyết định của chủ dự án).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // JUnit: cần cho test thuần Kotlin của danh sách chặn quảng cáo (ticket #8).
    testImplementation("junit:junit:4.13.2")
    // Material Components cho Android Views: thành phần Material 3 (switch, card, button) theo lựa chọn thiết kế.
    implementation("com.google.android.material:material:1.12.0")
}
