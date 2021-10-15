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
import com.huawei.agconnect.remoteconfig.AGConnectConfig
import com.huawei.agconnect.remoteconfig.ConfigValues
import com.huawei.hmf.tasks.Task
import com.huawei.hmf.tasks.Tasks
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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class AgConnectDataSourceTest {

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()

    private val testExecutor = ImmediateExecutor()

    private lateinit var config: AGConnectConfig
    private lateinit var dataSource: AgConnectDataSource

    @Before
    fun before() {
        config = Mockito.mock(AGConnectConfig::class.java)

        dataSource = AgConnectDataSource(config = config, callbackExecutor = testExecutor)

        doAnswer { answer ->
            val configValues = answer.getArgument<ConfigValuesMock>(0).values
            doReturn(configValues).`when`(config).mergedAll
        }.`when`(config).apply(any())

        doReturn(emptyMap<String, Any>()).`when`(config).mergedAll
        doReturn(ConfigValuesMock()).`when`(config).loadLastFetched()
    }

    @Test
    fun returnsFlowOfAgConnectFlagsIfFetchedSuccessfully() = runBlockingTest {
        mockFetchTask(Tasks.call {
            ConfigValuesMock(
                mapOf(
                    BOOLEAN_FEATURE_KEY to TestResources.getRemoteBooleanFlagJson(),
                    DOUBLE_FEATURE_KEY to TestResources.getRemoteDoubleFlagJson(),
                    STRING_FEATURE_KEY to TestResources.getRemoteStringFlagJson(),
                    COMPLEX_FEATURE_KEY to TestResources.getRemoteComplexFlagJson()
                )
            )
        })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(allRemoteFlags, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun returnFlowWithThrowableIfAgConnectFlagsFetchFailed() = runBlockingTest {
        val exception = IOException("network failure")
        mockFetchTask(Tasks.call { throw exception })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(exception.message, awaitError().message)
        }
    }

    @Test
    fun returnsFlowOfOnlySuccessfullyParsedAgConnectFlags() = runBlockingTest {
        mockFetchTask(Tasks.call {
            ConfigValuesMock(
                mapOf(
                    STRING_FEATURE_KEY to "{ \"test\": true }",
                    COMPLEX_FEATURE_KEY to TestResources.getRemoteComplexFlagJson()
                )
            )
        })
        dataSource.getFlags(registry, converter, logger).test {
            assertEquals(mapOf(COMPLEX_FEATURE_KEY to complexFlagRemote), awaitItem())
            awaitComplete()
        }
    }

    private fun mockFetchTask(task: Task<ConfigValues>) {
        doReturn(task).`when`(config).fetch(anyLong())
    }

    private class ConfigValuesMock(val values: Map<String, Any> = emptyMap()) : ConfigValues {

        override fun containKey(key: String): Boolean = values.containsKey(key)

        override fun getValueAsBoolean(key: String): Boolean = values[key].toString().toBoolean()

        override fun getValueAsDouble(key: String): Double = values[key].toString().toDouble()

        override fun getValueAsLong(key: String): Long = values[key].toString().toLong()

        override fun getValueAsString(key: String): String = values[key].toString()

        override fun getValueAsByteArray(key: String): ByteArray = values[key].toString().toByteArray()

    }

}