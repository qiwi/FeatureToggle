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

import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.test.flag.*

class TestFlagRegistry(
    var flagsMap: Map<String, Class<*>> = mapOf(
        BOOLEAN_FEATURE_KEY to BooleanFeatureFlag::class.java,
        STRING_FEATURE_KEY to StringFeatureFlag::class.java,
        DOUBLE_FEATURE_KEY to DoubleFeatureFlag::class.java,
        COMPLEX_FEATURE_KEY to ComplexFeatureFlag::class.java
    )
) : FeatureFlagRegistry {

    override fun getFeatureFlagsMap(): Map<String, Class<*>> = flagsMap

    override fun getFeatureKeysMap(): Map<Class<*>, String> =
        flagsMap.entries.associateBy({ it.value }) { it.key }
}