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

import com.google.gson.Gson
import java.io.InputStream
import java.io.InputStreamReader

/**
 * A [FeatureFlagConverter] that uses [Gson](https://github.com/google/gson) library to convert json
 * into feature flag objects.
 *
 * @property gson A [Gson] instance.
 */
class GsonFeatureFlagConverter(private val gson: Gson = Gson()) : FeatureFlagConverter {

    override fun <T> read(content: String, type: Class<T>): T = gson.fromJson(content, type)

    override fun <T> read(source: InputStream, type: Class<T>): T =
        gson.fromJson(InputStreamReader(source), type)

    override fun <T> write(value: T): String = gson.toJson(value)

    override fun <T> convert(value: Any, toType: Class<T>): T = gson.fromJson(gson.toJsonTree(value), toType)
}