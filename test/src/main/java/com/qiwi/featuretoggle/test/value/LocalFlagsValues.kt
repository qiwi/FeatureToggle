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
package com.qiwi.featuretoggle.test.value

import com.qiwi.featuretoggle.test.flag.*

val booleanFlagLocal = BooleanFeatureFlag().apply { isEnabled = false }

val doubleFlagLocal = DoubleFeatureFlag(4.26)

val stringFlagLocal = StringFeatureFlag("Android", "Oreo")

val complexFlagLocal = ComplexFeatureFlag(
    booleanValue = false,
    intValue = 10,
    stringValue = "QIWI",
    listIntValue = listOf(2019, 2020),
    objValue = SampleObject(intValue = 40, stringValue = "FeatureManager")
)

val allLocalFlags = mapOf(
    BOOLEAN_FEATURE_KEY to booleanFlagLocal,
    DOUBLE_FEATURE_KEY to doubleFlagLocal,
    STRING_FEATURE_KEY to stringFlagLocal,
    COMPLEX_FEATURE_KEY to complexFlagLocal
)