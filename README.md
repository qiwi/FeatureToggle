# FeatureToggle

Feature toggle library for Android.

**FeatureToggle** library uses feature flags that stored in Json and parsed into Kotlin classes. Feature flags can be loaded from multiple data sources.
Feature factories are used to provide actual features implementations.

Registry of current application feature flags and factories are created using annotation processors.

## Quick Start

1. Add feature manager and compiler:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager:${version}")
kapt("com.qiwi.featuretoggle:featuretoggle-compiler:${version}")
```

2. Add converter that will be used to convert feature flags from Json into Kotlin objects. Two converters are available:
[Jackson](https://github.com/FasterXML/jackson-module-kotlin) and [Gson](https://github.com/google/gson):

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-converter-jackson:${version}")
//or
implementation("com.qiwi.featuretoggle:featuretoggle-converter-gson:${version}")
```

3. For each feature add feature flag and factory:

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
4. Create instance of `FeatureManager` using `FeatureManager.Builder` and fetch feature flags:

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
implementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-singleton:${version}")
```
```kotlin
FeatureToggle.setFeatureManager(...)

FeatureToggle.featureManager().getFeature(...)
```

6. Get feature from `FeatureManager`:

```kotlin
val feature = featureManager.getFeature<SampleFeature>()
```

It is recommended to wait with timeout for feature flags loading for example on SplashScreen:
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

## Remote Config DataSource

**FeatureToggle** supports [Firebase Remote Config](https://firebase.google.com/docs/remote-config)
and [AppGallery Connect Remote Configuration](https://developer.huawei.com/consumer/en/agconnect/remote-configuration/).

For each feature flag add remote config value with its feature key. Remote config value must be stored as Json string. Sample:

```json
{
    "versionName": "12",
    "apiLevel": 31
}
```

Usage with `FirebaseDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-firebase:${version}")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(FirebaseDataSource())
```

Usage with `AgConnectDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-agconnect:${version}")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(AgConnectDataSource())
```

## Remote DataSource

**FeatureToggle** also have `RemoteDataSource` that can load feature flags from Json REST API using [OkHttp](https://github.com/square/okhttp) library.
Response Json must be in the following format:
```json
[
    {
      "feature": "android_config",
      "versionName": "12",
      "apiLevel": 31
    }
]
```

Usage:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-remote:${version}")
```
```kotlin
FeatureManager.Builder(context)
    ...
    .addDataSource(RemoteDataSource("url"))
```

## Debug DataSource

If there is need to update feature flags manually (for debug purposes), use `DebugDataSource`:

```kotlin
implementation("com.qiwi.featuretoggle:featuretoggle-datasource-debug:${version}")
```
```kotlin
val debugDataSource = DebugDataSource()

FeatureManager.Builder(context)
    ...
    .addDataSource(debugDataSource)

...

debugDataSource.updateFeatureFlagsFromJsonString(...)
```

## Custom DataSource

It is possible to extend **FeatureToggle** with custom data source:

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
testImplementation("com.qiwi.featuretoggle:featuretoggle-feature-manager-test:${version}")
```
```kotlin
val fakeFeatureManager = FakeFeatureManager.create(FeatureFlagRegistryGenerated(), FeatureFactoryRegistryGenerated())

...

fakeFeatureManager.overrideFlag(...)
```

## R8/Proguard

**FeatureToggle** modules have bundled proguard rules for its classes. However for each feature flag class you need to add proguard rule:

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