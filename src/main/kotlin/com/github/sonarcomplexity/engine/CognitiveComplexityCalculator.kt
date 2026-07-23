package com.github.sonarcomplexity.engine

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil

/**
 * Calculates Cognitive Complexity following the SonarQube S3776 Specification.
 *
 * Rules:
 * 1. Increment for control flow structures (if, else if, switch, for, while, catch, ternary, etc.)
 * 2. Increment for nesting levels when control flow structures are nested inside each other.
 * 3. Increment for boolean logic operator sequences (&&, ||).
 */
object CognitiveComplexityCalculator {

    fun calculate(functionElement: PsiElement, functionName: String = "function"): ComplexityResult {
        var cognitiveScore = 0
        var cyclomaticScore = 1 // Cyclomatic starts at 1
        val breakdown = mutableListOf<ComplexityBreakdownItem>()

        fun walk(element: PsiElement, nestingLevel: Int) {
            val children = element.children
            for (child in children) {
                val typeName = child.node?.elementType?.toString() ?: ""
                val text = child.text

                var isControlFlow = false
                var isNestingBoundary = false
                var description = ""

                // Detect control flow increments
                when {
                    typeName.contains("IF_STATEMENT") || text.startsWith("if") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "if statement"
                    }
                    typeName.contains("FOR_STATEMENT") || text.startsWith("for") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "for loop"
                    }
                    typeName.contains("WHILE_STATEMENT") || text.startsWith("while") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "while loop"
                    }
                    typeName.contains("SWITCH_STATEMENT") || text.startsWith("switch") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "switch statement"
                    }
                    typeName.contains("CATCH") || text.startsWith("catch") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "catch clause"
                    }
                    typeName.contains("CONDLEXPR") || text.contains("?") && text.contains(":") -> {
                        isControlFlow = true
                        description = "ternary operator"
                    }
                    typeName.contains("BINARY_EXPRESSION") && (text.contains("&&") || text.contains("||")) -> {
                        cognitiveScore += 1
                        cyclomaticScore += 1
                        breakdown.add(ComplexityBreakdownItem(getLineNumber(child), "logical operator", 1))
                    }
                }

                if (isControlFlow) {
                    val increment = 1 + nestingLevel
                    cognitiveScore += increment
                    cyclomaticScore += 1
                    breakdown.add(
                        ComplexityBreakdownItem(
                            getLineNumber(child),
                            "$description (nesting +$nestingLevel)",
                            increment
                        )
                    )
                }

                val nextNesting = if (isNestingBoundary) nestingLevel + 1 else nestingLevel
                walk(child, nextNesting)
            }
        }

        // Walk inside the function body
        walk(functionElement, 0)

        val severity = ComplexitySeverity.fromScore(cognitiveScore)
        return ComplexityResult(
            functionName = functionName,
            cognitiveComplexity = cognitiveScore,
            cyclomaticComplexity = cyclomaticScore,
            severity = severity,
            breakdown = breakdown
        )
    }

    private fun getLineNumber(element: PsiElement): Int {
        val containingFile = element.containingFile ?: return 1
        val document = containingFile.viewProvider.document ?: return 1
        return document.getLineNumber(element.textOffset) + 1
    }
}
