package com.github.sonarcomplexity.engine

enum class ComplexitySeverity {
    LOW,
    MODERATE,
    HIGH;

    companion object {
        fun fromScore(score: Int, warningThreshold: Int = 15, criticalThreshold: Int = 25): ComplexitySeverity {
            return when {
                score >= criticalThreshold -> HIGH
                score >= warningThreshold -> MODERATE
                else -> LOW
            }
        }
    }
}

data class ComplexityBreakdownItem(
    val line: Int,
    val description: String,
    val cost: Int
)

data class ComplexityResult(
    val functionName: String,
    val cognitiveComplexity: Int,
    val cyclomaticComplexity: Int,
    val severity: ComplexitySeverity,
    val breakdown: List<ComplexityBreakdownItem> = emptyList()
)
