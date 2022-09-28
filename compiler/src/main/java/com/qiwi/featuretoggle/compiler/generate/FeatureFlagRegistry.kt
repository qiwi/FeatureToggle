package com.qiwi.featuretoggle.compiler.generate

import com.qiwi.featuretoggle.compiler.FeatureFlagRegistryProcessor
import com.qiwi.featuretoggle.registry.FeatureFlagRegistry
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
 * Package name where generated implementation of [FeatureFlagRegistry] will be placed.
 */
private const val GENERATED_PACKAGE_NAME = "com.qiwi.featuretoggle.registry"

/**
 * Name for generated implementation of [FeatureFlagRegistry].
 */
private const val GENERATED_CLASS_NAME = "FeatureFlagRegistryGenerated"

fun createFeatureFlagRegistryFileSpec(
    featureFlagsMap: Map<String, ClassName>,
    featureKeysMap: Map<ClassName, String>
): FileSpec {
    val flagsMapType = MAP.parameterizedBy(
        String::class.asTypeName(),
        Class::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Any::class))
    )
    val keysMapType = MAP.parameterizedBy(
        Class::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Any::class)),
        String::class.asTypeName()
    )
    val registry = TypeSpec.classBuilder(GENERATED_CLASS_NAME)
        .addSuperinterface(FeatureFlagRegistry::class)
        .addProperty(
            PropertySpec.builder("flagsMap", flagsMapType, KModifier.PRIVATE)
                .initializer(getFlagsMapInitializer(featureFlagsMap))
                .build()
        )
        .addProperty(
            PropertySpec.builder("keysMap", keysMapType, KModifier.PRIVATE)
                .initializer(getKeysMapInitializer(featureKeysMap))
                .build()
        )
        .addFunction(
            FunSpec.builder("getFeatureFlagsMap")
                .addModifiers(KModifier.OVERRIDE)
                .returns(flagsMapType)
                .addCode(
                    CodeBlock.Builder()
                        .addStatement("return flagsMap")
                        .build()
                )
                .build()
        )
        .addFunction(
            FunSpec.builder("getFeatureKeysMap")
                .addModifiers(KModifier.OVERRIDE)
                .returns(keysMapType)
                .addCode(
                    CodeBlock.Builder()
                        .addStatement("return keysMap")
                        .build()
                )
                .build()
        )
        .build()
    return FileSpec.builder(GENERATED_PACKAGE_NAME, GENERATED_CLASS_NAME)
        .addType(registry)
        .build()
}

private fun getFlagsMapInitializer(featureFlagMap: Map<String, ClassName>): CodeBlock {
    val builder = CodeBlock.Builder().addStatement("mapOf(")
    featureFlagMap.entries.forEachIndexed { index, entry ->
        val key = entry.key
        val className = entry.value
        val endOfStatement = if (index == featureFlagMap.size - 1) "" else ","
        builder.addStatement("%S to %T::class.java$endOfStatement", key, className)
    }
    return builder
        .addStatement(")")
        .build()
}

private fun getKeysMapInitializer(featureKeysMap: Map<ClassName, String>): CodeBlock {
    val builder = CodeBlock.Builder().addStatement("mapOf(")
    featureKeysMap.entries.forEachIndexed { index, entry ->
        val className = entry.key
        val key = entry.value
        val endOfStatement = if (index == featureKeysMap.size - 1) "" else ","
        builder.addStatement("%T::class.java to %S$endOfStatement", className, key)
    }
    return builder
        .addStatement(")")
        .build()
}