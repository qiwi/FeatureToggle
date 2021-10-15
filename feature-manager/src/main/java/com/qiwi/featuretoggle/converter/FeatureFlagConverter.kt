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
package com.qiwi.featuretoggle.converter

import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import java.io.InputStream

/**
 * Converts Json into feature flag objects.
 */
interface FeatureFlagConverter {

    /**
     * Deserializes Json from [InputStream] into an object of the specified class.
     *
     * @param source Source Json [InputStream].
     * @param type Specific class.
     */
    fun <T> read(source: InputStream, type: Class<T>): T

    /**
     * Deserializes Json from string into an object of the specified class.
     *
     * @param content Source Json string.
     * @param type Specific class.
     */
    fun <T> read(content: String, type: Class<T>): T

    /**
     * Serializes an object into string.
     *
     * @param value Source object.
     */
    fun <T> write(value: T): String

    /**
     * Converts an object that represents Json object into an object of the specific class.
     *
     * @param value Json object.
     * @param toType Specific class.
     */
    fun <T> convert(value: Any, toType: Class<T>): T
}

/**
 * Converts feature flag an Json object into Pair of feature key and feature flag object.
 *
 * @param flagKey Feature key.
 * @param flagValue Feature flag value.
 * @param sourceKey Key of the [FeatureFlagDataSource] that converts feature flag.
 * @param registry A [FeatureFlagRegistry] that will be used to map feature flag key to object that represents feature flag.
 * @param logger A [Logger] that will be used to log possible exception.
 */
fun FeatureFlagConverter.convertFeatureFlag(
    flagKey: String,
    flagValue: Any,
    sourceKey: String,
    registry: FeatureFlagRegistry,
    logger: Logger
): Pair<String, Any>? {
    val flagClass = registry.getFeatureFlagsMap()[flagKey]
    return if (flagClass != null) {
        try {
            Pair(flagKey, convert(flagValue, flagClass))
        } catch (e: Throwable) {
            logger.logConverterException(sourceKey, flagKey, e)
            null
        }
    } else {
        null
    }
}

/**
 * Reads feature flag an Json string into Pair of feature key and feature flag object.
 *
 * @param flagKey Feature key.
 * @param flagValue Feature flag value.
 * @param sourceKey Key of the [FeatureFlagDataSource] that converts feature flag.
 * @param registry A [FeatureFlagRegistry] that will be used to map feature flag key to object that represents feature flag.
 * @param logger A [Logger] that will be used to log possible exception.
 */
fun FeatureFlagConverter.readFeatureFlag(
    flagKey: String,
    flagValue: String,
    sourceKey: String,
    registry: FeatureFlagRegistry,
    logger: Logger
): Pair<String, Any>? {
    val flagClass = registry.getFeatureFlagsMap()[flagKey]
    return if (flagClass != null) {
        try {
            Pair(flagKey, read(flagValue, flagClass))
        } catch (e: Throwable) {
            logger.logConverterException(sourceKey, flagKey, e)
            null
        }
    } else {
        null
    }
}

private fun Logger.logConverterException(sourceKey: String, flagKey: String, throwable: Throwable) {
    log(
        level = Logger.Level.WARN,
        tag = sourceKey,
        message = "Exception while parsing feature flag to object",
        details = mapOf("flagKey" to flagKey),
        throwable = throwable
    )
}