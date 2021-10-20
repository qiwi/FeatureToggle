# FeatureToggle

Feature toggle library for Android.

## Overview

`FeatureToggle` library allows to configure features of Android application in runtime using feature flags. 
Common usecases:
- [Trunk-Based Developement](https://trunkbaseddevelopment.com) when developers can release application with not production ready code in main branch and hide this code behind a feature flag.
- Safe release with new implementation of critical part of application. If critial problem found in new implementation, developers can switch to old implementation using feature flag.
- A/B testing when feature flags used to switch between multiple feature implementations.

When using `FeatureToggle` library each dynamic feature in Android application must been represent as separate class or interface with multiple implementations. 
Each dynamic feature have `Feature Flag` with unique key and `FeatureFactory`. 

`Feature Flag` is Kotlin class that contains one or more fields that describe feature config.

`Feature Flag`s can be loaded from multiple `FeatureFlagDataSource`s. `FeatureFlagDataSource` have priority, that used to decide from which `FeatureFlagDataSource` apply specific `Feature Flag`.
`Feature Flag`s stored in Json and in runtime represented as Koltin objects. 

`FeatureFactory` is responsible to create feature object using provided `Feature Flag` object to make a decision how to create feature object.

`FeatureToggle` library automatically generates registries of current application feature flags and factories using annotation processors.

## Quick Start

1. Add feature manager and compiler:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager:0.1.0")
kapt("com.qiwi.featuretoggle:featuretoggle-compiler:0.1.0")
```

2. Add converter that will be used to convert feature flags from Json into Kotlin objects. Two converters are available:
[Jackson](https://github.com/FasterXML/jackson-module-kotlin) and [Gson](https://github.com/google/gson):

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-converter-jackson:0.1.0")
//or
implementation("com.qiwi.featuretoggle:featuretoggle-converter-gson:0.1.0")
```

3. For each feature add feature flag with unique key and factory:

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
4. Create instance of `FeatureManager` using `FeatureManager.Builder`. Provide converter, necessary data sources and generated registries. Also it is recommended to fetch feature flags immidiately after creation instance of `FeatureManager`:

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

5. Provide instance of `FeatureManager` using your favourite DI framework or use `FeatureToggle` singleton:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-singleton:0.1.0")
```
```kotlin
FeatureToggle.setFeatureManager(...)

FeatureToggle.featureManager().getFeature(...)
```

6. Get feature from `FeatureManager`:

```kotlin
val feature = featureManager.getFeature<SampleFeature>()
```

It is recommended to wait with timeout for feature flags loading for example on splash screen:
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

`AssetsDataSource` loads feature flags from Json file in `assets` folder. It is used to include default feature flag values into apk or app bundle. Default `AssetsDataSource` priority is `1`.

## Cache

Feature flags that loaded from remote data sources will be cached and used on next fetch. You can set priority of cached feature flags in `FeatureManager.Builder`:
```kotlin
FeatureManager.Builder(context)
    ...
    .cachedFlagsPriority(2)
```
Default cached flags priority is `2`.

## Remote Config DataSource

`FeatureToggle` supports [Firebase Remote Config](https://firebase.google.com/docs/remote-config)
and [AppGallery Connect Remote Configuration](https://developer.huawei.com/consumer/en/agconnect/remote-configuration/).

For each feature flag add remote config value with its feature flag key. Remote config value must be stored as Json string. Sample:

```json
{
    "versionName": "12",
    "apiLevel": 31
}
```

Default remote config data sources priority is `4`.

### Usage with `FirebaseDataSource`:

1. If you haven't already, [add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2. If you need to use Google Analytics with Remote Config, add analytics dependency:

```kotlin
implementation("com.google.firebase:firebase-analytics:${version}")
```

3. Add `FirebaseDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-firebase:0.1.0")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(FirebaseDataSource())
```
4. Add feature flags in **Engage > Remote Config** section in the [Firebase console](https://console.firebase.google.com). Sample:

<img src="https://user-images.githubusercontent.com/27818051/137912355-0ef1634d-75d5-4e57-88e9-cdaeeca8d753.png" width="400" height="300">

For more details about Firebase Remote Config see [official docs](https://firebase.google.com/docs/remote-config).

### Usage with `AgConnectDataSource`:

1. If you haven't already, [integrate the AppGallery Connect SDK](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-get-started-android-0000001058210705#section1552914317248).
2. If you need to use HUAWEI Analytics with remote configuration, add analytics dependency:

```kotlin
implementation("com.huawei.hms:hianalytics:${version}")
```
3. Add `AgConnectDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-agconnect:0.1.0")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(AgConnectDataSource())
```
4. Add feature flags in **Grow > Remote Configuration** section in [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html). Sample:

<img src="https://user-images.githubusercontent.com/27818051/137916454-35613ba8-fe58-4198-8d71-ec52cb5db030.png" width="700" height="100">
<img src="https://user-images.githubusercontent.com/27818051/137917223-e1daafa8-3d05-4f81-9f95-b0caa4a417c5.png" width="400" height="300">

For more details about AppGallery Connect Remote Configuration see [official docs](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-remoteconfig-android-getstarted-0000001056347165).

## Remote DataSource

`FeatureToggle` also have `RemoteDataSource` that can load feature flags from Json REST API using [OkHttp](https://github.com/square/okhttp) library.
Response Json must be in the following format:
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
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-remote:0.1.0")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(RemoteDataSource("url"))
```

Default `RemoteDataSource` priority is `3`.

## Debug DataSource

If there is need to update feature flags manually (for debug purposes), use `DebugDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-debug:0.1.0")
```
```kotlin
val debugDataSource = DebugDataSource()

FeatureManager.Builder(context)
    ...
    .addDataSource(debugDataSource)

...

debugDataSource.updateFeatureFlagsFromJsonString(...)
```

Default `DebugDataSource` priority is `100`.

## Custom DataSource

It is possible to extend `FeatureToggle` with custom data source:

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

If you need to use `FeatureManager` in unit tests, use `FakeFeatureManager`. It do not load flags from data sources. Instead it uses mocked feature flags.
Usage:

```kotlin
testImplementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-test:0.1.0")
```
```kotlin
val fakeFeatureManager = FakeFeatureManager.create(FeatureFlagRegistryGenerated(), FeatureFactoryRegistryGenerated())

...

fakeFeatureManager.overrideFlag(...)
```

## R8/Proguard

`FeatureToggle` modules have bundled proguard rules for its classes. However for each feature flag class you need to add proguard rule:

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
