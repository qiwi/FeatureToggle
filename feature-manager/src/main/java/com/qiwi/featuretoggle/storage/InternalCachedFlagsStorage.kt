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

import android.content.Context
import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import com.qiwi.featuretoggle.converter.FeatureFlagConverter
import com.qiwi.featuretoggle.converter.convertFeatureFlag
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.qiwi.featuretoggle.util.Logger
import java.io.File

/**
 * A [CachedFlagsStorage] that stores cached feature flags in Json file inside [Context.getFilesDir] directory.
 */
internal class InternalCachedFlagsStorage(
    file: File
) : CachedFlagsStorage {

    constructor(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME
    ) : this(File(context.filesDir, fileName))

    private val atomicFile = AtomicFile(file)

    override suspend fun getFlags(
        converter: FeatureFlagConverter,
        registry: FeatureFlagRegistry,
        logger: Logger
    ): Map<String, Any> {
        return if (atomicFile.baseFile.exists()) {
            val content = atomicFile.readText()
            val flags = converter.read(content, Map::class.java)
            flags.mapNotNull { entry ->
                converter.convertFeatureFlag(
                    entry.key as String,
                    entry.value as Any,
                    javaClass.simpleName,
                    registry,
                    logger
                )
            }.toMap()
        } else {
            emptyMap()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun saveFlags(
        flags: Map<String, Any>,
        converter: FeatureFlagConverter,
        logger: Logger
    ) {
        atomicFile.baseFile.createNewFile()
        atomicFile.writeText(converter.write(flags))
    }

    companion object {
        const val DEFAULT_FILE_NAME = "flags.json"
    }
}