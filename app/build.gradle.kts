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
}
