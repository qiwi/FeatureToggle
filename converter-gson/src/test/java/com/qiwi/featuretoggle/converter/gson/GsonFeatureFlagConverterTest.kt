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
package com.qiwi.featuretoggle.converter.gson

import com.qiwi.featuretoggle.converter.GsonFeatureFlagConverter
import com.qiwi.featuretoggle.test.flag.*
import com.qiwi.featuretoggle.test.util.TestResources
import com.qiwi.featuretoggle.test.util.unformatted
import com.qiwi.featuretoggle.test.value.*
import org.junit.Assert.assertEquals
import org.junit.Test

class GsonFeatureFlagConverterTest {

    private val converter = GsonFeatureFlagConverter()

    @Test
    fun readsSimpleJson() {
        val flag = converter.read(TestResources.getRemoteStringFlagJson(), StringFeatureFlag::class.java)
        assertEquals(stringFlagRemote, flag)
    }

    @Test
    fun readsComplexJson() {
        val flag = converter.read(TestResources.getRemoteComplexFlagJson(), ComplexFeatureFlag::class.java)
        assertEquals(complexFlagRemote, flag)
    }

    @Test
    fun readsJsonAsList() {
        val list = converter.read(TestResources.getRemoteFlagsJson(), List::class.java)
        val booleanFlag = converter.convert(list[0]!!, BooleanFeatureFlag::class.java)
        val complexFlag = converter.convert(list[1]!!, ComplexFeatureFlag::class.java)
        val stringFlag = converter.convert(list[2]!!, StringFeatureFlag::class.java)
        val doubleFlag = converter.convert(list[3]!!, DoubleFeatureFlag::class.java)
        assertEquals(booleanFlagRemote, booleanFlag)
        assertEquals(doubleFlagRemote, doubleFlag)
        assertEquals(stringFlagRemote, stringFlag)
        assertEquals(complexFlagRemote, complexFlag)
    }

    @Test
    fun readsJsonAsMap() {
        val map = converter.read(TestResources.getCachedFlagsJson(), Map::class.java)
        val booleanFlag = converter.convert(map[BOOLEAN_FEATURE_KEY]!!, BooleanFeatureFlag::class.java)
        val doubleFlag = converter.convert(map[DOUBLE_FEATURE_KEY]!!, DoubleFeatureFlag::class.java)
        val stringFlag = converter.convert(map[STRING_FEATURE_KEY]!!, StringFeatureFlag::class.java)
        val complexFlag = converter.convert(map[COMPLEX_FEATURE_KEY]!!, ComplexFeatureFlag::class.java)
        assertEquals(booleanFlagRemote, booleanFlag)
        assertEquals(doubleFlagRemote, doubleFlag)
        assertEquals(stringFlagRemote, stringFlag)
        assertEquals(complexFlagRemote, complexFlag)
    }

    @Test
    fun ignoresUnknownFlagFields() {
        val flag = converter.read(TestResources.getRemoteStringFlagV2Json(), StringFeatureFlag::class.java)
        assertEquals(stringFlagRemote, flag)
    }

    @Test
    fun writesMapAsJson() {
        val content = converter.write(allRemoteFlags)
        assertEquals(TestResources.getCachedFlagsJson().unformatted(), content)
    }
}