import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(project(":core"))
    implementation(Dependencies.kotlinpoet)
    implementation(Dependencies.kotlinpoetKsp)
    implementation(Dependencies.kotlinSymbolProcessingApi)
}

setupJavaLibraryPublishing()