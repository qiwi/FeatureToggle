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

import android.content.Context
import android.content.res.AssetManager
import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.converter.convertFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A [FeatureFlagDataSource] that loads feature flags from Json file stored in assets using [AssetManager].
 *
 * @property fileName Name of Json file in assets.
 * @property context Application [Context].
 * @property flagsArrayFieldName Name for field in Json file that represents array of feature flags objects.
 * @property key Unique key for this [FeatureFlagDataSource].
 * @property priority Priority for feature flags from this [FeatureFlagDataSource].
 */
class AssetsDataSource(
    private val fileName: String,
    private val context: Context,
    private val flagsArrayFieldName: String = FLAGS_ARRAY_FIELD_NAME,
    override val key: String = KEY,
    override val priority: Int = PRIORITY
) : FeatureFlagDataSource {

    override val sourceType: FeatureFlagsSourceType = FeatureFlagsSourceType.LOCAL

    @Suppress("UNCHECKED_CAST", "BlockingMethodInNonBlockingContext")
    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = flow {
        val content = context.assets.open(fileName).use { converter.read(it, Map::class.java) }
        val flags = content[flagsArrayFieldName] as List<Map<String, Any>>
        emit(flags.flatMap {
            it.mapNotNull { entry ->
                converter.convertFeatureFlag(entry.key, entry.value, key, registry, logger)
            }
        }.toMap())
    }

    companion object {
        /**
         * Default key for [AssetsDataSource].
         */
        const val KEY = "AssetsDataSource"

        /**
         * Default priority for feature flags from [AssetsDataSource].
         */
        const val PRIORITY = 1

        /**
         * Default name for Json object field that represents array of feature flags objects.
         */
        const val FLAGS_ARRAY_FIELD_NAME = "developerFlags"
    }
}