package com.jules.tkinterdesigner.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jules.tkinterdesigner.filetype.TkinterDesignFileType
import com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY
import com.jules.tkinterdesigner.model.DesignedDialog
import kotlinx.serialization.json.Json
import com.intellij.openapi.diagnostic.thisLogger // For logging

class TkinterDesignerFileEditorProvider : FileEditorProvider, DumbAware {

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == TkinterDesignFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        // Attempt to load dialog from file content if not already in UserData
        // (UserData might be populated by OpenTkinterDesignerAction for new files)
        if (file.getUserData(DESIGNED_DIALOG_KEY) == null && file.exists() && file.length > 0) {
            try {
                val jsonString = String(file.contentsToByteArray(), Charsets.UTF_8)
                if (jsonString.isNotBlank()) {
                    val loadedDialog = json.decodeFromString(DesignedDialog.serializer(), jsonString)
                    file.putUserData(DESIGNED_DIALOG_KEY, loadedDialog)
                } else {
                    // File is empty, ensure a new dialog is created by editor or action
                    thisLogger().warn("Opened an empty .tkdesign file: ${file.path}. A new dialog will be used.")
                }
            } catch (e: Exception) {
                thisLogger().error("Error decoding DesignedDialog from file: ${file.path}", e)
                // If decoding fails, a new dialog will be created in TkinterDesignerFileEditor's init
            }
        }
        // If it's a new LightVirtualFile, OpenTkinterDesignerAction should have already put a new dialog in UserData.
        // TkinterDesignerFileEditor's init also has fallback to create a new dialog if UserData is null.
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
