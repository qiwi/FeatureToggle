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

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.model.FeatureFlagsContainer
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.test.datasource.TestCachedDataSource
import com.qiwi.featuretoggle.test.datasource.TestLocalDataSource
import com.qiwi.featuretoggle.test.datasource.TestRemoteConfigDataSource
import com.qiwi.featuretoggle.test.datasource.TestRemoteDataSource
import com.qiwi.featuretoggle.test.flag.STRING_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.StringFeatureFlag
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.storage.TestCachedFlagsStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class FeatureRepositoryTest {

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()

    private val remoteDataSource = TestRemoteDataSource()
    private val localDataSource = TestLocalDataSource()
    private val cachedDataSource = TestCachedDataSource()
    private val remoteConfigDataSource = TestRemoteConfigDataSource()

    private val storage = TestCachedFlagsStorage()

    private lateinit var repository: RealFeatureRepository

    @Test
    fun emitsFlagsMergedWithPriority() = runBlockingTest {
        val dataSources = listOf(remoteDataSource, localDataSource, cachedDataSource)
        repository = RealFeatureRepository(dataSources, storage, registry, converter, logger)
        repository.getFlags().test {
            localDataSource.updateFlags(
                mapOf(
                    STRING_FEATURE_KEY to StringFeatureFlag(
                        "Android",
                        "10"
                    )
                ), final = true
            )
            cachedDataSource.updateFlags(
                mapOf(
                    STRING_FEATURE_KEY to StringFeatureFlag(
                        "Android",
                        "11"
                    )
                ), final = true
            )
            assertInitialValue()
            assertEquals(
                FeatureFlagsContainer(
                    mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "10")),
                    setOf(FeatureFlagsSourceType.LOCAL), setOf(TestLocalDataSource.KEY)
                ), awaitItem()
            )
            assertEquals(
                FeatureFlagsContainer(
                    mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "11")),
                    setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.CACHE),
                    setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY)
                ), awaitItem()
            )
            remoteDataSource.updateFlags(
                mapOf(
                    STRING_FEATURE_KEY to StringFeatureFlag(
                        "Android",
                        "12"
                    )
                ), final = true
            )
            assertEquals(
                FeatureFlagsContainer(
                    mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "12")),
                    setOf(
                        FeatureFlagsSourceType.LOCAL,
                        FeatureFlagsSourceType.CACHE,
                        FeatureFlagsSourceType.REMOTE
                    ),
                    setOf(
                        TestLocalDataSource.KEY,
                        TestCachedDataSource.KEY,
                        TestRemoteDataSource.KEY
                    )
                ), awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun dataSourceErrorHandled() = runBlockingTest {
        val dataSources = listOf(localDataSource, remoteDataSource)
        repository = RealFeatureRepository(dataSources, storage, registry, converter, logger)
        repository.getFlags().test {
            localDataSource.updateFlags(
                mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "10")),
                final = true
            )
            remoteDataSource.throwError(IOException("no connection"))
            assertInitialValue()
            assertEquals(
                FeatureFlagsContainer(
                    mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "10")),
                    setOf(FeatureFlagsSourceType.LOCAL),
                    setOf(TestLocalDataSource.KEY)
                ), awaitItem()
            )
            assertEquals(
                FeatureFlagsContainer(
                    mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "10")),
                    setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.REMOTE),
                    setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY)
                ), awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun allDataSourceKeysPresent() {
        val dataSources = listOf(localDataSource, cachedDataSource, remoteDataSource, remoteConfigDataSource)
        repository = RealFeatureRepository(dataSources, storage, registry, converter, logger)
        assertEquals(
            setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY, TestRemoteConfigDataSource.KEY),
            repository.getDataSourceKeys()
        )
    }

    private suspend fun FlowTurbine<FeatureFlagsContainer>.assertInitialValue() {
        assertEquals(FeatureFlagsContainer(), awaitItem())
    }
}