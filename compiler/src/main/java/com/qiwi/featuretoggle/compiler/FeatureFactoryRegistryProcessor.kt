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

    companion object {

        /**
         * Package name where generated implementation of [FeatureFactoryRegistry] will be placed.
         */
        const val GENERATED_PACKAGE_NAME = "com.qiwi.featuretoggle.registry"

        /**
         * Name for generated implementation of [FeatureFactoryRegistry].
         */
        const val GENERATED_CLASS_NAME = "FeatureFactoryRegistryGenerated"
    }

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
            createFeatureFactoryRegistry(featureFactoryMap)
        }
        return false
    }

    private fun createFeatureFactoryRegistry(featureFactoryMap: Map<ClassName, Pair<String, ClassName>>) {
        val factoryMapType = MAP.parameterizedBy(
            Class::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Any::class)),
            Pair::class.asTypeName().parameterizedBy(
                String::class.asTypeName(), Class::class.asTypeName().parameterizedBy(
                    WildcardTypeName.producerOf(Any::class)
                )
            )
        )
        val registry = TypeSpec.classBuilder(GENERATED_CLASS_NAME)
            .addSuperinterface(FeatureFactoryRegistry::class)
            .addProperty(
                PropertySpec.builder("factoryMap", factoryMapType, KModifier.PRIVATE)
                .initializer(getFactoryMapInitializer(featureFactoryMap))
                .build())
            .addFunction(
                FunSpec.builder("getFactoryMap")
                .addModifiers(KModifier.OVERRIDE)
                .returns(factoryMapType)
                .addCode(
                    CodeBlock.Builder()
                    .addStatement("return factoryMap")
                    .build())
                .build())
            .build()
        FileSpec.builder(GENERATED_PACKAGE_NAME, GENERATED_CLASS_NAME)
            .addType(registry)
            .build()
            .writeTo(processingEnv.filer)
    }

    private fun getFactoryMapInitializer(featureFactoryMap: Map<ClassName, Pair<String, ClassName>>): CodeBlock {
        val builder = CodeBlock.Builder().addStatement("mapOf(")
        featureFactoryMap.entries.forEachIndexed { index, entry ->
            val featureClassName = entry.key
            val featureKey = entry.value.first
            val featureFactoryClassName = entry.value.second
            val endOfStatement = if(index == featureFactoryMap.size-1) "" else ","
            builder.addStatement("%T::class.java to Pair(%S, %T::class.java)$endOfStatement", featureClassName, featureKey, featureFactoryClassName)
        }
        return builder
            .addStatement(")")
            .build()
    }
}