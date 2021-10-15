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

import app.cash.turbine.test
import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.creator.RealFeatureCreator
import com.qiwi.featuretoggle.model.FeatureFlagsContainer
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.storage.InMemoryFlagsStorage
import com.qiwi.featuretoggle.test.datasource.TestCachedDataSource
import com.qiwi.featuretoggle.test.datasource.TestLocalDataSource
import com.qiwi.featuretoggle.test.datasource.TestRemoteDataSource
import com.qiwi.featuretoggle.test.feature.BooleanFeature
import com.qiwi.featuretoggle.test.feature.StringFeature
import com.qiwi.featuretoggle.test.flag.BOOLEAN_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.BooleanFeatureFlag
import com.qiwi.featuretoggle.test.flag.STRING_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.StringFeatureFlag
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFactoryRegistry
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.repository.TestFeatureRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class FeatureManagerTest {

    private val flagRegistry = TestFlagRegistry()
    private val factoryRegistry = TestFactoryRegistry()
    private val logger = TestLogger()
    private val converter = JacksonFeatureFlagConverter()
    private val storage = InMemoryFlagsStorage()
    private val creator = RealFeatureCreator(flagRegistry, factoryRegistry, storage, logger)
    private val repository = TestFeatureRepository()

    private val testDispatcher = TestCoroutineDispatcher()

    private val featureManager = RealFeatureManager(
        flagRegistry,
        repository,
        creator,
        storage,
        converter,
        logger,
        testDispatcher
    )

    @Test
    fun returnsDefaultFeatureWhenFlagsNotLoaded() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        val feature = featureManager.getFeature(BooleanFeature::class.java)
        assertEquals(false, feature.isEnabled)
    }

    @Test
    fun returnsSameFeatureAfterFlagsChanged() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(
            booleanFeatureFlag = BooleanFeatureFlag().apply { isEnabled = false },
            stringFeatureFlag = StringFeatureFlag("Android", "KitKat"),
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals("Android KitKat", featureManager.getFeature(StringFeature::class.java).info)
        assertFalse(featureManager.getFeature(BooleanFeature::class.java).isEnabled)
        updateFlags(
            booleanFeatureFlag = BooleanFeatureFlag().apply { isEnabled = true },
            stringFeatureFlag = StringFeatureFlag("Android", "Lollipop"),
            sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.REMOTE),
            dataSources = setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY)
        )
        assertEquals("Android KitKat", featureManager.getFeature(StringFeature::class.java).info)
        assertFalse(featureManager.getFeature(BooleanFeature::class.java).isEnabled)
    }

    @Test
    fun returnsUpdatedFeatureAfterUsedFlagsReset() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(
            booleanFeatureFlag = BooleanFeatureFlag().apply { isEnabled = true },
            stringFeatureFlag = StringFeatureFlag("Android", "KitKat"),
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals("Android KitKat", featureManager.getFeature(StringFeature::class.java).info)
        assertTrue(featureManager.getFeature(BooleanFeature::class.java).isEnabled)
        updateFlags(
            booleanFeatureFlag = BooleanFeatureFlag().apply { isEnabled = true },
            stringFeatureFlag = StringFeatureFlag("Android", "Lollipop"),
            sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.REMOTE),
            dataSources = setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY)
        )
        featureManager.resetUsedFlags()
        assertEquals("Android Lollipop", featureManager.getFeature(StringFeature::class.java).info)
        assertTrue(featureManager.getFeature(BooleanFeature::class.java).isEnabled)
    }

    @Test
    fun returnsSameFlagsAfterFlagsChanged() = testDispatcher.runBlockingTest {
        val localStringFlag = StringFeatureFlag("Android", "Marshmallow")
        val localBooleanFlag = BooleanFeatureFlag().apply { isEnabled = false }
        val remoteStringFlag = StringFeatureFlag("Android", "Nougat")
        val remoteBooleanFlag = BooleanFeatureFlag().apply { isEnabled = true }
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(
            booleanFeatureFlag = localBooleanFlag,
            stringFeatureFlag = localStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals(localStringFlag, featureManager.getFeatureFlag(StringFeatureFlag::class.java))
        assertEquals(localBooleanFlag, featureManager.getFeatureFlag(BooleanFeatureFlag::class.java))
        updateFlags(
            booleanFeatureFlag = remoteBooleanFlag,
            stringFeatureFlag = remoteStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals(localStringFlag, featureManager.getFeatureFlag(StringFeatureFlag::class.java))
        assertEquals(localBooleanFlag, featureManager.getFeatureFlag(BooleanFeatureFlag::class.java))
    }

    @Test
    fun returnsUpdateFlagsAfterFlagsChanged() = testDispatcher.runBlockingTest {
        val localStringFlag = StringFeatureFlag("Android", "Marshmallow")
        val localBooleanFlag = BooleanFeatureFlag().apply { isEnabled = true }
        val remoteStringFlag = StringFeatureFlag("Android", "Nougat")
        val remoteBooleanFlag = BooleanFeatureFlag().apply { isEnabled = true }
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(
            booleanFeatureFlag = localBooleanFlag,
            stringFeatureFlag = localStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals(localStringFlag, featureManager.getFeatureFlag(StringFeatureFlag::class.java))
        assertEquals(localBooleanFlag, featureManager.getFeatureFlag(BooleanFeatureFlag::class.java))
        featureManager.resetUsedFlags()
        updateFlags(
            booleanFeatureFlag = remoteBooleanFlag,
            stringFeatureFlag = remoteStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertEquals(remoteStringFlag, featureManager.getFeatureFlag(StringFeatureFlag::class.java))
        assertEquals(remoteBooleanFlag, featureManager.getFeatureFlag(BooleanFeatureFlag::class.java))
    }

    @Test
    fun returnsFeatureWhenSpecifiedDataSourceProcessed() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY))
        flow {
            emit(featureManager.awaitFeature(BooleanFeature::class.java, TestRemoteDataSource.KEY))
        }.test {
            featureManager.fetchFlags()
            updateFlag(BooleanFeatureFlag().apply { isEnabled = false },
                sources = setOf(FeatureFlagsSourceType.LOCAL), dataSources = setOf(TestLocalDataSource.KEY))
            expectNoEvents()
            updateFlag(BooleanFeatureFlag().apply { isEnabled = true },
                sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.REMOTE),
                dataSources = setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
            assertTrue(awaitItem().isEnabled)
            awaitComplete()
        }
    }

    @Test
    fun returnsFeatureWhenOneOfSourceTypesProcessed() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY))
        flow {
            emit(featureManager.awaitFeature(BooleanFeature::class.java, FeatureFlagsSourceType.REMOTE))
        }.test {
            featureManager.fetchFlags()
            updateFlag(BooleanFeatureFlag().apply { isEnabled = false },
                sources = setOf(FeatureFlagsSourceType.LOCAL), dataSources = setOf(TestLocalDataSource.KEY))
            expectNoEvents()
            updateFlag(BooleanFeatureFlag().apply { isEnabled = true },
                sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.REMOTE),
                dataSources = setOf(TestLocalDataSource.KEY, TestRemoteDataSource.KEY))
            assertTrue(awaitItem().isEnabled)
            awaitComplete()
        }
    }

    @Test
    fun returnsAvailableDataSources() = testDispatcher.runBlockingTest {
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(sources = setOf(FeatureFlagsSourceType.LOCAL), dataSources = setOf(TestLocalDataSource.KEY))
        assertEquals(setOf(TestLocalDataSource.KEY), featureManager.availableDataSources())
        updateFlags(
            sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.CACHE, FeatureFlagsSourceType.REMOTE),
            dataSources = setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY)
        )
        assertEquals(
            setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY),
            featureManager.availableDataSources()
        )
    }

    @Test
    fun flagsSavedToCacheAfterFetchRemoteAndCachedFlags() = testDispatcher.runBlockingTest{
        val localStringFlag = StringFeatureFlag("Android", "Marshmallow")
        val localBooleanFlag = BooleanFeatureFlag().apply { isEnabled = false }
        val remoteStringFlag = StringFeatureFlag("Android", "Nougat")
        val remoteBooleanFlag = BooleanFeatureFlag().apply { isEnabled = true }
        repository.setDataSourceKeys(setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY))
        featureManager.fetchFlags()
        updateFlags(
            booleanFeatureFlag = localBooleanFlag,
            stringFeatureFlag = localStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL),
            dataSources = setOf(TestLocalDataSource.KEY)
        )
        assertTrue(repository.flagsCache.isEmpty())
        updateFlags(
            booleanFeatureFlag = localBooleanFlag,
            stringFeatureFlag = localStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.CACHE),
            dataSources = setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY)
        )
        assertTrue(repository.flagsCache.isEmpty())
        updateFlags(
            booleanFeatureFlag = remoteBooleanFlag,
            stringFeatureFlag = remoteStringFlag,
            sources = setOf(FeatureFlagsSourceType.LOCAL, FeatureFlagsSourceType.CACHE, FeatureFlagsSourceType.REMOTE),
            dataSources = setOf(TestLocalDataSource.KEY, TestCachedDataSource.KEY, TestRemoteDataSource.KEY)
        )
        assertEquals(
            mapOf(
                BOOLEAN_FEATURE_KEY to remoteBooleanFlag,
                STRING_FEATURE_KEY to remoteStringFlag
            ), repository.flagsCache
        )
    }

    private suspend fun updateFlag(booleanFeatureFlag: BooleanFeatureFlag,
                                   sources: Set<FeatureFlagsSourceType>, dataSources: Set<String>) {
        updateFlags(
            flags = mapOf(
                BOOLEAN_FEATURE_KEY to booleanFeatureFlag,
            ),
            sources = sources,
            dataSources = dataSources
        )
    }

    private suspend fun updateFlags(booleanFeatureFlag: BooleanFeatureFlag, stringFeatureFlag: StringFeatureFlag,
                                    sources: Set<FeatureFlagsSourceType>, dataSources: Set<String>) {
        updateFlags(
            flags = mapOf(
                BOOLEAN_FEATURE_KEY to booleanFeatureFlag,
                STRING_FEATURE_KEY to stringFeatureFlag
            ),
            sources = sources,
            dataSources = dataSources
        )
    }

    private suspend fun updateFlags(flags: Map<String, Any> = emptyMap(),
                                    sources: Set<FeatureFlagsSourceType>, dataSources: Set<String>) {
        repository.updateFlags(
            FeatureFlagsContainer(
                flags = flags,
                sourceTypes = sources,
                dataSourceKeys = dataSources
            )
        )
    }

}