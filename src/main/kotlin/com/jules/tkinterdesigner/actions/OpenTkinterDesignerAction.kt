package com.jules.tkinterdesigner.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class OpenTkinterDesignerAction : AnAction(), DumbAware {

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.LightVirtualFile
import com.jules.tkinterdesigner.filetype.TkinterDesignFileType
import com.jules.tkinterdesigner.model.DesignedDialog // Import for DesignedDialog
import com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY // Import for the Key

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Create a unique name for the new file
        val fileName = "Untitled_${System.currentTimeMillis()}.tkdesign"

        // Create a LightVirtualFile. This file exists only in memory.
        val virtualFile = LightVirtualFile(fileName, TkinterDesignFileType, "")
        virtualFile.isWritable = true

        // Create a fresh dialog model and store it in the VirtualFile's user data
        val newDialog = DesignedDialog()
        newDialog.title = virtualFile.nameWithoutExtension // Set dialog title from filename
        virtualFile.putUserData(DESIGNED_DIALOG_KEY, newDialog)

        // Open the virtual file in the editor
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    override fun update(e: AnActionEvent) {
        // Action is enabled and visible only if a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
