package com.qiwi.featuretoggle.compiler.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor

class ClassDeclarationAnnotationVisitor<R>(
    private val visitClassDeclarationBlock: (KSClassDeclaration) -> R
) : KSEmptyVisitor<Unit, R>() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): R {
        return visitClassDeclarationBlock(classDeclaration)
    }

    override fun defaultHandler(node: KSNode, data: Unit): R {
        TODO("Not yet implemented")
    }
}