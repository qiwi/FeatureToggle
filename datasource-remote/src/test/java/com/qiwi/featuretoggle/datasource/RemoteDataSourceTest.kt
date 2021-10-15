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
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.util.ImmediateExecutorService
import com.qiwi.featuretoggle.test.util.TestResources
import com.qiwi.featuretoggle.test.value.allRemoteFlags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class RemoteDataSourceTest {

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()

    private lateinit var server: MockWebServer
    private lateinit var serverDispatcher: TestServerDispatcher

    private lateinit var client: OkHttpClient
    private lateinit var dataSource: RemoteDataSource

    private lateinit var testDispatcher: TestCoroutineDispatcher

    @Before
    fun before() {
        serverDispatcher = TestServerDispatcher(TestResources.getRemoteFlagsJson())

        server = MockWebServer()
        server.dispatcher = serverDispatcher
        server.start()

        client = OkHttpClient.Builder().dispatcher(okhttp3.Dispatcher(ImmediateExecutorService())).build()

        dataSource = RemoteDataSource(url = server.url("feature_flags.json").toString(), client = client)

        testDispatcher = TestCoroutineDispatcher()
    }

    @After
    fun after() {
        testDispatcher.cleanupTestCoroutines()
        server.shutdown()
    }

    @Test
    fun returnsFlowOfRemoteFlagsIfRequestWasSuccessful() = testDispatcher.runBlockingTest {
        dataSource.getFlags(registry, converter, logger).flowOn(testDispatcher).test {
            assertEquals(allRemoteFlags, awaitItem())
            awaitComplete()
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun retriesRequestWhenItWasFailed() = testDispatcher.runBlockingTest {
        serverDispatcher.serverFailure = true
        dataSource.getFlags(registry, converter, logger).flowOn(testDispatcher).test {
            advanceTimeBy(1000L)
            assertEquals(2, server.requestCount)
            advanceTimeBy(4000L)
            assertEquals(3, server.requestCount)
            advanceTimeBy(9000L)
            assertEquals(4, server.requestCount)
            advanceTimeBy(16000L)
            assertEquals(5, server.requestCount)
            awaitError()
        }
    }

    @Test
    fun returnsFlowOfRemoteFlagsAfterSuccessfulRetry() = testDispatcher.runBlockingTest {
        serverDispatcher.serverFailure = true
        dataSource.getFlags(registry, converter, logger).flowOn(testDispatcher).test {
            advanceTimeBy(1000L)
            serverDispatcher.serverFailure = false
            advanceTimeBy(4000L)
            awaitItem()
            awaitComplete()
        }
    }

    class TestServerDispatcher(var responseBody: String,
                               var serverFailure: Boolean = false): Dispatcher() {


        override fun dispatch(request: RecordedRequest): MockResponse {
            return if(serverFailure) {
                MockResponse()
                    .setBody("")
                    .setResponseCode(500)
            }
            else {
                MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
            }
        }
    }
}