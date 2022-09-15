plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp")
}

android {
    defaultConfig {
        applicationId = "com.qiwi.featuretoggle.sample"
        versionCode = 1
        versionName = "1.0"
        compileSdk = Versions.compileSdk
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
    implementation(project(":feature-manager"))
    implementation(project(":feature-manager-singleton"))
    implementation(project(":converter-jackson"))
    implementation(project(":datasource-remote"))
    ksp(project(":compiler"))
    implementation(Dependencies.androidxCore)
    implementation(Dependencies.androidxAppcompat)
    implementation(Dependencies.androidxLifecycleRuntime)
    implementation(Dependencies.googleMaterial)
}

kotlin {
    sourceSets.all {
        kotlin.srcDir("build/generated/ksp/$name/kotlin")
    }
}
