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
package com.qiwi.featuretoggle

import android.content.Context
import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.creator.RealFeatureCreator
import com.qiwi.featuretoggle.datasource.CacheDataSource
import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.factory.FeatureFactory
import com.qiwi.featuretoggle.flag.SimpleFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.repository.RealFeatureRepository
import com.qiwi.featuretoggle.storage.CachedFlagsStorage
import com.qiwi.featuretoggle.storage.InMemoryFlagsStorage
import com.qiwi.featuretoggle.storage.InternalCachedFlagsStorage
import com.qiwi.featuretoggle.util.Logger
import com.qiwi.featuretoggle.util.LoggerStub

/**
 * A [FeatureManager] fetches feature flags from multiple [FeatureFlagDataSource]s and
 * creates features using fetched feature flags.
 */
interface FeatureManager {

    /**
     * @return Feature flag object.
     * @param flagClass Feature flag class.
     */
    fun <Flag> getFeatureFlag(flagClass: Class<Flag>): Flag?

    /**
     * @return Feature object.
     * @param featureClass Feature class.
     */
    fun <Feature> getFeature(featureClass: Class<Feature>): Feature

    /**
     * Returns feature object immediately (feature will be created using current feature flags config).
     *
     * @return Feature object.
     * @param featureClass Feature class.
     * @param factory A [FeatureFactory] that will be used to create feature object.
     */
    fun <Feature, Flag> getFeature(featureClass: Class<Feature>, factory: FeatureFactory<Feature, Flag>): Feature

    /**
     * Waits for loading feature flags from [FeatureFlagDataSource] with provided key and then returns
     * feature object that will be created using updated feature flags config.
     *
     * @return Feature object.
     * @param featureClass Feature class.
     * @param dataSourceKey Key for [FeatureFlagDataSource].
     */
    suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, dataSourceKey: String): Feature

    /**
     * Waits for loading feature flags from at least one [FeatureFlagDataSource] with provided [FeatureFlagsSourceType]
     * and then returns feature object that will be created using updated feature flags config.
     *
     * @return Feature object.
     * @param featureClass Feature class.
     * @param sourceType A [FeatureFlagsSourceType].
     */
    suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, sourceType: FeatureFlagsSourceType): Feature

    /**
     * Waits for loading feature flags from all [FeatureFlagDataSource]s and then returns.
     *
     * @return [Unit].
     */
    suspend fun awaitFullLoading()

    /**
     * Starts feature flags fetching.
     */
    fun fetchFlags()

    /**
     * Clears all fetched feature flags and used feature flags cache.
     */
    fun resetAllFlags()

    /**
     * Clears used feature flags cache.
     */
    fun resetUsedFlags()

    /**
     * Shutdowns this [FeatureManager] instance. After shutdown instance can no longer be reused.
     */
    fun shutdown()

    /**
     * Returns set of [FeatureFlagDataSource]s keys from which flags loaded at least once.
     */
    fun availableDataSources(): Set<String>

    /**
     * Returns set of all known feature flags keys.
     */
    fun getAllKnownFlagsKeys(): Set<String>

    /**
     * Returns set of all known feature flags keys that feature flag is inherited from [SimpleFeatureFlag].
     */
    fun getAllKnownSimpleFlagsKeys(): Set<String>

    /**
     * A Builder that creates default [FeatureManager] instance.
     */
    class Builder(context: Context) {

        private lateinit var converter: FeatureFlagConverter
        private var flagRegistry: FeatureFlagRegistry = object: FeatureFlagRegistry {
            override fun getFeatureFlagsMap(): Map<String, Class<*>> = emptyMap()
            override fun getFeatureKeysMap(): Map<Class<*>, String> = emptyMap()
        }
        private var factoryRegistry: FeatureFactoryRegistry = object: FeatureFactoryRegistry {
            override fun getFactoryMap(): Map<Class<*>, Pair<String, Class<*>>> = emptyMap()
        }
        private var cachedFlagsStorage: CachedFlagsStorage = InternalCachedFlagsStorage(context)
        private var cachedFlagsPriority: Int = CacheDataSource.PRIORITY
        private var logger: Logger = LoggerStub()
        private val dataSources = mutableListOf<FeatureFlagDataSource>()

        fun converter(converter: FeatureFlagConverter) = apply {
            this.converter = converter
        }

        fun flagRegistry(flagRegistry: FeatureFlagRegistry) = apply {
            this.flagRegistry = flagRegistry
        }

        fun flagRegistry(vararg flags: Pair<String, Class<*>>) = apply {
            val flagsMap = flags.toMap()
            val keysMap = flagsMap.entries.associateBy({ it.value }) { it.key }
            flagRegistry = object : FeatureFlagRegistry {
                override fun getFeatureFlagsMap(): Map<String, Class<*>> = flagsMap
                override fun getFeatureKeysMap(): Map<Class<*>, String> = keysMap
            }
        }

        fun factoryRegistry(factoryRegistry: FeatureFactoryRegistry) = apply {
            this.factoryRegistry = factoryRegistry
        }

        fun factoryRegistry(vararg factories: Pair<Class<*>, Pair<String, Class<*>>>) = apply {
            val factoriesMap = factories.toMap()
            factoryRegistry = object: FeatureFactoryRegistry {
                override fun getFactoryMap(): Map<Class<*>, Pair<String, Class<*>>> = factoriesMap
            }
        }

        fun logger(logger: Logger) = apply {
            this.logger = logger
        }

        fun cachedFlagsStorage(storage: CachedFlagsStorage) = apply {
            cachedFlagsStorage = storage
        }

        fun cachedFlagsPriority(priority: Int) = apply {
            cachedFlagsPriority = priority
        }

        fun addDataSource(dataSource: FeatureFlagDataSource) = apply {
            dataSources.add(dataSource)
        }

        fun build(): FeatureManager {
            dataSources.add(CacheDataSource(storage = cachedFlagsStorage, priority = cachedFlagsPriority))
            val repository = RealFeatureRepository(dataSources, cachedFlagsStorage, flagRegistry, converter, logger)
            val inMemoryStorage = InMemoryFlagsStorage()
            val creator = RealFeatureCreator(flagRegistry, factoryRegistry, inMemoryStorage, logger)
            return RealFeatureManager(flagRegistry, repository, creator, inMemoryStorage, converter, logger)
        }
    }

}

inline fun <reified Feature> FeatureManager.getFeatureFlag(): Feature? = getFeatureFlag(Feature::class.java)

inline fun <reified Feature, Flag> FeatureManager.getFeature(factory: FeatureFactory<Feature, Flag>): Feature =
    getFeature(Feature::class.java, factory)

inline fun <reified Feature> FeatureManager.getFeature(): Feature = getFeature(Feature::class.java)

suspend inline fun <reified Feature> FeatureManager.awaitFeature(dataSourceKey: String): Feature =
    awaitFeature(Feature::class.java, dataSourceKey)

suspend inline fun <reified Feature> FeatureManager.awaitFeature(source: FeatureFlagsSourceType): Feature =
    awaitFeature(Feature::class.java, source)