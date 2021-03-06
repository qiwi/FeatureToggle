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

import app.cash.turbine.test
import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.test.flag.BOOLEAN_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.BooleanFeatureFlag
import com.qiwi.featuretoggle.test.flag.COMPLEX_BOOLEAN_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.ComplexBooleanFeatureFlag
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.util.TestResources
import com.qiwi.featuretoggle.test.value.allRemoteFlags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class DebugDataSourceTest {

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()
    private val testDispatcher = TestCoroutineDispatcher()

    private val dataSource = DebugDataSource(dispatcher = testDispatcher)

    @Test
    fun updatesSimpleFeatureFlag() = testDispatcher.runBlockingTest {
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(emptyMap<String, Any>(), awaitItem())
            dataSource.updateSimpleFeatureFlag(BOOLEAN_FEATURE_KEY, false)
            assertEquals(mapOf(BOOLEAN_FEATURE_KEY to BooleanFeatureFlag().apply {
                isEnabled = false
            }), awaitItem())
            dataSource.updateSimpleFeatureFlag(BOOLEAN_FEATURE_KEY, true)
            assertEquals(mapOf(BOOLEAN_FEATURE_KEY to BooleanFeatureFlag().apply {
                isEnabled = true
            }), awaitItem())
            dataSource.updateSimpleFeatureFlag(COMPLEX_BOOLEAN_FEATURE_KEY, false)
            assertEquals(mapOf(COMPLEX_BOOLEAN_FEATURE_KEY to ComplexBooleanFeatureFlag().apply {
                isEnabled = false
            }), awaitItem())
            dataSource.updateSimpleFeatureFlag(COMPLEX_BOOLEAN_FEATURE_KEY, true)
            assertEquals(mapOf(COMPLEX_BOOLEAN_FEATURE_KEY to ComplexBooleanFeatureFlag().apply {
                isEnabled = true
            }), awaitItem())
        }
    }

    @Test
    fun updatesFeatureFlagsFromJsonString() = testDispatcher.runBlockingTest {
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(emptyMap<String, Any>(), awaitItem())
            dataSource.updateFeatureFlagsFromJsonString("")
            assertEquals(emptyMap<String, Any>(), awaitItem())
            dataSource.updateFeatureFlagsFromJsonString(TestResources.getCachedFlagsJson())
            assertEquals(allRemoteFlags, awaitItem())
        }
    }
}