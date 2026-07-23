package com.github.sonarcomplexity.actions

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor

class CalculateComplexityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        val functions = mutableListOf<PsiElement>()
        psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                val typeName = element.node?.elementType?.toString() ?: ""
                if (!typeName.contains("KEYWORD") && !typeName.contains("IDENTIFIER") && 
                    (typeName.contains("FUNCTION") || typeName.contains("METHOD"))) {
                    functions.add(element)
                }
                super.visitElement(element)
            }
        })
        
        if (functions.isEmpty()) {
            Messages.showInfoMessage(project, "No functions found in this file.", "Sonar Complexity")
            return
        }
        
        val sb = StringBuilder()
        sb.append("Sonar Complexity Analysis for '${psiFile.name}':\n\n")
        var totalCognitive = 0
        var maxCognitive = 0
        var complexFunctionsCount = 0
        
        functions.forEach { func ->
            val text = func.text.take(40).replace("\n", " ")
            val name = when {
                text.contains("function") -> text.substringAfter("function").substringBefore("(").trim()
                text.contains("=") -> text.substringBefore("=").trim()
                else -> "anonymous"
            }.ifEmpty { "function" }
            
            val result = CognitiveComplexityCalculator.calculate(func, name)
            totalCognitive += result.cognitiveComplexity
            if (result.cognitiveComplexity > maxCognitive) {
                maxCognitive = result.cognitiveComplexity
            }
            if (result.cognitiveComplexity >= 15) { // High threshold
                complexFunctionsCount++
            }
            sb.append("- $name: Cognitive = ${result.cognitiveComplexity}, Cyclomatic = ${result.cyclomaticComplexity} (${result.severity})\n")
        }
        
        sb.append("\nSummary:\n")
        sb.append("Total Functions: ${functions.size}\n")
        sb.append("Total Cognitive Complexity: $totalCognitive\n")
        sb.append("Average Complexity: ${String.format("%.2f", totalCognitive.toDouble() / functions.size)}\n")
        sb.append("Max Complexity: $maxCognitive\n")
        if (complexFunctionsCount > 0) {
            sb.append("⚠️ $complexFunctionsCount functions exceed high complexity threshold (>= 15)!\n")
        }
        
        Messages.showInfoMessage(project, sb.toString(), "Sonar Complexity Analysis")
    }
}
