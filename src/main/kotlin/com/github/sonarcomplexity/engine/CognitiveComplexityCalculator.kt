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

        var precedingOperator: String? = null

        fun walk(element: PsiElement, nestingLevel: Int) {
            val children = element.children
            for (child in children) {
                val typeName = child.node?.elementType?.toString() ?: ""
                val text = child.text

                // Skip nested function/method/arrow function declarations entirely
                if (!typeName.contains("KEYWORD") && !typeName.contains("IDENTIFIER") &&
                    (typeName.contains("FUNCTION") || typeName.contains("METHOD") || typeName.contains("JS_ARROW_FUNCTION") || typeName.contains("ARROW_FUNCTION"))
                ) {
                    continue
                }

                // Reset preceding operator on statements, blocks, etc.
                val isStatementBoundary = typeName.contains("STATEMENT") || typeName.contains("BLOCK")
                if (isStatementBoundary) {
                    precedingOperator = null
                }

                var isControlFlow = false
                var isNestingBoundary = false
                var description = ""

                // Detect control flow increments
                when {
                    typeName.contains("IF_STATEMENT") || text.startsWith("if") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "if statement"

                        // Check for solo else inside this JSIfStatement
                        val childList = child.children
                        val elseIndex = childList.indexOfFirst { it.text == "else" }
                        if (elseIndex != -1 && elseIndex + 1 < childList.size) {
                            val elseBranch = childList[elseIndex + 1]
                            val elseBranchType = elseBranch.node?.elementType?.toString() ?: ""
                            if (!elseBranchType.contains("IF_STATEMENT") && elseBranch.text != "if") {
                                cognitiveScore += 1
                                breakdown.add(ComplexityBreakdownItem(getLineNumber(elseBranch), "else branch", 1))
                            }
                        }
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
                    typeName.contains("DO_WHILE_STATEMENT") || text.startsWith("do") -> {
                        isControlFlow = true
                        isNestingBoundary = true
                        description = "do-while loop"
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
                    typeName.contains("CONDLEXPR") || (text.contains("?") && text.contains(":") && typeName.contains("CONDITIONAL_EXPRESSION")) -> {
                        isControlFlow = true
                        description = "ternary operator"
                    }
                    // Labeled break or continue statement
                    (typeName.contains("BREAK_STATEMENT") || typeName.contains("CONTINUE_STATEMENT")) && child.children.any { it.node?.elementType?.toString()?.contains("IDENTIFIER") == true } -> {
                        cognitiveScore += 1
                        breakdown.add(ComplexityBreakdownItem(getLineNumber(child), "break/continue with label", 1))
                    }
                    // Handle logical operators
                    typeName.contains("ANDAND") || typeName.contains("OROR") || typeName.contains("QUESTQUEST") || text == "&&" || text == "||" || text == "??" -> {
                        val op = text
                        if (precedingOperator == null || precedingOperator != op) {
                            cognitiveScore += 1
                            breakdown.add(ComplexityBreakdownItem(getLineNumber(child), "logical operator sequence", 1))
                            precedingOperator = op
                        }
                        cyclomaticScore += 1
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

                // Handle pausing sequence on call expressions, prefix expressions, indexed accesses
                val isPause = typeName.contains("CALL_EXPRESSION") || typeName.contains("PREFIX_EXPRESSION") || typeName.contains("INDEXED_PROPERTY_ACCESS_EXPRESSION")
                val savedOp = precedingOperator
                if (isPause) {
                    precedingOperator = null
                }

                walk(child, nextNesting)

                if (isPause) {
                    precedingOperator = savedOp
                }
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
