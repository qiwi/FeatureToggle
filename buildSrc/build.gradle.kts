plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

val kotlinVersion = "1.5.30"
val androidToolsVersion = "7.0.2"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.android.tools.build:gradle:$androidToolsVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$kotlinVersion")
}