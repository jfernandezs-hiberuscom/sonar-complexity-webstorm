package com.github.sonarcomplexity.inlay

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
import com.github.sonarcomplexity.engine.ComplexitySeverity
import com.github.sonarcomplexity.settings.ComplexitySettingsState
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class ComplexityLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val settings = ComplexitySettingsState.instance
        if (!settings.enableInlayHints) return null

        val functionAncestor = getFunctionAncestor(element) ?: return null
        // Anchor the marker on the name identifier or first leaf of the function
        val anchor = getAnchorElement(functionAncestor)
        if (element != anchor) return null

        val functionName = getFunctionName(functionAncestor)
        val result = CognitiveComplexityCalculator.calculate(functionAncestor, functionName)

        val badgeText = if (settings.showCyclomaticComplexity) {
            "Cognitive: ${result.cognitiveComplexity} | Cyclomatic: ${result.cyclomaticComplexity}"
        } else {
            "Cognitive Complexity: ${result.cognitiveComplexity}"
        }

        val cogColor = when (result.severity) {
            ComplexitySeverity.LOW -> JBColor(Color(46, 125, 50), Color(76, 175, 80))
            ComplexitySeverity.MODERATE -> JBColor(Color(230, 81, 0), Color(255, 152, 0))
            ComplexitySeverity.HIGH -> JBColor(Color(198, 40, 40), Color(239, 83, 80))
        }

        val cycColor = when {
            result.cyclomaticComplexity >= 20 -> JBColor(Color(198, 40, 40), Color(239, 83, 80))
            result.cyclomaticComplexity >= 10 -> JBColor(Color(230, 81, 0), Color(255, 152, 0))
            else -> JBColor(Color(46, 125, 50), Color(76, 175, 80))
        }

        val tooltip = "Sonar Complexity: $badgeText"

        val icon = if (settings.showCyclomaticComplexity) {
            DoubleBadgeIcon(
                text1 = "cog: ${result.cognitiveComplexity}",
                color1 = cogColor,
                text2 = "cyc: ${result.cyclomaticComplexity}",
                color2 = cycColor
            )
        } else {
            DoubleBadgeIcon(
                text1 = "complexity: ${result.cognitiveComplexity}",
                color1 = cogColor,
                text2 = null,
                color2 = null
            )
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltip },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    private fun getFunctionAncestor(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            val typeName = current.node?.elementType?.toString() ?: ""
            if (!typeName.contains("KEYWORD") && !typeName.contains("IDENTIFIER") && 
                (typeName.contains("FUNCTION") || typeName.contains("METHOD"))) {
                return current
            }
            if (current is com.intellij.psi.PsiFile) break
            current = current.parent
        }
        return null
    }

    private fun getAnchorElement(functionElement: PsiElement): PsiElement {
        if (functionElement is com.intellij.psi.PsiNameIdentifierOwner) {
            val nameId = functionElement.nameIdentifier
            if (nameId != null) return nameId
        }
        return getFirstLeaf(functionElement)
    }

    private fun getFirstLeaf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) {
            current = current.firstChild!!
        }
        return current
    }

    private fun getFunctionName(element: PsiElement): String {
        val text = element.text.take(40).replace("\n", " ")
        return when {
            text.contains("function") -> text.substringAfter("function").substringBefore("(").trim()
            text.contains("=") -> text.substringBefore("=").trim()
            else -> "anonymous"
        }.ifEmpty { "function" }
    }

    private class DoubleBadgeIcon(
        private val text1: String,
        private val color1: Color,
        private val text2: String? = null,
        private val color2: Color? = null
    ) : Icon {
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as java.awt.Graphics2D
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g2.font = g2.font.deriveFont(java.awt.Font.BOLD, 9f)
            val fm = g2.fontMetrics

            // Paint first badge
            val rectWidth1 = 12 + text1.length * 6
            val rectHeight = 14
            g2.color = color1
            g2.fillRoundRect(x + 1, y + 1, rectWidth1 - 2, rectHeight - 2, 6, 6)

            g2.color = Color.WHITE
            val textX1 = x + 1 + (rectWidth1 - 2 - fm.stringWidth(text1)) / 2
            val textY1 = y + 1 + (rectHeight - 2 - fm.height) / 2 + fm.ascent
            g2.drawString(text1, textX1, textY1)

            // Paint second badge if present
            if (text2 != null && color2 != null) {
                val rectWidth2 = 12 + text2.length * 6
                val startX2 = x + rectWidth1 + 2
                g2.color = color2
                g2.fillRoundRect(startX2 + 1, y + 1, rectWidth2 - 2, rectHeight - 2, 6, 6)

                g2.color = Color.WHITE
                val textX2 = startX2 + 1 + (rectWidth2 - 2 - fm.stringWidth(text2)) / 2
                g2.drawString(text2, textX2, textY1)
            }

            g2.dispose()
        }

        override fun getIconWidth(): Int {
            val width1 = 12 + text1.length * 6
            if (text2 != null) {
                val width2 = 12 + text2.length * 6
                return width1 + width2 + 2
            }
            return width1
        }

        override fun getIconHeight(): Int = 14
    }
}
