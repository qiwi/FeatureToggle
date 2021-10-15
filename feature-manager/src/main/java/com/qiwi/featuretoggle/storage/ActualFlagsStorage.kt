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
package com.qiwi.featuretoggle.storage

/**
 * Storage for actual feature flags.
 *
 * Storage may cache feature flag after first request (feature flag was "used") to prevent changing
 * application config if feature flag changed after first request.
 */
interface ActualFlagsStorage {

    /**
     * @return Feature flag object.
     */
    fun getFlag(key: String): Any?

    /**
     * Updates stored feature flags replacing existing.
     *
     * @param flags Map where key is feature key and value is feature flag object.
     * @param force If true, storage must clear used flags cache.
     */
    fun updateFlags(flags: Map<String, Any>, force: Boolean = false)

    /**
     * Clears used flags cache.
     */
    fun resetUsedFlags()

    /**
     * Clears all stored flags and used flags cache.
     */
    fun resetAllFlags()

}