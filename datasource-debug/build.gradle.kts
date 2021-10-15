plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
}

android {
    defaultConfig {
        compileSdk = Versions.compileSdk
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
        consumerProguardFiles(
            file("consumer-rules.pro")
        )
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = false }
    }
}

dependencies {
    api(project(":feature-manager"))
    api(Dependencies.coroutinesAndroid)
    testImplementation(project(":converter-jackson"))
    testImplementation(project(":test"))
    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.coroutinesTest)
    testImplementation(Dependencies.turbine)
}

setupAndroidLibraryPublishing()