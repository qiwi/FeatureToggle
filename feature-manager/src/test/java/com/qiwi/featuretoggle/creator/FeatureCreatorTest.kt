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
import com.qiwi.featuretoggle.storage.InMemoryFlagsStorage
import com.qiwi.featuretoggle.test.feature.StringFeature
import com.qiwi.featuretoggle.test.flag.STRING_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.StringFeatureFlag
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFactoryRegistry
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import org.junit.Assert.*
import org.junit.Test

class FeatureCreatorTest {

    private val flagRegistry = TestFlagRegistry()
    private val factoryRegistry = TestFactoryRegistry()
    private val storage = InMemoryFlagsStorage()
    private val logger = TestLogger()
    private val creator = RealFeatureCreator(flagRegistry, factoryRegistry, storage, logger)

    @Test
    fun createsFeatureWithFlag() {
        storage.updateFlags(mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "Lollipop")))
        val feature = creator.createFeature(StringFeature::class.java)
        assertEquals("Android Lollipop", feature.info)
    }

    @Test
    fun createsFeatureWithFlagAndFactory() {
        storage.updateFlags(mapOf(STRING_FEATURE_KEY to StringFeatureFlag("Android", "Lollipop")))
        val customFactory = object : FeatureFactory<StringFeature, StringFeatureFlag>() {

            override fun createFeature(flag: StringFeatureFlag): StringFeature =
                StringFeature(info = flag.stringValueOne)

            override fun createDefault(): StringFeature = StringFeature()
        }
        val feature = creator.createFeature(StringFeature::class.java, customFactory)
        assertEquals("Android", feature.info)
    }

    @Test
    fun createsFeatureByDefault() {
        val feature = creator.createFeature(StringFeature::class.java)
        assertEquals(StringFeature.DEFAULT_INFO, feature.info)
    }

    @Test
    fun throwsErrorWhenFactoryNotFound() {
        factoryRegistry.factoriesMap = emptyMap()
        try {
            creator.createFeature(StringFeature::class.java)
            fail()
        } catch (exception: Exception) {
            assertTrue(exception is RuntimeException)
            assertEquals(
                "No factory for feature ${StringFeature::class.java.simpleName} found in registry",
                exception.message
            )
        }
    }
}