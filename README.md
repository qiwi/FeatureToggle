# FeatureToggle

The Feature Toggle library for Android.

## Overview

The `FeatureToggle` library allows you to configure features of an Android application at runtime using feature flags. 
Here are common usecases:
- [Trunk-Based Developement](https://trunkbaseddevelopment.com).
- Safe release with a new implementation of the app’s critical part. If the new implementation causes a critical problem, developers can switch back to the old implementation using a feature flag.
- A/B testing, when feature flags are used to switch between multiple feature implementations.

When you use `FeatureToggle` library, each dynamic feature in an Android application must be represented as a separate class or an interface with multiple implementations. 
Each dynamic feature has a `Feature Flag` with unique key and a `FeatureFactory`.

`Feature Flag` is Kotlin class that contains one or more fields that describe feature configuration.

`Feature Flag`s can be loaded from multiple `FeatureFlagDataSource`s. `FeatureFlagDataSource` has a priority value, which helps to decide which `FeatureFlagDataSource` should be used to apply a certain `Feature Flag`.

`Feature Flag`s are stored in JSON format, and at runtime are represented as Koltin objects. 

A `FeatureFactory` is responsible for creating feature objects, using the provided `Feature Flag` objects to decide how to create feature objects.

The `FeatureToggle` library automatically generates registries of current application feature flags and factories using annotation processors.

## Quick Start

1. Add a feature manager and compiler:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager:0.1.1")
kapt("com.qiwi.featuretoggle:featuretoggle-compiler:0.1.1")
```

2. Add a converter that will be used to convert feature flags from Json into Kotlin objects. Two converters are available:
[Jackson](https://github.com/FasterXML/jackson-module-kotlin) and [Gson](https://github.com/google/gson):

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-converter-jackson:0.1.1")
//or
implementation("com.qiwi.featuretoggle:featuretoggle-converter-gson:0.1.1")
```

3. Add a feature flag with an unique key and factory for every feature:

```kotlin
class SampleFeature {
    //code
}

@FeatureFlag("feature_key")
class SampleFeatureFlag(
    //fields
)

@Factory
class SampleFeatureFeatureFactory : FeatureFactory<SampleFeature, SampleFeatureFlag>() {

    override fun createFeature(flag: SampleFeatureFlag): SampleFeature {
        //construct feature using flag
    }

    override fun createDefault(): AndroidInfoFeature {
        //construct default feature implementation
    }
}
```
4. Create an instance of `FeatureManager` using `FeatureManager.Builder`. Provide a converter, necessary data sources and generated registries. It is recommended to fetch feature flags immediately after creating an instance of `FeatureManager`:

```kotlin
val featureManager = FeatureManager.Builder(context)
    .converter(JacksonFeatureFlagConverter()) //or GsonFeatureFlagConverter()
    .logger(DebugLogger()) //optional logger
    .addDataSource(AssetsDataSource("feature_flags.json", context)) //also available additional data sources: FirebaseDataSource, AgConnectDataSource, RemoteDataSource
    .flagRegistry(FeatureFlagRegistryGenerated()) //set generated flag registry
    .factoryRegistry(FeatureFactoryRegistryGenerated()) //set generated factory registry
    .build()

featureManager.fetchFlags()
```

5. Provide an instance of `FeatureManager` using your favourite DI framework, or you can use the `FeatureToggle` singleton:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-singleton:0.1.1")
```
```kotlin
FeatureToggle.setFeatureManager(...)

FeatureToggle.featureManager().getFeature(...)
```

6. Get a feature from `FeatureManager`:

```kotlin
val feature = featureManager.getFeature<SampleFeature>()
```

It is recommended to wait for the feature flags to get loaded, for example on a splash screen:
```kotlin
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
        lifecycleScope.launchWhenCreated {
            runCatching {
                withTimeout(TIMEOUT_MILLIS) {
                    FeatureToggle.featureManager().awaitFullLoading()
                }
            }
            openMainActivity()
        }
    }

    ...
}
```
## Assets DataSource

`AssetsDataSource` loads feature flags from a JSON file from `assets` folder. It is used to include default feature flag values into apk or app bundle. Default `AssetsDataSource` priority value is `1`.

## Cache

Feature flags when loaded from remote data sources are cached and used on the next fetch. You can also set a priority of cached feature flags in a `FeatureManager.Builder`:
```kotlin
FeatureManager.Builder(context)
    ...
    .cachedFlagsPriority(2)
```
Default cached flags priority value is `2`.

## Remote Config DataSource

`FeatureToggle` supports [Firebase Remote Config](https://firebase.google.com/docs/remote-config)
and [AppGallery Connect Remote Configuration](https://developer.huawei.com/consumer/en/agconnect/remote-configuration/).

Add a remote config value with its feature flag key for every feature. Remote config value must be stored as a Json string. Sample:

```json
{
    "versionName": "12",
    "apiLevel": 31
}
```

Default remote config data sources priority value is `4`.

### `FirebaseDataSource`:

1. [Add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2. If you need to use Google Analytics with Remote Config, add the analytics dependency:

```kotlin
implementation("com.google.firebase:firebase-analytics:${version}")
```

3. Add `FirebaseDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-firebase:0.1.1")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(FirebaseDataSource())
```
4. Add feature flags into the **Engage > Remote Config** section in the [Firebase console](https://console.firebase.google.com). Sample:

<img src="https://user-images.githubusercontent.com/27818051/137912355-0ef1634d-75d5-4e57-88e9-cdaeeca8d753.png" width="400" height="300">

For more details about Firebase Remote Config, look at the [official docs](https://firebase.google.com/docs/remote-config).

### `AgConnectDataSource`:

1. [Integrate the AppGallery Connect SDK](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-get-started-android-0000001058210705#section1552914317248).
2. If you need to use HUAWEI Analytics with remote configuration, add the analytics dependency:

```kotlin
implementation("com.huawei.hms:hianalytics:${version}")
```
3. Add `AgConnectDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-agconnect:0.1.1")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(AgConnectDataSource())
```
4. Add feature flags into the **Grow > Remote Configuration** section in [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html). Sample:

<img src="https://user-images.githubusercontent.com/27818051/137916454-35613ba8-fe58-4198-8d71-ec52cb5db030.png" width="700" height="100">
<img src="https://user-images.githubusercontent.com/27818051/137917223-e1daafa8-3d05-4f81-9f95-b0caa4a417c5.png" width="400" height="300">

For more details about AppGallery Connect Remote Configuration, look at the [official docs](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-remoteconfig-android-getstarted-0000001056347165).

## Remote DataSource

The `FeatureToggle` library also has `RemoteDataSource` that can download feature flags from JSON REST API using the [OkHttp](https://github.com/square/okhttp) library.
JSON response must be in the following format:
```json
[
    {
      "feature": "android_info",
      "versionName": "12",
      "apiLevel": 31
    }
]
```

Usage:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-remote:0.1.1")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(RemoteDataSource("url"))
```

Default `RemoteDataSource` priority value is `3`.

## Debug DataSource

If you need to update feature flags manually (for debug purposes), use `DebugDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-debug:0.1.1")
```
```kotlin
val debugDataSource = DebugDataSource()

FeatureManager.Builder(context)
    ...
    .addDataSource(debugDataSource)

...

debugDataSource.updateFeatureFlagsFromJsonString(...)
```

Default `DebugDataSource` priority value is `100`.

## Custom DataSource

You can extend the `FeatureToggle` library with custom data source:

```kotlin
class CustomDataSource : FeatureFlagDataSource {

    override val sourceType: FeatureFlagsSourceType ...

    override val key: String ...

    override val priority: Int ...

    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = flow {
        ...
    }

}
```

## Testing

If you need to use `FeatureManager` in unit tests, use `FakeFeatureManager`. It doesn’t load flags from data sources – but uses mocked feature flags instead.
Usage example:

```kotlin
testImplementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-test:0.1.1")
```
```kotlin
val fakeFeatureManager = FakeFeatureManager.create(FeatureFlagRegistryGenerated(), FeatureFactoryRegistryGenerated())

...

fakeFeatureManager.overrideFlag(...)
```

## R8/Proguard

`FeatureToggle` library modules have bundled proguard rules for its classes. However, you need to add a proguard rule for every feature flag class:

```kotlin
-keep class com.example.SampleFeatureFlag { *; }
```

## License

    MIT License

    Copyright (c) 2021 QIWI

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
