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
package com.qiwi.featuretoggle.repository

import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.model.FeatureFlagsContainer
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.storage.CachedFlagsStorage
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [FeatureRepository].
 */
internal class RealFeatureRepository(
    private val dataSources: List<FeatureFlagDataSource>,
    private val cachedFlagsStorage: CachedFlagsStorage,
    private val flagRegistry: FeatureFlagRegistry,
    private val converter: FeatureFlagConverter,
    private val logger: Logger
): FeatureRepository {

    override fun getFlags(): Flow<FeatureFlagsContainer> {
        val allSources = dataSources.map { dataSource ->
            dataSource.getFlags(flagRegistry, converter, logger)
                .map<Map<String, Any>, PrioritizedFlags?> { flags ->
                    PrioritizedFlags(
                        flags,
                        dataSource.sourceType,
                        dataSource.priority,
                        dataSource.key
                    )
                }.onStart {
                    emit(null)
                }.catch {
                    emit(
                        PrioritizedFlags(
                            emptyMap(),
                            dataSource.sourceType,
                            dataSource.priority,
                            dataSource.key
                        )
                    )
                }
        }
        return combine(allSources) { prioritizedFlags ->
            val presentFlags = prioritizedFlags
                .filterNotNull()
            val actualFlags = presentFlags
                .sortedBy { pFlags -> pFlags.priority }
                .flatMap { pFlags -> pFlags.flags.map { it.key to it.value } }
                .toMap()
            val sources = presentFlags.map { it.source }.toSet()
            val keys = presentFlags.map { it.key }.toSet()
            FeatureFlagsContainer(actualFlags, sources, keys)
        }
    }

    override fun getDataSourceKeys(): Set<String> = dataSources.map { it.key }.toSet()

    override suspend fun saveFlags(flags: Map<String, Any>) {
        cachedFlagsStorage.saveFlags(flags, converter, logger)
    }

    private class PrioritizedFlags(
        val flags: Map<String, Any>,
        val source: FeatureFlagsSourceType,
        val priority: Int,
        val key: String
    )
}