# Complete Guide: Building a Sonar-Complexity Plugin for WebStorm

This document provides complete, step-by-step instructions to create, build, and install a **WebStorm / IntelliJ Platform Plugin** that calculates function complexity (Cognitive Complexity S3776 & Cyclomatic Complexity), replicating the VS Code [`sonar-complexity`](https://github.com/kevinjshah2207/sonar-complexity) extension.

---

## 1. Project Overview & File Structure

Create a directory named `sonar-complexity-webstorm/` with the following structure:

```
sonar-complexity-webstorm/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── README.md
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── META-INF/
    │   │       └── plugin.xml
    │   └── kotlin/
    │       └── com/github/sonarcomplexity/
    │           ├── engine/
    │           │   ├── CognitiveComplexityCalculator.kt
    │           │   └── ComplexityResult.kt
    │           ├── inlay/
    │           │   └── ComplexityLineMarkerProvider.kt
    │           ├── inspection/
    │           │   └── ComplexityInspectionTool.kt
    │           └── settings/
    │               ├── ComplexitySettingsConfigurable.kt
    │               └── ComplexitySettingsState.kt
    └── test/
        └── kotlin/
            └── com/github/sonarcomplexity/
                └── engine/
                    └── CognitiveComplexityCalculatorTest.kt
```

---

## 2. Configuration Files

### `settings.gradle.kts`
```kotlin
rootProject.name = "sonar-complexity-webstorm"
```

### `gradle.properties`
```properties
pluginGroup = com.github.sonarcomplexity
pluginVersion = 1.0.0
platformType = WS
platformVersion = 2024.1
kotlin.code.style = official
```

### `build.gradle.kts`
```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij.platform") version "2.0.0-beta6"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        webstorm(providers.gradleProperty("platformVersion").get())
        bundledPlugins("JavaScript")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.sonarcomplexity.webstorm"
        name = "Sonar Complexity"
        version = providers.gradleProperty("pluginVersion").get()
        description = "Calculates Cognitive and Cyclomatic complexity metrics for functions inline in WebStorm."

        vendor {
            name = "Antigravity Team"
            url = "https://github.com/kevinjshah2207/sonar-complexity"
        }
        
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "243.*"
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    test {
        useJUnitPlatform()
    }
}
```

### `src/main/resources/META-INF/plugin.xml`
```xml
<idea-plugin>
    <id>com.github.sonarcomplexity.webstorm</id>
    <name>Sonar Complexity</name>
    <vendor url="https://github.com/kevinjshah2207/sonar-complexity">Antigravity</vendor>

    <description><![CDATA[
    Surfaces function-level SonarQube Cognitive Complexity (S3776) and Cyclomatic Complexity directly in WebStorm.<br>
    <ul>
        <li>Gutter & Line markers next to function declarations</li>
        <li>Customizable complexity thresholds</li>
        <li>Inspection warnings in the Problems view</li>
    </ul>
    ]]></description>

    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.lang</depends>

    <extensions defaultExtensionNs="com.intellij">
        <applicationService serviceImplementation="com.github.sonarcomplexity.settings.ComplexitySettingsState"/>
        <applicationConfigurable instance="com.github.sonarcomplexity.settings.ComplexitySettingsConfigurable"
                                 id="com.github.sonarcomplexity.settings"
                                 displayName="Sonar Complexity"/>

        <codeInsight.lineMarkerProvider
                language=""
                implementationClass="com.github.sonarcomplexity.inlay.ComplexityLineMarkerProvider"/>

        <localInspection
                language=""
                displayName="High Cognitive Complexity Function"
                groupPath="Code metrics"
                groupName="Code complexity"
                enabledByDefault="true"
                level="WARNING"
                implementationClass="com.github.sonarcomplexity.inspection.ComplexityInspectionTool"/>
    </extensions>
</idea-plugin>
```

---

## 3. Core Logic Implementation

### `ComplexityResult.kt`
```kotlin
package com.github.sonarcomplexity.engine

enum class ComplexitySeverity {
    LOW, MODERATE, HIGH;

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
```

### `CognitiveComplexityCalculator.kt`
```kotlin
package com.github.sonarcomplexity.engine

import com.intellij.psi.PsiElement

object CognitiveComplexityCalculator {

    fun calculate(functionElement: PsiElement, functionName: String = "function"): ComplexityResult {
        var cognitiveScore = 0
        var cyclomaticScore = 1
        val breakdown = mutableListOf<ComplexityBreakdownItem>()

        fun walk(element: PsiElement, nestingLevel: Int) {
            for (child in element.children) {
                val typeName = child.node?.elementType?.toString() ?: ""
                val text = child.text

                var isControlFlow = false
                var isNestingBoundary = false
                var description = ""

                when {
                    typeName.contains("IF_STATEMENT") || text.startsWith("if") -> {
                        isControlFlow = true; isNestingBoundary = true; description = "if statement"
                    }
                    typeName.contains("FOR_STATEMENT") || text.startsWith("for") -> {
                        isControlFlow = true; isNestingBoundary = true; description = "for loop"
                    }
                    typeName.contains("WHILE_STATEMENT") || text.startsWith("while") -> {
                        isControlFlow = true; isNestingBoundary = true; description = "while loop"
                    }
                    typeName.contains("SWITCH_STATEMENT") || text.startsWith("switch") -> {
                        isControlFlow = true; isNestingBoundary = true; description = "switch statement"
                    }
                    typeName.contains("CATCH") || text.startsWith("catch") -> {
                        isControlFlow = true; isNestingBoundary = true; description = "catch clause"
                    }
                    typeName.contains("BINARY_EXPRESSION") && (text.contains("&&") || text.contains("||")) -> {
                        cognitiveScore += 1
                        cyclomaticScore += 1
                    }
                }

                if (isControlFlow) {
                    val increment = 1 + nestingLevel
                    cognitiveScore += increment
                    cyclomaticScore += 1
                }

                val nextNesting = if (isNestingBoundary) nestingLevel + 1 else nestingLevel
                walk(child, nextNesting)
            }
        }

        walk(functionElement, 0)

        return ComplexityResult(
            functionName = functionName,
            cognitiveComplexity = cognitiveScore,
            cyclomaticComplexity = cyclomaticScore,
            severity = ComplexitySeverity.fromScore(cognitiveScore)
        )
    }
}
```

---

## 4. UI & Inspection Providers

### `ComplexityLineMarkerProvider.kt`
```kotlin
package com.github.sonarcomplexity.inlay

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
import com.github.sonarcomplexity.engine.ComplexitySeverity
import com.github.sonarcomplexity.settings.ComplexitySettingsState
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class ComplexityLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val settings = ComplexitySettingsState.instance
        if (!settings.enableInlayHints) return null
        if (!isFunctionDeclaration(element)) return null

        val functionName = getFunctionName(element)
        val result = CognitiveComplexityCalculator.calculate(element, functionName)

        val badgeText = if (settings.showCyclomaticComplexity) {
            "Cognitive: ${result.cognitiveComplexity} | Cyclomatic: ${result.cyclomaticComplexity}"
        } else {
            "Cognitive Complexity: ${result.cognitiveComplexity}"
        }

        val iconText = when (result.severity) {
            ComplexitySeverity.LOW -> "🟢"
            ComplexitySeverity.MODERATE -> "🟡"
            ComplexitySeverity.HIGH -> "🔴"
        }

        val tooltip = "Sonar Complexity: $badgeText"
        val firstChild = element.firstChild ?: element

        return LineMarkerInfo(
            firstChild,
            firstChild.textRange,
            BadgeIcon(iconText),
            { tooltip },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    private fun isFunctionDeclaration(element: PsiElement): Boolean {
        val typeName = element.node?.elementType?.toString() ?: ""
        return typeName.contains("FUNCTION") || typeName.contains("METHOD") || typeName.contains("ARROW_FUNCTION")
    }

    private fun getFunctionName(element: PsiElement): String {
        val text = element.text.take(40).replace("\n", " ")
        return when {
            text.contains("function") -> text.substringAfter("function").substringBefore("(").trim()
            text.contains("=") -> text.substringBefore("=").trim()
            else -> "anonymous"
        }.ifEmpty { "function" }
    }

    private class BadgeIcon(private val symbol: String) : Icon {
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            g.drawString(symbol, x, y + 12)
        }
        override fun getIconWidth(): Int = 14
        override fun getIconHeight(): Int = 14
    }
}
```

### `ComplexityInspectionTool.kt`
```kotlin
package com.github.sonarcomplexity.inspection

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
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
                val typeName = element.node?.elementType?.toString() ?: ""
                if (typeName.contains("FUNCTION") || typeName.contains("METHOD")) {
                    val result = CognitiveComplexityCalculator.calculate(element, "function")
                    if (result.cognitiveComplexity >= settings.warningThreshold) {
                        holder.registerProblem(
                            element.firstChild ?: element,
                            "Function has Cognitive Complexity of ${result.cognitiveComplexity} (threshold: ${settings.warningThreshold})."
                        )
                    }
                }
            }
        }
    }
}
```

### `ComplexitySettingsState.kt`
```kotlin
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
```

### `ComplexitySettingsConfigurable.kt`
```kotlin
package com.github.sonarcomplexity.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ComplexitySettingsConfigurable : Configurable {

    private val enableInlayHintsCheckBox = JBCheckBox("Enable inline markers next to function declarations")
    private val enableInspectionsCheckBox = JBCheckBox("Enable local inspection warnings in editor")
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
```

---

## 5. How to Build & Install into WebStorm

### Building the Plugin `.zip`
Run in terminal inside the project directory:
```bash
gradle buildPlugin
```

The output zip file will be generated at:
`build/distributions/sonar-complexity-webstorm-1.0.0.zip`

### Installing into WebStorm
1. Open **WebStorm**.
2. Go to **Settings** (`Ctrl+Alt+S` on Linux/Windows, `Cmd+,` on macOS).
3. Select **Plugins**.
4. Click the ⚙️ **Gear Icon** at the top right of the Plugins tab.
5. Click **Install Plugin from Disk...**.
6. Select `build/distributions/sonar-complexity-webstorm-1.0.0.zip`.
7. Click **Apply** and restart WebStorm when prompted.
