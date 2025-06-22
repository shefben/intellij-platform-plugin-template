package org.jetbrains.plugins.template.tkdesigner.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class TkPreviewSettingsEditor : SettingsEditor<TkPreviewRunConfiguration>() {
    private val scriptField = TextFieldWithBrowseButton()
    private val interpreterField = TextFieldWithBrowseButton()

    init {
        scriptField.addBrowseFolderListener("Script", null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
        interpreterField.addBrowseFolderListener("Interpreter", null, null, FileChooserDescriptorFactory.createSingleFileDescriptor())
    }

    private val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Script", scriptField)
        .addLabeledComponent("Interpreter", interpreterField)
        .panel

    override fun resetEditorFrom(s: TkPreviewRunConfiguration) {
        scriptField.text = s.script
        interpreterField.text = s.interpreter
    }

    override fun applyEditorTo(s: TkPreviewRunConfiguration) {
        s.script = scriptField.text
        s.interpreter = interpreterField.text
    }

    override fun createEditor(): JComponent = panel
}
