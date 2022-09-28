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

import com.qiwi.featuretoggle.annotation.Factory
import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.compiler.generate.createFeatureFactoryRegistryFileSpec
import com.qiwi.featuretoggle.factory.FeatureFactory
import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/**
 * Collects classes that marked with annotation [Factory] and inherited from [FeatureFactory].
 * Creates implementation of [FeatureFactoryRegistry] that maps feature class to its key and [FeatureFactory].
 */
class FeatureFactoryRegistryProcessor: AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(Factory::class.java.canonicalName)

    override fun getSupportedOptions(): Set<String> = emptySet()

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val featureFactoryMap = roundEnv.getElementsAnnotatedWith(Factory::class.java).map { element ->
            element as TypeElement
            val factoryInterface = element.superclass as DeclaredType
            val featureType = (factoryInterface.typeArguments[0] as DeclaredType).asElement()
            val featureFlagType = (factoryInterface.typeArguments[1] as DeclaredType).asElement()
            val featureClass = ClassName(featureType.enclosingElement.toString(), featureType.simpleName.toString())
            val featureFactoryClass = ClassName(element.enclosingElement.toString(), element.simpleName.toString())
            val featureKey = featureFlagType.getAnnotation(FeatureFlag::class.java).key
            featureClass to Pair(featureKey, featureFactoryClass)
        }.toMap().toSortedMap()
        if(featureFactoryMap.isNotEmpty()) {
            createFeatureFactoryRegistryFileSpec(featureFactoryMap)
                .writeTo(processingEnv.filer)
        }
        return false
    }

}