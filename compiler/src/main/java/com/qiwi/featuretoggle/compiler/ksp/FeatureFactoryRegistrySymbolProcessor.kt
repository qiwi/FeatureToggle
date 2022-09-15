package com.qiwi.featuretoggle.compiler.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.qiwi.featuretoggle.annotation.Factory
import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.compiler.generate.createFeatureFactoryRegistryFileSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo

class FeatureFactoryRegistrySymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val featureFactorySymbols = resolver
            .getSymbolsWithAnnotation(Factory::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val originFiles = featureFactorySymbols.map { it.containingFile!! }

        val featureFactorySymbolsVisitor =
            ClassDeclarationAnnotationVisitor { classDeclaration ->
                val factoryClassName = ClassName(
                    classDeclaration.packageName.asString(),
                    classDeclaration.simpleName.asString()
                )
                val superTypeFeatureFactory = classDeclaration.superTypes.first().resolve()

                val featureKSType = superTypeFeatureFactory.arguments[0].type!!
                val featureFlagKSType = superTypeFeatureFactory.arguments[1].type!!

                val featureClassName = ClassName(
                    featureKSType.resolve().declaration.packageName.asString(),
                    featureKSType.resolve().declaration.simpleName.asString()
                )
                val featureKey =
                    featureFlagKSType.resolve().declaration.getAnnotationsByType(FeatureFlag::class)
                        .first().key

                featureClassName to Pair(featureKey, factoryClassName)
            }

        val featureFactoryMap =
            featureFactorySymbols.associate { it.accept(featureFactorySymbolsVisitor, Unit) }
                .toSortedMap()

        if (featureFactoryMap.isNotEmpty()) {
            createFeatureFactoryRegistryFileSpec(featureFactoryMap)
                .writeTo(codeGenerator, aggregating = true, originFiles)
        }
        return emptyList()
    }
}