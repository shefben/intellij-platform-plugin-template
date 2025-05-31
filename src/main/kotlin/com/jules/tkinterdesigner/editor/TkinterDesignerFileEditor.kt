package com.jules.tkinterdesigner.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.FileEditorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile // For new files
import com.intellij.util.messages.MessageBusConnection
import com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget // For listener
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.messaging.WidgetPropertyListener
import com.jules.tkinterdesigner.toolWindow.TkinterDesignerPanel
import kotlinx.serialization.json.Json
import javax.swing.JComponent

class TkinterDesignerFileEditor(private val project: Project, private val virtualFile: VirtualFile) : FileEditorBase() {

    private val designerPanel: TkinterDesignerPanel
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    private var isDirtyInternal: Boolean = false
    private var projectMessageBusConnection: MessageBusConnection? = null


    init {
        var dialog = virtualFile.getUserData(DESIGNED_DIALOG_KEY)
        if (dialog == null && virtualFile !is LightVirtualFile && virtualFile.exists() && virtualFile.length > 0) { // Try loading from actual file content
            try {
                val jsonString = String(virtualFile.contentsToByteArray(), Charsets.UTF_8)
                if (jsonString.isNotBlank()) {
                    dialog = json.decodeFromString(DesignedDialog.serializer(), jsonString)
                    virtualFile.putUserData(DESIGNED_DIALOG_KEY, dialog) // Cache in user data
                }
            } catch (e: Exception) {
                thisLogger().error("Error decoding DesignedDialog from VirtualFile content: ${virtualFile.path}", e)
                // Fallback to new dialog if load fails
            }
        }

        if (dialog == null) { // Still null (new LightVirtualFile or error loading existing file)
            dialog = DesignedDialog()
            dialog.title = virtualFile.nameWithoutExtension
            virtualFile.putUserData(DESIGNED_DIALOG_KEY, dialog)
            if (virtualFile !is LightVirtualFile) isDirtyInternal = true // Mark new non-Light files as dirty to prompt save
        }

        designerPanel = TkinterDesignerPanel(project, dialog)
        setupListeners()
    }

    private fun setupListeners() {
        projectMessageBusConnection = project.messageBus.connect(this) // Use 'this' as disposable
        projectMessageBusConnection?.subscribe(WIDGET_MODIFIED_TOPIC, object : WidgetPropertyListener {
            override fun propertyChanged(widget: DesignedWidget, dialog: DesignedDialog?) {
                if (dialog == virtualFile.getUserData(DESIGNED_DIALOG_KEY)) {
                    markDirty(true)
                }
            }
        })

        // Listen to IDE save events to push model to LightVirtualFile content
        // For real files, the IDE handles it if Document is saved.
        // For LightVirtualFile, we need to ensure its content is updated before IDE "saves" it (e.g. for undo buffer).
        // Using AppTopics.FILE_DOCUMENT_SYNC for this.
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(com.intellij.AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    val file = FileDocumentManager.getInstance().getFile(document)
                    if (file == virtualFile) {
                        saveChangesToVirtualFileContent()
                    }
                }
            })
    }


    private fun markDirty(dirty: Boolean) {
        isDirtyInternal = dirty
        // Notify the IDE that the editor's modified state has changed.
        // This is important for '*' in tab, save prompts, etc.
        ApplicationManager.getApplication().invokeLater {
             propertyChangeSupport.firePropertyChange(FileEditorBase.PROP_MODIFIED, !dirty, dirty)
        }
    }

    // Pushes the current DesignedDialog model into the LightVirtualFile's in-memory content.
    // This is called before IntelliJ attempts to "save" the LightVirtualFile (e.g. for its internal state/history).
    private fun saveChangesToVirtualFileContent() {
        if (!isModified && virtualFile !is LightVirtualFile) return // Only save if modified, or if it's a LightVirtualFile (always update its content)

        val dialog = virtualFile.getUserData(DESIGNED_DIALOG_KEY) ?: return
        try {
            val jsonString = json.encodeToString(DesignedDialog.serializer(), dialog)
            // For LightVirtualFile, setContent updates its byte array.
            // For real files, this step would be a direct write to disk if not using IntelliJ's Document system.
            // Since we are trying to integrate with IDE save, updating content for LightVirtualFile is key.
            if (virtualFile is LightVirtualFile) {
                virtualFile.setContent(null, jsonString, System.currentTimeMillis())
            } else {
                // For non-LightVirtualFiles, rely on Document saving after this point.
                // Ensure the Document itself is updated if not already.
                // This might involve FileDocumentManager.getInstance().getDocument(virtualFile)?.setText(jsonString)
                // but that could trigger another save cycle.
                // For now, assume direct save for non-Light files or that Document is already in sync via other means.
                // The beforeDocumentSaving listener is the primary hook.
                // If we are saving a "real" file, this method is called *before* its document is saved.
                // We need to ensure the document has the latest content.
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                if (document != null) {
                    ApplicationManager.getApplication().runWriteAction { // Must be in write action to modify document
                        document.setText(jsonString)
                    }
                }
            }
            markDirty(false)
        } catch (e: Exception) {
            thisLogger().error("Error serializing design to VirtualFile content: ${e.message}", e)
        }
    }


    override fun getComponent(): JComponent = designerPanel
    override fun getPreferredFocusedComponent(): JComponent? = designerPanel.visualCanvasPanel
    override fun getName(): String = virtualFile.name
    override fun setState(state: FileEditorState) { /* TODO */ }
    override fun isModified(): Boolean = isDirtyInternal
    override fun isValid(): Boolean = virtualFile.isValid

    override fun dispose() {
        projectMessageBusConnection?.disconnect()
        // Other disposals if needed
        super.dispose()
    }
}
