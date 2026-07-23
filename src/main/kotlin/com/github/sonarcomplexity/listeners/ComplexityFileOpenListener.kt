package com.github.sonarcomplexity.listeners

import com.github.sonarcomplexity.engine.CognitiveComplexityCalculator
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor

class ComplexityFileOpenListener(private val project: Project) : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        
        // Only run for JS/TS files
        val ext = file.extension?.lowercase() ?: ""
        if (ext != "js" && ext != "ts" && ext != "jsx" && ext != "tsx") return
        
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
        
        if (functions.isEmpty()) return
        
        var totalCognitive = 0
        functions.forEach { func ->
            val text = func.text.take(40).replace("\n", " ")
            val name = when {
                text.contains("function") -> text.substringAfter("function").substringBefore("(").trim()
                text.contains("=") -> text.substringBefore("=").trim()
                else -> "anonymous"
            }.ifEmpty { "function" }
            val result = CognitiveComplexityCalculator.calculate(func, name)
            totalCognitive += result.cognitiveComplexity
        }
        
        val notificationContent = "Opened '${file.name}'. Total Cognitive Complexity: $totalCognitive across ${functions.size} functions."
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("Sonar Complexity Notifications")
        if (notificationGroup != null) {
            val notification = notificationGroup.createNotification(
                "Sonar Complexity",
                notificationContent,
                NotificationType.INFORMATION
            )
            notification.notify(project)
        }
    }
}
