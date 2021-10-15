/**
 * Copyright (c) 2021 QIWI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
object Versions {

    const val compileSdk = 30
    const val minSdk = 21
    const val targetSdk = 30

    const val kotlin = "1.5.30"
    const val androidTools = "7.0.2"

    const val coroutines = "1.5.2"

    const val androidxCore = "1.6.0"
    const val androidxAppcompat = "1.3.1"
    const val androidxLifecycle = "2.3.1"
    const val googleMaterial = "1.4.0"

    const val jackson = "2.12.5"
    const val gson = "2.8.8"

    const val okhttp = "4.9.1"

    const val firebaseConfig = "21.0.1"
    const val agConnectRemoteConfig = "1.6.0.300"

    const val kotlinpoet = "1.10.1"

    const val junit = "4.13.2"
    const val robolectric = "4.6.1"
    const val mockito = "3.3.3"
    const val androidxTestCore = "1.4.0"
    const val turbine = "0.6.1"
}

object Dependencies {

    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val androidPlugin = "com.android.tools.build:gradle:${Versions.androidTools}"
    const val dokkaPlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.kotlin}"

    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"

    const val androidxCore = "androidx.core:core-ktx:${Versions.androidxCore}"
    const val androidxAppcompat = "androidx.appcompat:appcompat:${Versions.androidxAppcompat}"
    const val androidxLifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.androidxLifecycle}"
    const val googleMaterial = "com.google.android.material:material:${Versions.googleMaterial}"

    const val jacksonKotlinModule = "com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"

    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"

    const val firebaseConfig = "com.google.firebase:firebase-config-ktx:${Versions.firebaseConfig}"
    const val agConnectRemoteConfig = "com.huawei.agconnect:agconnect-remoteconfig:${Versions.agConnectRemoteConfig}"

    const val kotlinpoet = "com.squareup:kotlinpoet:${Versions.kotlinpoet}"

    const val junit = "junit:junit:${Versions.junit}"
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val androidxTestCore = "androidx.test:core:${Versions.androidxTestCore}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
    const val okhttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.okhttp}"
}