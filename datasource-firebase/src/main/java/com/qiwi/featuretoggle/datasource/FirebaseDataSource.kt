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

import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.converter.readFeatureFlag
import com.qiwi.featuretoggle.model.FeatureFlagsSourceType
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.*

/**
 * A [FeatureFlagDataSource] that loads feature flags from
 * [Firebase Remote Config](https://firebase.google.com/docs/remote-config).
 *
 * @property fetchInterval Minimum fetch interval between remote config fetches.
 * @property remoteConfig A [FirebaseRemoteConfig] instance.
 * @property callbackExecutor An [Executor] on which fetch config task listener will be executed.
 * @property key Unique key for this [FeatureFlagDataSource].
 * @property priority Priority for feature flags from this [FeatureFlagDataSource].
 */
class FirebaseDataSource(
    private val fetchInterval: Long = FETCH_INTERVAL,
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig,
    private val callbackExecutor: Executor = TaskExecutors.MAIN_THREAD,
    override val key: String = KEY,
    override val priority: Int = PRIORITY
) : FeatureFlagDataSource {

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = fetchInterval
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    override val sourceType: FeatureFlagsSourceType = FeatureFlagsSourceType.REMOTE

    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = flow {
        fetchRemoteConfig()
        emit(remoteConfig.all.mapNotNull { entry ->
            converter.readFeatureFlag(entry.key, entry.value.asString(), key, registry, logger)
        }.toMap())
    }

    private suspend fun fetchRemoteConfig(): Unit = suspendCancellableCoroutine { continuation ->
        remoteConfig.fetchAndActivate().addOnCompleteListener(callbackExecutor) { task ->
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Task unsuccessful without error")
                )
            }
        }
    }

    companion object {
        /**
         * Default key for [FirebaseDataSource].
         */
        const val KEY = "FirebaseDataSource"

        /**
         * Default priority for feature flags from [AgConnectDataSource].
         */
        const val PRIORITY = 4

        /**
         * Default fetch interval (in seconds) between remote config fetches.
         */
        const val FETCH_INTERVAL = 900L
    }
}