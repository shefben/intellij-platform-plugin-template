package com.jules.tkinterdesigner.toolWindow // Keep in toolWindow package for now, or move to 'editor' or 'ui' later

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.jules.tkinterdesigner.codegen.TkinterCodeGenerator
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.WidgetPaletteInfo
import com.jules.tkinterdesigner.model.WidgetPropertyRegistry
import com.jules.tkinterdesigner.model.DesignedDialog
import java.awt.BorderLayout


// TkinterDesignerPanel is now the main UI for the editor, primarily housing the VisualCanvasPanel.
// It receives the DesignedDialog from the FileEditor.
internal class TkinterDesignerPanel(
    private val project: Project, // Keep project for potential future use (MessageBus, services)
    private val designedDialog: DesignedDialog
) : JBPanel<TkinterDesignerPanel>(BorderLayout()) {

    internal val visualCanvasPanel: VisualCanvasPanel

    init {
        // The VisualCanvasPanel needs the project for publishing messages and the dialog for rendering
        visualCanvasPanel = VisualCanvasPanel(project, designedDialog)
        add(visualCanvasPanel, BorderLayout.CENTER)

        // Example: If you wanted a "Generate Code" button specific to this editor instance
        // val generateCodeButton = JButton("Generate Code for this Editor")
        // generateCodeButton.addActionListener {
        //     val pythonCode = TkinterCodeGenerator.generateCode(designedDialog) // Use the instance's dialog
        //     // ... show dialog code ...
        // }
        // val bottomPanel = JBPanel<JBPanel<*>>() // Example panel for editor-specific controls
        // bottomPanel.add(generateCodeButton)
        // add(bottomPanel, BorderLayout.SOUTH)
    }
}

// VisualCanvasPanel constructor needs to be updated to accept Project and DesignedDialog
// This change will be done in a separate step for VisualCanvasPanel.kt
