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
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.flow.Flow

/**
 * Loads feature flags from specific source, such as local file or remote config.
 *
 * Each [FeatureFlagDataSource] must have unique key and specify type of its source
 * (eg local/remote/cache, see [FeatureFlagsSourceType]).
 */
interface FeatureFlagDataSource {

    /**
     * Loads feature flags from this DataSource.
     * DataSource can load and update feature flags config any time by emitting value into result [Flow].
     *
     * @return A [Flow] of [Map] where key is feature key and value is object that represents feature flag.
     *
     * @param registry A [FeatureFlagRegistry] that can be used to map feature flag key to feature flag class.
     * @param converter A [FeatureFlagConverter] that can be used to convert feature flag from Json string into object.
     * @param logger A [Logger] that can be used to log any events that will occur while loading feature flags.
     */
    fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>>

    /**
     * Unique key for this [FeatureFlagDataSource].
     */
    val key: String

    /**
     * Type of this [FeatureFlagDataSource].
     * @see [FeatureFlagsSourceType].
     */
    val sourceType: FeatureFlagsSourceType

    /**
     * Priority for feature flags from this [FeatureFlagDataSource].
     *
     * If multiple [FeatureFlagDataSource] return flag with same key,
     * flag from [FeatureFlagDataSource] with biggest priority will be used.
     */
    val priority: Int
}