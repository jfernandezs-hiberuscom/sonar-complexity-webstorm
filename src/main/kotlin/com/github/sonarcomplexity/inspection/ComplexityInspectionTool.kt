package com.github.sonarcomplexity.inspection

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
import com.github.sonarcomplexity.engine.ComplexitySeverity
import com.github.sonarcomplexity.settings.ComplexitySettingsState
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class ComplexityInspectionTool : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val settings = ComplexitySettingsState.instance
        if (!settings.enableInspections) return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (isFunctionDeclaration(element)) {
                    val functionName = getFunctionName(element)
                    val result = CognitiveComplexityCalculator.calculate(element, functionName)

                    if (result.cognitiveComplexity >= settings.warningThreshold) {
                        val message = "Function '$functionName' has a Cognitive Complexity of ${result.cognitiveComplexity} (threshold: ${settings.warningThreshold}). Consider refactoring to simplify control flow."
                        holder.registerProblem(element.firstChild ?: element, message)
                    }
                }
            }
        }
    }

    private fun isFunctionDeclaration(element: PsiElement): Boolean {
        val typeName = element.node?.elementType?.toString() ?: ""
        return !typeName.contains("KEYWORD") && !typeName.contains("IDENTIFIER") && 
                (typeName.contains("FUNCTION") || typeName.contains("METHOD"))
    }

    private fun getFunctionName(element: PsiElement): String {
        val text = element.text.take(40).replace("\n", " ")
        return when {
            text.contains("function") -> text.substringAfter("function").substringBefore("(").trim()
            text.contains("=") -> text.substringBefore("=").trim()
            else -> "anonymous"
        }.ifEmpty { "function" }
    }
}
