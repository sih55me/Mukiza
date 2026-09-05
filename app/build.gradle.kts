plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.wiwolf.music"
    compileSdk {
        version = release(36){
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.wiwolf.music"
        minSdk = 33
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 33
        versionCode = 2
        versionName = "2.1"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

dependencies {

    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
}