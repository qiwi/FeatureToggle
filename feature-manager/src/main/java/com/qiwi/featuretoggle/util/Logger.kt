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
package com.qiwi.featuretoggle.util

import android.util.Log
import com.qiwi.featuretoggle.FeatureManager

/**
 * Logging interface for [FeatureManager].
 *
 * @see [LoggerStub].
 * @see [DebugLogger].
 */
interface Logger {

    /**
     * Writes log message with specified params.
     *
     * @param level Log [Level].
     * @param tag Used to identify the source of a log message.
     * @param message Log message.
     * @param details Additional details.
     * @param throwable Optional [Throwable].
     */
    fun log(
        level: Level,
        tag: String,
        message: String,
        details: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    )

    /**
     * Represents log level.
     *
     * @property priority Priority of log level. Uses Android [Log] priority values.
     */
    enum class Level(val priority: Int) {
        INFO(4),
        WARN(5),
        ERROR(6)
    }
}