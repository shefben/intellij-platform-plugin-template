package com.jules.tkinterdesigner.editor.toolwindows

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.*

class FontChooserDialog(parent: Component?, private val initialFontString: String?) : DialogWrapper(parent, false) {
    private val availableFontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
    private val familyComboBox = JComboBox(availableFontFamilies)
    private val sizeSpinner = JSpinner(SpinnerNumberModel(12, 6, 72, 1)) // Default 12, min 6, max 72, step 1
    private val boldCheckBox = JBCheckBox("Bold")
    private val italicCheckBox = JBCheckBox("Italic")

    private var selectedFontString: String? = null

    init {
        title = "Choose Font"
        parseInitialFontString()
        init() // From DialogWrapper
    }

    private fun parseInitialFontString() {
        initialFontString?.let {
            val parts = it.split(" ").map { part -> part.trim() }.filter { part -> part.isNotEmpty() }
            if (parts.isEmpty()) return

            var style = 0
            val styles = mutableListOf<String>()
            var sizeVal: Int? = null
            val familyParts = mutableListOf<String>()

            for (part in parts.reversed()) { // Parse from end for style and size
                if (part.equals("bold", ignoreCase = true)) {
                    style = style or Font.BOLD
                    styles.add("bold")
                    boldCheckBox.isSelected = true
                } else if (part.equals("italic", ignoreCase = true) || part.equals("oblique", ignoreCase = true)) {
                    style = style or Font.ITALIC
                    styles.add("italic")
                    italicCheckBox.isSelected = true
                } else if (sizeVal == null && part.toIntOrNull() != null) {
                    sizeVal = part.toInt()
                    sizeSpinner.value = sizeVal
                } else {
                    familyParts.add(0, part) // Add to beginning to maintain order
                }
            }

            val familyName = familyParts.joinToString(" ").ifEmpty { familyComboBox.font.family }
            familyComboBox.selectedItem = if (availableFontFamilies.contains(familyName)) familyName else availableFontFamilies.firstOrNull()

            if (sizeVal == null) sizeSpinner.value = 12 // Default if not parsed

        } ?: run {
            // Default values if initialFontString is null
            familyComboBox.selectedItem = "Arial" // A common default
            sizeSpinner.value = 12
            boldCheckBox.isSelected = false
            italicCheckBox.isSelected = false
        }
    }

    override fun createCenterPanel(): JComponent {
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Family:"), familyComboBox)
            .addLabeledComponent(JBLabel("Size:"), sizeSpinner)
            .addComponent(boldCheckBox)
            .addComponent(italicCheckBox)
            .panel
        return formPanel
    }

    override fun doOKAction() {
        val family = familyComboBox.selectedItem as? String ?: "Arial"
        val size = sizeSpinner.value as? Int ?: 12
        val styles = mutableListOf<String>()
        if (boldCheckBox.isSelected) styles.add("bold")
        if (italicCheckBox.isSelected) styles.add("italic")

        selectedFontString = "$family $size ${styles.joinToString(" ")}".trim()
        super.doOKAction()
    }

    fun getSelectedFontString(): String? = selectedFontString
}
