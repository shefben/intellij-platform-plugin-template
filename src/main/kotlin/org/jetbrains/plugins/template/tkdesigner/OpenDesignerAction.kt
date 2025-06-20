package org.jetbrains.plugins.template.tkdesigner

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class OpenDesignerAction : AnAction("Open Tkinter Designer") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("TkinterDesigner")?.show()
    }
}
