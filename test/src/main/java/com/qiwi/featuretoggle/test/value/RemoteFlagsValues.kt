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

val booleanFlagRemote = BooleanFeatureFlag().apply { isEnabled = true }

val doubleFlagRemote = DoubleFeatureFlag(doubleValue = 12.0)

val stringFlagRemote = StringFeatureFlag(stringValueOne = "Android", stringValueTwo = "Pie")

val complexFlagRemote = ComplexFeatureFlag(
    booleanValue = true,
    intValue = 100,
    stringValue = "QIWI",
    listIntValue = listOf(2019, 2020, 2021),
    objValue = SampleObject(intValue = 50, stringValue = "FeatureToggle")
)

val allRemoteFlags = mapOf(
    BOOLEAN_FEATURE_KEY to booleanFlagRemote,
    DOUBLE_FEATURE_KEY to doubleFlagRemote,
    STRING_FEATURE_KEY to stringFlagRemote,
    COMPLEX_FEATURE_KEY to complexFlagRemote
)