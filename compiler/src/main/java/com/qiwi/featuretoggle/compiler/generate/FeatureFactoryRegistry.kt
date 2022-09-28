/**
 * Copyright (c) 2022 QIWI
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
package com.qiwi.featuretoggle.compiler.generate

import com.qiwi.featuretoggle.registry.FeatureFactoryRegistry
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName

/**
 * Package name where generated implementation of [FeatureFactoryRegistry] will be placed.
 */
private const val GENERATED_PACKAGE_NAME = "com.qiwi.featuretoggle.registry"

/**
 * Name for generated implementation of [FeatureFactoryRegistry].
 */
private const val GENERATED_CLASS_NAME = "FeatureFactoryRegistryGenerated"

fun createFeatureFactoryRegistryFileSpec(featureFactoryMap: Map<ClassName, Pair<String, ClassName>>): FileSpec {
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
                .build()
        )
        .addFunction(
            FunSpec.builder("getFactoryMap")
                .addModifiers(KModifier.OVERRIDE)
                .returns(factoryMapType)
                .addCode(
                    CodeBlock.Builder()
                        .addStatement("return factoryMap")
                        .build()
                )
                .build()
        )
        .build()
    return FileSpec.builder(GENERATED_PACKAGE_NAME, GENERATED_CLASS_NAME)
        .addType(registry)
        .build()
}

private fun getFactoryMapInitializer(featureFactoryMap: Map<ClassName, Pair<String, ClassName>>): CodeBlock {
    val builder = CodeBlock.Builder().addStatement("mapOf(")
    featureFactoryMap.entries.forEachIndexed { index, entry ->
        val featureClassName = entry.key
        val featureKey = entry.value.first
        val featureFactoryClassName = entry.value.second
        val endOfStatement = if (index == featureFactoryMap.size - 1) "" else ","
        builder.addStatement(
            "%T::class.java to Pair(%S, %T::class.java)$endOfStatement",
            featureClassName,
            featureKey,
            featureFactoryClassName
        )
    }
    return builder
        .addStatement(")")
        .build()
}