package com.qiwi.featuretoggle.compiler.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class FeatureFactoryRegistrySymbolProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FeatureFactoryRegistrySymbolProcessor(
            environment.codeGenerator,
            environment.logger
        )
}