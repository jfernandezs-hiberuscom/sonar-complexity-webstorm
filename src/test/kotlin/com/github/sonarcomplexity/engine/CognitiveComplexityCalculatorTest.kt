package com.github.sonarcomplexity.engine

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class CognitiveComplexityCalculatorTest {

    @Test
    fun `test severity thresholds`() {
        assertThat(ComplexitySeverity.fromScore(4)).isEqualTo(ComplexitySeverity.LOW)
        assertThat(ComplexitySeverity.fromScore(16)).isEqualTo(ComplexitySeverity.MODERATE)
        assertThat(ComplexitySeverity.fromScore(28)).isEqualTo(ComplexitySeverity.HIGH)
    }

    @Test
    fun `test complexity result structure`() {
        val result = ComplexityResult(
            functionName = "calculateDiscount",
            cognitiveComplexity = 12,
            cyclomaticComplexity = 5,
            severity = ComplexitySeverity.MODERATE
        )

        assertThat(result.functionName).isEqualTo("calculateDiscount")
        assertThat(result.cognitiveComplexity).isEqualTo(12)
        assertThat(result.severity).isEqualTo(ComplexitySeverity.MODERATE)
    }
}
