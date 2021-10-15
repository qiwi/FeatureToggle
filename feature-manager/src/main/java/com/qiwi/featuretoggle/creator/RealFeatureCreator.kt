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
package com.qiwi.featuretoggle.creator

import com.qiwi.featuretoggle.factory.FeatureFactory
import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.storage.ActualFlagsStorage
import com.qiwi.featuretoggle.util.Logger
import java.lang.reflect.ParameterizedType

/**
 * Default implementation of [FeatureCreator].
 *
 * @property flagRegistry [FeatureFlagRegistry] for obtaining feature key for the specific feature.
 * @property factoryRegistry [FeatureFactoryRegistry] for obtaining [FeatureFactory] for the specific feature.
 * @property storage [ActualFlagsStorage] for retrieving actual feature flag object for the specific feature.
 * @property logger A [Logger] for logging warnings and exceptions.
 */
class RealFeatureCreator(
    private val flagRegistry: FeatureFlagRegistry,
    private val factoryRegistry: FeatureFactoryRegistry,
    private val storage: ActualFlagsStorage,
    private val logger: Logger
) : FeatureCreator {

    @Suppress("UNCHECKED_CAST")
    override fun <Feature> createFeature(featureClass: Class<Feature>): Feature {
        //Find Pair of key and factory class in registry using provided feature class.
        val factoryWithKey = factoryRegistry.getFactoryMap()[featureClass]
            ?: throw getFeatureCreatorException(featureClass.simpleName)

        //Obtain feature key.
        val featureKey = factoryWithKey.first

        // Create instance of factory class using its first constructor.
        val factory = factoryWithKey.second.constructors.first().newInstance() as FeatureFactory<Feature, Any>
        return createFeature(featureKey, factory)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Feature, Flag> createFeature(featureClass: Class<Feature>, factory: FeatureFactory<Feature, Flag>): Feature {
        //Because factory is already provided, we can use its generic type parameter to get feature flag class.
        val featureFlagClass = (factory::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<Feature>

        //Obtain feature key using reversed map in flag registry.
        val featureKey = flagRegistry.getFeatureKeysMap()[featureFlagClass] ?: throw getFeatureCreatorException(featureClass.simpleName)

        return createFeature(featureKey, factory)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Feature, Flag> createFeature(featureKey: String, factory: FeatureFactory<Feature, Flag>): Feature {
        //First we need to find actual feature flag object using key and cast it to Flag
        val featureFlag = storage.getFlag(featureKey) as Flag

        return if(featureFlag != null) {
            factory.createFeature(featureFlag)
        } else {
            //If feature flag is null we can ask to create feature using its default implementation.
            factory.createDefault().also {
                logDefaultFlagWarning(featureKey)
            }
        }
    }

    private fun getFeatureCreatorException(className: String): RuntimeException {
        val exception = RuntimeException("No factory for feature $className found in registry")
        logger.log(level = Logger.Level.ERROR, tag = "FeatureCreatorException",
            message = "No factory for feature $className found in registry", throwable = exception)
        return exception
    }

    private fun logDefaultFlagWarning(featureKey: String) {
        logger.log(level = Logger.Level.WARN, tag = "FeatureCreatorProd",
            message = "requested feature but flag is empty", details = mapOf("featureKey" to featureKey))
    }
}