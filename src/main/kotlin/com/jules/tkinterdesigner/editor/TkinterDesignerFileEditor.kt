package com.jules.tkinterdesigner.editor

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction // For Document modification
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.FileEditorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY
import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.messaging.WIDGET_MODIFIED_TOPIC
import com.jules.tkinterdesigner.messaging.WidgetPropertyListener
import com.jules.tkinterdesigner.toolWindow.TkinterDesignerPanel
import kotlinx.serialization.json.Json
import javax.swing.JComponent

class TkinterDesignerFileEditor(private val project: Project, private val virtualFile: VirtualFile) : FileEditorBase() {

    private val designerPanel: TkinterDesignerPanel
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    private var isDirtyInternal: Boolean = false
    // private var projectMessageBusConnection: MessageBusConnection? = null // Disposed by 'this' as disposable

    companion object {
        private const val NOTIFICATION_GROUP_ID = "TkinterDesigner"
    }

    init {
        var dialog = virtualFile.getUserData(DESIGNED_DIALOG_KEY)
        var loadedFromFile = false

        if (dialog == null && virtualFile !is LightVirtualFile && virtualFile.exists() && virtualFile.length > 0) {
            try {
                val jsonString = String(virtualFile.contentsToByteArray(), Charsets.UTF_8)
                if (jsonString.isNotBlank()) {
                    dialog = json.decodeFromString(DesignedDialog.serializer(), jsonString)
                    virtualFile.putUserData(DESIGNED_DIALOG_KEY, dialog)
                    loadedFromFile = true // Successfully loaded from existing file content
                    thisLogger().info("Loaded Tkinter design from file: ${virtualFile.path}")
                } else {
                     thisLogger().warn("Opened an empty .tkdesign file: ${virtualFile.path}. A new dialog will be used.")
                }
            } catch (e: Exception) {
                thisLogger().error("Error decoding DesignedDialog from VirtualFile content: ${virtualFile.path}", e)
                NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification("Failed to load Tkinter design", "Error: ${e.message}", NotificationType.ERROR)
                    .notify(project)
                // Fallback to new dialog if load fails
            }
        }

        if (dialog == null) {
            dialog = DesignedDialog()
            dialog.title = virtualFile.nameWithoutExtension
            virtualFile.putUserData(DESIGNED_DIALOG_KEY, dialog)
            // Only mark dirty if it's a new file that's not a LightVirtualFile (which are in-memory until first save)
            // Or if it IS a LightVirtualFile, it's inherently "new" and needs saving to persist.
            isDirtyInternal = true
        } else if (loadedFromFile) {
            isDirtyInternal = false // Loaded from file, initially not dirty
        }

        designerPanel = TkinterDesignerPanel(project, dialog)
        setupListeners()
        // After loading, ensure the dirty state reflects whether a new dialog was created for an existing file path or if it's a new LVF.
        // This is handled by setting isDirtyInternal = true for new LVFs or if dialog was created for an existing file.
        // If loadedFromFile is true, it's initially clean.
        if (isDirtyInternal) { // If it was marked dirty during init (e.g. new LightVirtualFile)
             ApplicationManager.getApplication().invokeLater {
                  propertyChangeSupport.firePropertyChange(FileEditorBase.PROP_MODIFIED, false, true)
             }
        }
    }

    private fun setupListeners() {
        val busConnection = project.messageBus.connect(this)
        busConnection.subscribe(WIDGET_MODIFIED_TOPIC, object : WidgetPropertyListener {
            override fun propertyChanged(widget: DesignedWidget, dialog: DesignedDialog?) {
                if (dialog == virtualFile.getUserData(DESIGNED_DIALOG_KEY)) {
                    markDirty(true)
                }
            }
        })

        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(com.intellij.AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    val file = FileDocumentManager.getInstance().getFile(document)
                    if (file == virtualFile) {
                        saveChangesToDocument(document)
                    }
                }
            })
    }

    private fun markDirty(dirty: Boolean) {
        val oldIsDirty = isDirtyInternal
        isDirtyInternal = dirty
        if (oldIsDirty != dirty) {
            ApplicationManager.getApplication().invokeLater {
                 propertyChangeSupport.firePropertyChange(FileEditorBase.PROP_MODIFIED, oldIsDirty, dirty)
            }
        }
    }

    private fun saveChangesToDocument(document: Document) {
        val dialog = virtualFile.getUserData(DESIGNED_DIALOG_KEY) ?: return
        try {
            val jsonString = json.encodeToString(DesignedDialog.serializer(), dialog)

            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(jsonString)
            }
            // FileDocumentManager.getInstance().saveDocument(document) // IDE will do this after beforeDocumentSaving
            thisLogger().info("Tkinter design saved to document: ${virtualFile.path}")
            markDirty(false)
        } catch (e: Exception) {
            thisLogger().error("Error serializing design to Document: ${e.message}", e)
            NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification("Failed to save Tkinter design", "Error: ${e.message}", NotificationType.ERROR)
                .notify(project)
        }
    }

    // This method can be used if we need to update LightVirtualFile content directly,
    // but for IDE integration, updating the Document is preferred when one exists.
    @Suppress("unused")
    private fun saveChangesToLightVirtualFile() {
        if (virtualFile !is LightVirtualFile) return
        if (!isModified) return

        val dialog = virtualFile.getUserData(DESIGNED_DIALOG_KEY) ?: return
        try {
            val jsonString = json.encodeToString(DesignedDialog.serializer(), dialog)
            virtualFile.setContent(null, jsonString.toByteArray(Charsets.UTF_8), System.currentTimeMillis())
            thisLogger().info("Tkinter design content updated for LightVirtualFile: ${virtualFile.name}")
            markDirty(false)
        } catch (e: Exception) {
            thisLogger().error("Error serializing design to LightVirtualFile: ${e.message}", e)
        }
    }

    override fun getComponent(): JComponent = designerPanel
    override fun getPreferredFocusedComponent(): JComponent? = designerPanel.visualCanvasPanel
    override fun getName(): String = virtualFile.name
    override fun setState(state: FileEditorState) { /* TODO */ }
    override fun isModified(): Boolean = isDirtyInternal
    override fun isValid(): Boolean = virtualFile.isValid && virtualFile.getUserData(DESIGNED_DIALOG_KEY) != null


    override fun dispose() {
        // projectMessageBusConnection was changed to be disposed by "this"
        super.dispose()
    }
}
