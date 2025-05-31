package com.jules.tkinterdesigner.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class OpenTkinterDesignerAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowManager = ToolWindowManager.getInstance(project)
        // Ensure this ID matches the one in plugin.xml's <toolWindow id="..."/>
        val toolWindow = toolWindowManager.getToolWindow("TkinterDesignerWindow")
        toolWindow?.activate(null)
    }

    override fun update(e: AnActionEvent) {
        // Action is enabled and visible only if a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
