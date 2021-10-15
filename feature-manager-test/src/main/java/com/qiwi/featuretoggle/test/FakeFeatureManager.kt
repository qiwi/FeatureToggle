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
package com.qiwi.featuretoggle.test

import com.qiwi.featuretoggle.FeatureManager
import com.qiwi.featuretoggle.creator.FeatureCreator
import com.qiwi.featuretoggle.creator.RealFeatureCreator
import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.factory.FeatureFactory
import com.qiwi.featuretoggle.flag.SimpleFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.storage.ActualFlagsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.lang.reflect.ParameterizedType

/**
 * A [FeatureManager] implementation that do not load flags from data sources.
 * Instead it uses mocked flags that can be set via [FakeFeatureManager.overrideFlag]
 * and [FakeFeatureManager.overrideFlags] methods.
 *
 * Intended for use in unit tests.
 */
class FakeFeatureManager(private val flagRegistry: FeatureFlagRegistry,
                         private val storage: ActualFlagsStorage,
                         private val creator: FeatureCreator): FeatureManager {

    private val dataSourceKeys = MutableStateFlow<Set<String>>(emptySet())
    private val sourceTypes = MutableStateFlow<Set<FeatureFlagsSourceType>>(emptySet())
    private val overriddenFactoriesMap = mutableMapOf<Class<*>, FeatureFactory<Class<*>, Class<*>>>()

    @Suppress("UNCHECKED_CAST")
    override fun <Flag> getFeatureFlag(flagClass: Class<Flag>): Flag? {
        val flagKey = flagRegistry.getFeatureKeysMap()[flagClass]
            ?: throw IllegalStateException("Feature flag $flagClass not found in registry")
        return storage.getFlag(flagKey) as Flag?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Feature> getFeature(featureClass: Class<Feature>): Feature {
        val overriddenFactory = overriddenFactoriesMap[featureClass]
        return if(overriddenFactory != null) {
            creator.createFeature(featureClass, overriddenFactory as FeatureFactory<Feature, *>)
        }
        else {
            creator.createFeature(featureClass)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Feature, Flag> getFeature(featureClass: Class<Feature>, factory: FeatureFactory<Feature, Flag>): Feature {
        val overriddenFactory = overriddenFactoriesMap[featureClass]
        return if(overriddenFactory != null) {
            creator.createFeature(featureClass, overriddenFactory as FeatureFactory<Feature, Flag>)
        }
        else {
            creator.createFeature(featureClass, factory)
        }
    }

    override suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, dataSourceKey: String): Feature {
        dataSourceKeys.first { it.contains(dataSourceKey) }
        return getFeature(featureClass)
    }

    override suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, sourceType: FeatureFlagsSourceType): Feature {
        sourceTypes.first { it.contains(sourceType) }
        return getFeature(featureClass)
    }

    override suspend fun awaitFullLoading() {}

    override fun fetchFlags() {}

    override fun resetAllFlags() {
        storage.resetAllFlags()
    }

    override fun resetUsedFlags() {
        storage.resetUsedFlags()
    }

    override fun shutdown() {}

    override fun availableDataSources(): Set<String> = dataSourceKeys.value.toSortedSet()

    override fun getAllKnownFlagsKeys(): Set<String> =
        flagRegistry.getFeatureFlagsMap().keys.toSortedSet()

    override fun getAllKnownSimpleFlagsKeys(): Set<String> = flagRegistry.getFeatureFlagsMap()
        .filter { it.value.superclass == SimpleFeatureFlag::class.java }.map { it.key }
        .toSortedSet()

    /**
     * Sets mocked [FeatureFlagDataSource] keys. [FakeFeatureManager] will behave as if flags from this
     * [FeatureFlagDataSource]s have been loaded.
     *
     * @param dataSourcesKeys [FeatureFlagDataSource] keys.
     */
    fun overrideAvailableDataSourcesKeys(dataSourcesKeys: Set<String>) {
        dataSourceKeys.value = dataSourcesKeys
    }

    /**
     * Sets mocked [FeatureFlagsSourceType]s. [FakeFeatureManager] will behave as if flags from this
     * [FeatureFlagsSourceType]s have been loaded.
     *
     * @param sourceTypes [FeatureFlagsSourceType]s.
     */
    fun overrideSourceTypes(sourceTypes: Set<FeatureFlagsSourceType>) {
        this.sourceTypes.value = sourceTypes
    }

    /**
     * Overrides feature flag. [FakeFeatureManager] will use this mocked flag to create features.
     *
     * @param flag Mocked feature flag object.
     */
    fun overrideFlag(flag: Any) {
        overrideFlags(mapOf(getKey(flag) to flag))
    }

    /**
     * Overrides multiple feature flags. [FakeFeatureManager] will use this mocked flags to create features.
     *
     * @param flags List of mocked feature flags objects.
     */
    fun overrideFlags(flags: List<Any>) {
        overrideFlags(flags.map { flag ->
            getKey(flag) to flag
        }.toMap())
    }

    /**
     * Overrides multiple feature flags. [FakeFeatureManager] will use this mocked flags to create features.
     *
     * @param flags Map where key is feature key and value is mocked feature flag object.
     */
    fun overrideFlags(flags: Map<String, Any>) {
        storage.updateFlags(flags, force = true)
    }

    /**
     * Overrides feature factory. [FakeFeatureManager] will use this mocked factory to create features.
     *
     * @param factory Mocked [FeatureFactory].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T, F> overrideFactory(factory: FeatureFactory<T, F>) {
        val featureClass = (factory::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>
        overriddenFactoriesMap[featureClass] = factory as FeatureFactory<Class<*>, Class<*>>
    }

    private fun getKey(flag: Any): String {
        return flagRegistry.getFeatureKeysMap()[flag::class.java] ?: throw RuntimeException("Key not found in registry")
    }

    companion object {

        /**
         * Creates instance of [FakeFeatureManager] with provided registries.
         *
         * @param flagRegistry A [FeatureFlagRegistry].
         * @param factoryRegistry A [FeatureFactoryRegistry].
         */
        @JvmStatic
        fun create(flagRegistry: FeatureFlagRegistry, factoryRegistry: FeatureFactoryRegistry): FakeFeatureManager {
            val logger = ConsoleLogger()
            val storage = SimpleFlagsStorage()
            val creator = RealFeatureCreator(flagRegistry, factoryRegistry, storage, logger)
            return FakeFeatureManager(flagRegistry, storage, creator)
        }
    }

}