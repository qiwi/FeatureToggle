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
package com.qiwi.featuretoggle.storage

import com.qiwi.featuretoggle.FeatureManager
import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger

/**
 * Storage that stores feature flags in disk cache after loading from [FeatureFlagDataSource].
 *
 * It is useful to keep flags from data sources with type [FeatureFlagsSourceType.REMOTE] in cache,
 * because this flags are more actual then flags from data sources with type [FeatureFlagsSourceType.LOCAL].
 *
 * Feature flags that stored in this cache will be merged with other flags next time when [FeatureManager].
 * will fetch flags.
 */
interface CachedFlagsStorage {

    /**
     * @return Map where key is feature key and value is feature flag object.
     */
    suspend fun getFlags(converter: FeatureFlagConverter, registry: FeatureFlagRegistry, logger: Logger): Map<String, Any>

    /**
     * Stores feature flags to disk cache.
     *
     * @param flags Map where key is feature key and value is feature flag object.
     * @param converter A [FeatureFlagConverter] that can be used to convert feature flags objects into Json.
     * @param logger A [Logger] that can be used to log any events during saving flags to disk cache.
     */
    suspend fun saveFlags(flags: Map<String, Any>, converter: FeatureFlagConverter, logger: Logger)
}