package com.qiwi.featuretoggle.compiler.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.compiler.generate.createFeatureFlagRegistryFileSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo

class FeatureFlagRegistrySymbolProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val featureFlagSymbols = resolver
            .getSymbolsWithAnnotation(FeatureFlag::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val originFiles = featureFlagSymbols.map { it.containingFile!! }

        val featureFlagSymbolsVisitor =
            ClassDeclarationAnnotationVisitor { classDeclaration ->
                val flagClassName = ClassName(
                    classDeclaration.packageName.asString(),
                    classDeclaration.simpleName.asString()
                )
                val featureKey = classDeclaration.getAnnotationsByType(FeatureFlag::class)
                    .first().key
                featureKey to flagClassName
            }

        val featureFlagMap =
            featureFlagSymbols.associate { it.accept(featureFlagSymbolsVisitor, Unit) }
                .toSortedMap()

        if (featureFlagMap.isNotEmpty()) {
            val featureKeyMap = featureFlagMap.entries.associate { it.value to it.key }
                .toSortedMap()
            createFeatureFlagRegistryFileSpec(featureFlagMap, featureKeyMap)
                .writeTo(codeGenerator, aggregating = true, originFiles)
        }
        return emptyList()
    }
}