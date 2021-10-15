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
package com.qiwi.featuretoggle.sample

import android.app.Application
import com.qiwi.featuretoggle.FeatureManager
import com.qiwi.featuretoggle.FeatureToggle
import com.qiwi.featuretoggle.converter.JacksonFeatureFlagConverter
import com.qiwi.featuretoggle.datasource.AssetsDataSource
import com.qiwi.featuretoggle.datasource.RemoteDataSource
import com.qiwi.featuretoggle.registry.FeatureFactoryRegistryGenerated
import com.qiwi.featuretoggle.registry.FeatureFlagRegistryGenerated
import com.qiwi.featuretoggle.util.DebugLogger

class FeatureToggleSampleApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        FeatureToggle.setFeatureManager(
            FeatureManager.Builder(this)
                .converter(JacksonFeatureFlagConverter())
                .logger(DebugLogger())
                .addDataSource(AssetsDataSource("feature_flags.json", this))
                .flagRegistry(FeatureFlagRegistryGenerated())
                .factoryRegistry(FeatureFactoryRegistryGenerated())
                .build()
                .apply {
                    fetchFlags()
                }
        )
    }
}