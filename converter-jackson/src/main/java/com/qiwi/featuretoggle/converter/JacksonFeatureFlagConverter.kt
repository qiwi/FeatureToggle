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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.InputStream

/**
 * A [FeatureFlagConverter] that uses [Jackson](https://github.com/FasterXML/jackson-core) library
 * with [Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin) to convert json into
 * feature flag objects.
 *
 * @property objectMapper An [ObjectMapper] instance.
 */
class JacksonFeatureFlagConverter(
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
) : FeatureFlagConverter {

    override fun <T> read(source: InputStream, type: Class<T>): T = objectMapper.readValue(source, type)

    override fun <T> read(content: String, type: Class<T>): T = objectMapper.readValue(content, type)

    override fun <T> write(value: T): String = objectMapper.writeValueAsString(value)

    override fun <T> convert(value: Any, toType: Class<T>): T = objectMapper.convertValue(value, toType)
}