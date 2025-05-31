package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane // Import JBScrollPane
import com.intellij.ui.content.ContentFactory

class PropertyEditorToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val propertyEditorComponent = PropertyEditorComponent(project)
        // It's common to wrap content in a scroll pane, especially for property editors
        val scrollPane = JBScrollPane(propertyEditorComponent)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(scrollPane, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
