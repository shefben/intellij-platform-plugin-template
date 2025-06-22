package org.jetbrains.plugins.template.tkdesigner

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.template.TkDesignerBundle

class OpenDesignerAction : AnAction(TkDesignerBundle.message("action.openDesigner.text")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("TkinterDesigner")?.show()
    }
}
