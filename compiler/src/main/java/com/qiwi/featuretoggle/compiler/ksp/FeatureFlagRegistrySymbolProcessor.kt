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