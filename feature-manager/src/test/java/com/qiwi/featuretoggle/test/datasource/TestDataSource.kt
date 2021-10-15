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
package com.qiwi.featuretoggle.test.datasource

import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.datasource.FeatureFlagDataSource
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

abstract class TestDataSource: FeatureFlagDataSource {

    private val events = Channel<Event>()

    override fun getFlags(
        registry: FeatureFlagRegistry,
        converter: FeatureFlagConverter,
        logger: Logger
    ): Flow<Map<String, Any>> = events.receiveAsFlow().map { event->
        when(event) {
            is Event.Error -> throw event.throwable
            is Event.Item -> event.flags
        }
    }

    suspend fun updateFlags(flags: Map<String, Any>, final: Boolean = false) {
        events.send(Event.Item(flags))
        if(final) {
            events.close()
        }
    }

    suspend fun throwError(throwable: Throwable) {
        events.send(Event.Error(throwable))
    }

    private sealed class Event {
        data class Item(val flags: Map<String, Any>): Event()
        data class Error(val throwable: Throwable): Event()
    }
}