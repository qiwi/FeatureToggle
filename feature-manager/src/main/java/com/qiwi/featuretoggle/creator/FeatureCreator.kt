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
package com.qiwi.featuretoggle.creator

import com.qiwi.featuretoggle.factory.FeatureFactory

/**
 * Creates features using feature flags and factories.
 */
interface FeatureCreator {

    /**
     * Creates the specified feature with default [FeatureFactory].
     *
     * @param featureClass Feature class.
     */
    fun <Feature> createFeature(featureClass: Class<Feature>): Feature

    /**
     * Creates the specified feature using provided factory.
     *
     * @param featureClass Feature class.
     * @param factory A [FeatureFactory] that will be used to create feature.
     */
    fun <Feature, Flag> createFeature(featureClass: Class<Feature>, factory: FeatureFactory<Feature, Flag>): Feature
}