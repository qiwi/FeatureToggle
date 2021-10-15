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
import com.qiwi.featuretoggle.converter.convertFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [FeatureFlagDataSource] that loads feature flags from Json REST API using
 * [OkHttp](https://github.com/square/okhttp) library.
 *
 * Response Json must be in the following format:
 * `[
 *   {
 *     "feature": "android_info",
 *     "versionName": "12",
 *     "apiLevel": 31
 *   }
 * ]`.
 *
 * It is possible to customize object field that represents feature key (by default "feature"),
 * see [RemoteDataSource.keyField].
 *
 * @property url Json REST API url.
 * @property client An [OkHttpClient] instance.
 * @property retryCount Count of attempts to retry network request if it is failed.
 * @property featureKeyFieldName Name for Json object field that represents feature key.
 * @property key Unique key for this [FeatureFlagDataSource].
 * @property priority Priority for feature flags from this [FeatureFlagDataSource].
 */
class RemoteDataSource(
    private val url: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val retryCount: Long = RETRY_COUNT,
    private val featureKeyFieldName: String = FEATURE_KEY_FIELD_NAME,
    override val key: String = KEY,
    override val priority: Int = PRIORITY
) : FeatureFlagDataSource {

    override val sourceType: FeatureFlagsSourceType = FeatureFlagsSourceType.REMOTE

    @Suppress("UNCHECKED_CAST")
    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = flow {
        val json = fetchRemoteFlags()
        val flags = converter.read(json, List::class.java)
        emit(flags.mapNotNull { entry ->
            entry as Map<String, Any>
            val flagKey = entry[featureKeyFieldName] as String
            converter.convertFeatureFlag(flagKey, entry, key, registry, logger)
        }.toMap())
    }.retryWhen { _, attempt ->
        if (attempt < retryCount) {
            val counter = attempt + 1
            delay(counter * counter * 1000L)
            true
        } else {
            false
        }
    }

    private suspend fun fetchRemoteFlags(): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    continuation.resume(response.body!!.string())
                } else {
                    continuation.resumeWithException(OkHttpRequestException(response.code))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
        continuation.invokeOnCancellation {
            try {
                call.cancel()
            } catch (ex: Throwable) {
                //Ignore cancel exception
            }
        }
    }

    /**
     * Represents an exception caused by non 200 code in [Response].
     */
    class OkHttpRequestException(code: Int) : IOException("OkHttp response code $code")

    companion object {
        /**
         * Default key for [RemoteDataSource].
         */
        const val KEY = "RemoteDataSource"

        /**
         * Default priority for feature flags from [RemoteDataSource].
         */
        const val PRIORITY = 3

        /**
         * Default count of attempts to retry network request if it is failed.
         */
        const val RETRY_COUNT = 4L

        /**
         * Default name for Json object field that represents feature key.
         */
        const val FEATURE_KEY_FIELD_NAME = "feature"
    }
}