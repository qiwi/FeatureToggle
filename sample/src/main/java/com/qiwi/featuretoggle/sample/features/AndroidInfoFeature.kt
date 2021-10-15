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
package com.qiwi.featuretoggle.sample.features

import com.qiwi.featuretoggle.annotation.Factory
import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.factory.FeatureFactory

class AndroidInfoFeature(val versionInfo: String)

@FeatureFlag("android_info")
class AndroidInfoFlag(
    val versionName: String,
    val apiLevel: Int
)

@Factory
class AndroidInfoFeatureFactory : FeatureFactory<AndroidInfoFeature, AndroidInfoFlag>() {

    override fun createFeature(flag: AndroidInfoFlag): AndroidInfoFeature {
        return AndroidInfoFeature("Actual Android version name: ${flag.versionName}, actual API level: ${flag.apiLevel}")
    }

    override fun createDefault(): AndroidInfoFeature {
        return AndroidInfoFeature("Actual Android version name: unknown, actual API level: unknown")
    }
}