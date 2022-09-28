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
package com.qiwi.featuretoggle.compiler

import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.compiler.generate.createFeatureFlagRegistryFileSpec
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Collects classes that marked with annotation [FeatureFlag].
 * Creates implementation of [FeatureFlagRegistry] that maps feature key to its feature flag class and vice versa.
 */
class FeatureFlagRegistryProcessor: AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(FeatureFlag::class.java.canonicalName)

    override fun getSupportedOptions(): Set<String> = emptySet()

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val featureFlagMap = roundEnv.getElementsAnnotatedWith(FeatureFlag::class.java).map { element ->
            element as TypeElement
            val packageName = element.enclosingElement.toString()
            val className = ClassName(packageName, element.simpleName.toString())
            val featureKey = element.getAnnotation(FeatureFlag::class.java).key
            featureKey to className
        }.toMap().toSortedMap()
        if(featureFlagMap.isNotEmpty()) {
            val featureKeyMap = featureFlagMap.entries.associateBy({ it.value }) { it.key }.toSortedMap()
            createFeatureFlagRegistryFileSpec(featureFlagMap, featureKeyMap)
                .writeTo(processingEnv.filer)
        }
        return false
    }

}