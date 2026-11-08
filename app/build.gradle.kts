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
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // JUnit: cần cho test thuần Kotlin của danh sách chặn quảng cáo (ticket #8).
    testImplementation("junit:junit:4.13.2")
    // Material Components cho Android Views: thành phần Material 3 (switch, card, button) theo lựa chọn thiết kế.
    implementation("com.google.android.material:material:1.12.0")
}
