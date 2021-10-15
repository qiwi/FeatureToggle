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
package com.qiwi.featuretoggle.test.registry

import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.qiwi.featuretoggle.test.feature.BooleanFeature
import com.qiwi.featuretoggle.test.feature.BooleanFeatureFactory
import com.qiwi.featuretoggle.test.feature.StringFeature
import com.qiwi.featuretoggle.test.feature.StringFeatureFactory
import com.qiwi.featuretoggle.test.flag.BOOLEAN_FEATURE_KEY
import com.qiwi.featuretoggle.test.flag.STRING_FEATURE_KEY

class TestFactoryRegistry(var factoriesMap: Map<Class<*>, Pair<String, Class<*>>> = mapOf(
    BooleanFeature::class.java to Pair(BOOLEAN_FEATURE_KEY, BooleanFeatureFactory::class.java),
    StringFeature::class.java to Pair(STRING_FEATURE_KEY, StringFeatureFactory::class.java)
)): FeatureFactoryRegistry {

    override fun getFactoryMap(): Map<Class<*>, Pair<String, Class<*>>> = factoriesMap
}