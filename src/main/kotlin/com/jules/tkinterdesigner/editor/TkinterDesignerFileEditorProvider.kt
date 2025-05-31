package com.jules.tkinterdesigner.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jules.tkinterdesigner.filetype.TkinterDesignFileType

class TkinterDesignerFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == TkinterDesignFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TkinterDesignerFileEditor(project, file)
    }

    override fun getEditorTypeId(): String {
        return "tkinter-visual-designer-editor" // Unique ID for this editor type
    }

    override fun getPolicy(): FileEditorPolicy {
        // Hides the default text editor for this file type, showing our designer instead.
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}
