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

import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.creator.FeatureCreator
import com.qiwi.featuretoggle.factory.FeatureFactory
import com.qiwi.featuretoggle.flag.SimpleFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsContainer
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.repository.FeatureRepository
import com.qiwi.featuretoggle.storage.ActualFlagsStorage
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [FeatureManager].
 */
internal class RealFeatureManager(
    private val flagRegistry: FeatureFlagRegistry,
    private val repository: FeatureRepository,
    private val creator: FeatureCreator,
    private val storage: ActualFlagsStorage,
    private val converter: FeatureFlagConverter,
    private val logger: Logger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FeatureManager {

    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher
            + CoroutineExceptionHandler { _, throwable -> logExceptionInsideCoroutineScope(throwable) })
    private val flagsSourcesState = MutableStateFlow(FlagsSourcesContainer())
    private var fetchFlagsJob: Job? = null
    private val saveEvents = Channel<Map<String, Any>>(capacity = CONFLATED)
    private val logEvents = Channel<FeatureFlagsContainer>(capacity = BUFFERED)

    init {
        startSaveEventsProcessing()
        startLogEventsProcessing()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Flag> getFeatureFlag(flagClass: Class<Flag>): Flag? {
        val flagKey = flagRegistry.getFeatureKeysMap()[flagClass]
            ?: throw IllegalStateException("Feature flag $flagClass not found in registry")
        return storage.getFlag(flagKey) as Flag?
    }

    override fun <Feature> getFeature(featureClass: Class<Feature>): Feature {
        return creator.createFeature(featureClass)
    }

    override fun <Feature, Flag> getFeature(featureClass: Class<Feature>, factory: FeatureFactory<Feature, Flag>): Feature {
        return creator.createFeature(featureClass, factory)
    }

    override suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, dataSourceKey: String): Feature {
        flagsSourcesState.first { container -> container.dataSourceKeys.contains(dataSourceKey) }
        return creator.createFeature(featureClass)
    }

    override suspend fun <Feature> awaitFeature(featureClass: Class<Feature>, sourceType: FeatureFlagsSourceType): Feature {
        flagsSourcesState.first { container -> container.sourceTypes.contains(sourceType) }
        return creator.createFeature(featureClass)
    }

    override suspend fun awaitFullLoading() {
        flagsSourcesState.first { container -> container.dataSourceKeys == repository.getDataSourceKeys() }
    }

    override fun fetchFlags() = synchronized(this) {
        if (fetchFlagsJob == null) {
            fetchFlagsJob = repository
                .getFlags()
                .flowOn(dispatcher)
                .onEach { container ->
                    processFlags(container)
                }
                .launchIn(coroutineScope)
        }
    }

    override fun resetAllFlags() = synchronized(this) {
        fetchFlagsJob?.cancel()
        fetchFlagsJob = null
        flagsSourcesState.value = FlagsSourcesContainer()
        storage.resetAllFlags()
    }

    override fun shutdown() {
        resetAllFlags()
        coroutineScope.cancel()
    }

    override fun resetUsedFlags() {
        storage.resetUsedFlags()
    }

    override fun availableDataSources(): Set<String> = flagsSourcesState.value.dataSourceKeys.toSortedSet()

    override fun getAllKnownFlagsKeys(): Set<String> = flagRegistry.getFeatureFlagsMap().keys.toSortedSet()

    override fun getAllKnownSimpleFlagsKeys(): Set<String> = flagRegistry.getFeatureFlagsMap()
        .filter { it.value.superclass == SimpleFeatureFlag::class.java }.map { it.key }
        .toSortedSet()

    private suspend fun processFlags(container: FeatureFlagsContainer) {
        if (container.flags.isNotEmpty()) {
            storage.updateFlags(container.flags, false)
        }
        flagsSourcesState.value = FlagsSourcesContainer(container.sourceTypes, container.dataSourceKeys)
        logEvents.send(container)
        if (container.sourceTypes.contains(FeatureFlagsSourceType.REMOTE)
            && container.sourceTypes.contains(FeatureFlagsSourceType.CACHE)
        ) {
            saveEvents.send(container.flags)
        }
    }

    private fun startSaveEventsProcessing() {
        saveEvents
            .consumeAsFlow()
            .onEach { flags ->
                runCatching {
                    repository.saveFlags(flags)
                }.onSuccess {
                    logger.log(
                        level = Logger.Level.INFO,
                        tag = TAG,
                        message = "flags saved to cache"
                    )
                }.onFailure { throwable ->
                    logger.log(
                        level = Logger.Level.ERROR,
                        tag = TAG,
                        message = "exception while saving flags to cache",
                        throwable = throwable
                    )
                }
            }
            .flowOn(dispatcher)
            .launchIn(coroutineScope)
    }

    private fun startLogEventsProcessing() {
        logEvents
            .consumeAsFlow()
            .onEach { container ->
                runCatching {
                    logger.log(
                        level = Logger.Level.INFO,
                        tag = TAG,
                        message = "flags loaded from data sources: [${container.dataSourceKeys.joinToString()}] ; " +
                                "actual flags: ${converter.write(container.flags)}"
                    )
                }
            }
            .flowOn(dispatcher)
            .launchIn(coroutineScope)
    }

    private fun logExceptionInsideCoroutineScope(throwable: Throwable) {
        logger.log(
            level = Logger.Level.ERROR,
            tag = TAG,
            message = "Exception inside CoroutineScope",
            throwable = throwable
        )
    }

    private data class FlagsSourcesContainer(
        val sourceTypes: Set<FeatureFlagsSourceType> = emptySet(),
        val dataSourceKeys: Set<String> = emptySet()
    )

    companion object {
        internal const val TAG = "FeatureManager"
    }

}