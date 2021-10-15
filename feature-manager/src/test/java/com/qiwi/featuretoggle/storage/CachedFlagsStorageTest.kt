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

import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.test.logger.TestLogger
import com.qiwi.featuretoggle.test.registry.TestFlagRegistry
import com.qiwi.featuretoggle.test.util.TestResources
import com.qiwi.featuretoggle.test.value.allRemoteFlags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CachedFlagsStorageTest {

    @get:Rule
    val testFolder = TemporaryFolder()

    private val registry = TestFlagRegistry()
    private val converter = JacksonFeatureFlagConverter()
    private val logger = TestLogger()

    private lateinit var testFile: File
    private lateinit var storage: InternalCachedFlagsStorage

    @Before
    fun before() {
        testFile = testFolder.newFile("flags.json")
        storage = InternalCachedFlagsStorage(testFile)
    }

    @Test
    fun returnsCachedFlagsSuccessfully() = runBlocking {
        testFile.writeText(TestResources.getCachedFlagsJson())
        val flags = storage.getFlags(converter, registry, logger)
        assertEquals(allRemoteFlags, flags)
    }

    @Test
    fun returnsEmptyFlagsWhenFileNotExists() = runBlocking {
        testFile.delete()
        val flags = storage.getFlags(converter, registry, logger)
        assertTrue(flags.isEmpty())
    }

    @Test
    fun throwsErrorWhenFileCorrupted() = runBlocking {
        testFile.writeText("corrupted")
        try {
            storage.getFlags(converter, registry, logger)
            fail()
        } catch (e: Exception) {
        }
    }
}