package com.jules.tkinterdesigner.editor

import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.FileEditorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.jules.tkinterdesigner.model.DesignedDialog // For user data key
import com.jules.tkinterdesigner.toolWindow.TkinterDesignerPanel // Corrected import
import javax.swing.JComponent

// Define a key for storing DesignedDialog in VirtualFile's user data (optional for now, but good practice)
import com.intellij.openapi.wm.ToolWindowManager

// val DESIGNED_DIALOG_KEY: Key<DesignedDialog> = Key.create("TkinterDesigner.DesignedDialog")

class TkinterDesignerFileEditor(private val project: Project, private val virtualFile: VirtualFile) : FileEditorBase() {

    private val designerPanel: TkinterDesignerPanel

    init {
        val dialog = virtualFile.getUserData(com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY)
            ?: run {
                // Fallback if not found, though action should always set it.
                // Log this or handle as an error if critical.
                println("Warning: DESIGNED_DIALOG_KEY not found in VirtualFile. Creating new Dialog.")
                val newDialog = DesignedDialog()
                newDialog.title = virtualFile.nameWithoutExtension
                virtualFile.putUserData(com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY, newDialog) // Store it back
                newDialog
            }
        designerPanel = TkinterDesignerPanel(project, dialog)
    }

    override fun getComponent(): JComponent = designerPanel

    override fun getPreferredFocusedComponent(): JComponent? = designerPanel.visualCanvasPanel

    override fun getName(): String = virtualFile.name // Or a more descriptive name

    override fun setState(state: FileEditorState) {
        // TODO: Implement state restoration if needed
    }

    override fun isModified(): Boolean {
        // TODO: Implement based on changes to designerPanel.visualCanvasPanel.currentDesign
        return false // For now
    }

    override fun isValid(): Boolean = virtualFile.isValid

    override fun dispose() {
        // Release resources if any
        super.dispose()
    }

    // TODO: Implement save functionality if isModified can be true
    // override fun selectNotify() {}
    // override fun deselectNotify() {}
    // override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    // override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    // override fun getCurrentLocation(): com.intellij.openapi.fileEditor.FileEditorLocation? = null

    override fun selectNotify() {
        super.selectNotify()
        // Show tool windows when this editor is selected
        val toolWindowManager = ToolWindowManager.getInstance(project)
        toolWindowManager.getToolWindow("TkinterDesigner.WidgetPalette")?.show(null)
        toolWindowManager.getToolWindow("TkinterDesigner.PropertyEditor")?.show(null)

        // Publish selection of the root dialog or currently selected widget within it
        // This ensures the property editor updates if it was closed/reopened or missed an event
        val dialog = virtualFile.getUserData(com.jules.tkinterdesigner.model.DESIGNED_DIALOG_KEY)
        // Find the currently selected widget ID if any (this state isn't persisted yet, so it would be null on first open)
        val selectedWidget = designerPanel.visualCanvasPanel.currentDesign.widgets.find {
            it.id == designerPanel.visualCanvasPanel.getSelectedWidgetId() // Need a getter for selectedWidgetId
        }
         project.messageBus.syncPublisher(com.jules.tkinterdesigner.messaging.WIDGET_SELECTION_TOPIC)
            .widgetSelected(selectedWidget, dialog)

    }

    override fun deselectNotify() {
        super.deselectNotify()
        // Optionally hide tool windows when this editor is deselected
        // val toolWindowManager = ToolWindowManager.getInstance(project)
        // toolWindowManager.getToolWindow("TkinterDesigner.WidgetPalette")?.hide(null)
        // toolWindowManager.getToolWindow("TkinterDesigner.PropertyEditor")?.hide(null)
    }
}
