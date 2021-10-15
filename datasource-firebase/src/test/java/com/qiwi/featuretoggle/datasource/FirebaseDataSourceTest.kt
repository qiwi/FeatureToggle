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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.test.flag.BOOLEAN_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.COMPLEX_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.DOUBLE_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.STRING_FEATURE_KEY
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.util.ImmediateExecutor
import com.qiwi.featuretoggle.test.util.TestResources
import com.qiwi.featuretoggle.test.value.allRemoteFlags
import com.qiwi.featuretoggle.test.value.complexFlagRemote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class FirebaseDataSourceTest {

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()

    private val testExecutor = ImmediateExecutor()

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var dataSource: FirebaseDataSource

    @Before
    fun before() {
        remoteConfig = mock(FirebaseRemoteConfig::class.java)
        dataSource = FirebaseDataSource(remoteConfig = remoteConfig, callbackExecutor = testExecutor)
    }

    @Test
    fun returnsFlowOfFirebaseFlagsIfFetchedSuccessfully() = runBlockingTest {
        mockConfigValues(
            mapOf(
                BOOLEAN_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteBooleanFlagJson()),
                DOUBLE_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteDoubleFlagJson()),
                STRING_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteStringFlagJson()),
                COMPLEX_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteComplexFlagJson())
            )
        )
        mockFetchTask(Tasks.call(testExecutor) { true })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(allRemoteFlags, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun returnFlowWithThrowableIfFirebaseFlagsFetchFailed() = runBlockingTest {
        val exception = IOException("network failure")
        mockConfigValues(
            mapOf(
                BOOLEAN_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteBooleanFlagJson()),
                DOUBLE_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteDoubleFlagJson()),
                STRING_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteStringFlagJson()),
                COMPLEX_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteComplexFlagJson())
            )
        )
        mockFetchTask(Tasks.call(testExecutor) { throw exception })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(exception.message, awaitError().message)
        }
    }

    @Test
    fun returnsFlowOfOnlySuccessfullyParsedFirebaseFlags() = runBlockingTest {
        mockConfigValues(
            mapOf(
                STRING_FEATURE_KEY to FirebaseRemoteConfigValueMock("{ \"test\": true }"),
                COMPLEX_FEATURE_KEY to FirebaseRemoteConfigValueMock(TestResources.getRemoteComplexFlagJson())
            )
        )
        mockFetchTask(Tasks.call(testExecutor) { true })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(mapOf(COMPLEX_FEATURE_KEY to complexFlagRemote), awaitItem())
            awaitComplete()
        }
    }

    private fun mockConfigValues(values: Map<String, FirebaseRemoteConfigValue>) {
        doReturn(values).`when`(remoteConfig).all
    }

    private fun mockFetchTask(task: Task<Boolean>) {
        doReturn(task).`when`(remoteConfig).fetchAndActivate()
    }

    class FirebaseRemoteConfigValueMock(private val value: String) : FirebaseRemoteConfigValue {

        override fun asLong(): Long = value.toLong()

        override fun asDouble(): Double = value.toDouble()

        override fun asString(): String = value

        override fun asByteArray(): ByteArray = value.toByteArray()

        override fun asBoolean(): Boolean = value.toBoolean()

        override fun getSource(): Int = VALUE_SOURCE_REMOTE

    }
}