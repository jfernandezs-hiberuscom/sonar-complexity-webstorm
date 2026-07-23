package com.github.sonarcomplexity.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ComplexitySettingsConfigurable : Configurable {

    private val enableInlayHintsCheckBox = JBCheckBox("Enable inline inlay hints above function declarations")
    private val enableInspectionsCheckBox = JBCheckBox("Enable local code inspection warnings in editor")
    private val showCyclomaticCheckBox = JBCheckBox("Show Cyclomatic complexity alongside Cognitive complexity")
    private val warningThresholdField = JBTextField()
    private val criticalThresholdField = JBTextField()

    private var settingsPanel: JPanel? = null

    override fun getDisplayName(): String = "Sonar Complexity"

    override fun createComponent(): JComponent {
        settingsPanel = FormBuilder.createFormBuilder()
            .addComponent(enableInlayHintsCheckBox)
            .addComponent(enableInspectionsCheckBox)
            .addComponent(showCyclomaticCheckBox)
            .addLabeledComponent(JBLabel("Warning Threshold (Moderate):"), warningThresholdField)
            .addLabeledComponent(JBLabel("Critical Threshold (High):"), criticalThresholdField)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val state = ComplexitySettingsState.instance
        return enableInlayHintsCheckBox.isSelected != state.enableInlayHints ||
                enableInspectionsCheckBox.isSelected != state.enableInspections ||
                showCyclomaticCheckBox.isSelected != state.showCyclomaticComplexity ||
                warningThresholdField.text != state.warningThreshold.toString() ||
                criticalThresholdField.text != state.criticalThreshold.toString()
    }

    override fun apply() {
        val state = ComplexitySettingsState.instance
        state.enableInlayHints = enableInlayHintsCheckBox.isSelected
        state.enableInspections = enableInspectionsCheckBox.isSelected
        state.showCyclomaticComplexity = showCyclomaticCheckBox.isSelected
        state.warningThreshold = warningThresholdField.text.toIntOrNull() ?: 15
        state.criticalThreshold = criticalThresholdField.text.toIntOrNull() ?: 25
    }

    override fun reset() {
        val state = ComplexitySettingsState.instance
        enableInlayHintsCheckBox.isSelected = state.enableInlayHints
        enableInspectionsCheckBox.isSelected = state.enableInspections
        showCyclomaticCheckBox.isSelected = state.showCyclomaticComplexity
        warningThresholdField.text = state.warningThreshold.toString()
        criticalThresholdField.text = state.criticalThreshold.toString()
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
