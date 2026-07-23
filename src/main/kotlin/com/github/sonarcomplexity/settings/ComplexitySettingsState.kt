package com.github.sonarcomplexity.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.sonarcomplexity.settings.ComplexitySettingsState",
    storages = [Storage("SonarComplexitySettings.xml")]
)
class ComplexitySettingsState : PersistentStateComponent<ComplexitySettingsState> {

    var enableInlayHints: Boolean = true
    var enableInspections: Boolean = true
    var warningThreshold: Int = 15
    var criticalThreshold: Int = 25
    var showCyclomaticComplexity: Boolean = true

    override fun getState(): ComplexitySettingsState = this

    override fun loadState(state: ComplexitySettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: ComplexitySettingsState
            get() = ApplicationManager.getApplication().getService(ComplexitySettingsState::class.java)
    }
}
