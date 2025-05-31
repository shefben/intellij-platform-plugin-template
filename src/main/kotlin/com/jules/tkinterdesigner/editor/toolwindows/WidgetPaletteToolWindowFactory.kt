package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class WidgetPaletteToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val widgetPaletteComponent = WidgetPaletteComponent(project)
        val contentFactory = ContentFactory.getInstance() // Use updated API
        val content = contentFactory.createContent(widgetPaletteComponent, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
