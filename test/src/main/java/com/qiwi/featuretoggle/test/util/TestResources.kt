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
package com.qiwi.featuretoggle.test.util

import java.io.InputStream

object TestResources {

    private fun getResourceInputStream(name: String): InputStream =
        javaClass.classLoader?.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource not found")

    private fun getResourceAsString(name: String): String =
        getResourceInputStream(name).bufferedReader().use { it.readText() }

    fun getRemoteFlagsJson(): String = getResourceAsString("remote_feature_flags.json")

    fun getCachedFlagsJson(): String = getResourceAsString("cached_feature_flags.json")

    fun getRemoteBooleanFlagJson(): String = getResourceAsString("remote_boolean_flag.json")

    fun getRemoteDoubleFlagJson(): String = getResourceAsString("remote_double_flag.json")

    fun getRemoteStringFlagJson(): String = getResourceAsString("remote_string_flag.json")

    fun getRemoteStringFlagV2Json(): String = getResourceAsString("remote_string_flag_v2.json")

    fun getRemoteComplexFlagJson(): String = getResourceAsString("remote_complex_flag.json")

    fun getInvalidFlagJson(): String = getResourceAsString("invalid_feature_flag.json")
}