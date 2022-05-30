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
package com.qiwi.featuretoggle.datasource

import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.converter.convertFeatureFlag
import com.qiwi.featuretoggle.flag.SimpleFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.*

/**
 * A [FeatureFlagDataSource] that can be updated directly any time. Its purpose is to update
 * feature flags manually in debug builds.
 *
 * @property isEnabled If true, datasource will be alive and can be updated. Otherwise, it will completes immediately.
 * @property dispatcher A [CoroutineDispatcher] for schedule feature flags updates.
 * @property key Unique key for this [FeatureFlagDataSource].
 * @property priority Priority for feature flags from this [FeatureFlagDataSource].
 */
class DebugDataSource(private val isEnabled: Boolean = true,
                      private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
                      override val key: String = KEY,
                      override val priority: Int = PRIORITY): FeatureFlagDataSource {

    override val sourceType: FeatureFlagsSourceType = FeatureFlagsSourceType.LOCAL

    //Lazy properties to prevent unnecessary initialization if datasource is disabled.
    //Because we need to process all coming update requests, we are creating BUFFERED channel.
    private val events: Channel<FeatureFlagsUpdateEvent> by lazy { Channel(capacity = BUFFERED) }
    //Sending to BUFFERED channel may suspend execution. We need coroutine scope to launch suspend functions.
    private val scope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + dispatcher) }

    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = if(isEnabled) consumeEvents(registry, converter, logger) else flowOf(emptyMap())

    private fun consumeEvents(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> {
        return events.consumeAsFlow().map { event ->
            when (event) {
                is FeatureFlagsUpdateEvent.UpdateSimpleFlag -> convertSimpleFeatureFlag(
                    event.key,
                    event.isEnabled,
                    registry
                )
                is FeatureFlagsUpdateEvent.UpdateFromJsonString -> convertFeatureFlags(
                    event.content,
                    registry,
                    converter,
                    logger
                )
            }
        }.onStart {
            emit(emptyMap())
        }
    }

    private fun convertSimpleFeatureFlag(
        key: String,
        isEnabled: Boolean,
        registry: FeatureFlagRegistry
    ): Map<String, Any> {
        val flagClass = registry.getFeatureFlagsMap()[key]
        val hasSimpleFeatureFlagAsSuperclass = flagClass?.superclasses
            ?.any { it == SimpleFeatureFlag::class.java }
            ?: false
        return if (flagClass != null && hasSimpleFeatureFlagAsSuperclass) {
            val featureFlag = flagClass.constructors.first().newInstance() as SimpleFeatureFlag
            featureFlag.isEnabled = isEnabled
            mapOf(key to featureFlag)
        } else {
            emptyMap()
        }
    }

    private fun convertFeatureFlags(
        content: String,
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Map<String, Any> = try {
        val flags = converter.read(content, Map::class.java)
        flags.mapNotNull { entry ->
            converter.convertFeatureFlag(
                entry.key as String,
                entry.value as Any,
                KEY,
                registry,
                logger
            )
        }.toMap()
    } catch (e: Exception) {
        emptyMap()
    }

    /**
     * Updates [SimpleFeatureFlag].
     *
     * @param key key for [SimpleFeatureFlag].
     * @param isEnabled [SimpleFeatureFlag.isEnabled] property value
     */
    fun updateSimpleFeatureFlag(key: String, isEnabled: Boolean) {
        scope.launch {
            events.send(FeatureFlagsUpdateEvent.UpdateSimpleFlag(key, isEnabled))
        }
    }

    /**
     * Updates feature flags from json string. Json must have the following structure:
     * { "flagKeyOne":{"flagValue":"value"}, "flagKeyTwo":{"flagValue":"value"}}.
     *
     * @param content Json string
     */
    fun updateFeatureFlagsFromJsonString(content: String) {
        scope.launch {
            events.send(FeatureFlagsUpdateEvent.UpdateFromJsonString(content))
        }
    }

    companion object {
        /**
         * Default key for [DebugDataSource].
         */
        const val KEY = "DebugDataSource"

        /**
         * Default priority for feature flags from [DebugDataSource].
         */
        const val PRIORITY = 100
    }

    private sealed class FeatureFlagsUpdateEvent {
        data class UpdateSimpleFlag(val key: String, val isEnabled: Boolean): FeatureFlagsUpdateEvent()
        data class UpdateFromJsonString(val content: String): FeatureFlagsUpdateEvent()
    }

    private val Class<*>.superclasses: List<Class<*>>
        get() {
            val classesList = mutableListOf<Class<*>>()
            var currentSuperclass = superclass
            while (currentSuperclass != null) {
                classesList.add(currentSuperclass)
                currentSuperclass = currentSuperclass.superclass
            }
            return classesList
        }
}