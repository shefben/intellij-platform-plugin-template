package org.jetbrains.plugins.template.tkdesigner.ui

import org.jetbrains.plugins.template.tkdesigner.model.DesignProject
import javax.swing.*
import java.awt.BorderLayout
import javax.swing.table.DefaultTableModel

class TranslationManagerDialog(private val project: DesignProject) : JDialog() {
    init {
        title = "Translations"
        layout = BorderLayout()
        val languages = project.translations.keys.toMutableSet()
        if (languages.isEmpty()) languages.add("en")
        val keys = project.translations.values.flatMap { it.keys }.toMutableSet()
        val model = DefaultTableModel()
        model.addColumn("Key", keys.toTypedArray())
        languages.forEach { lang ->
            val colData = keys.map { project.translations[lang]?.get(it) ?: "" }.toTypedArray()
            model.addColumn(lang, colData)
        }
        val table = JTable(model)
        add(JScrollPane(table), BorderLayout.CENTER)
        val addLang = JButton("Add Language")
        addLang.addActionListener {
            val lang = JOptionPane.showInputDialog(this, "Language code", "")
            if (!lang.isNullOrBlank()) {
                val colData = keys.map { "" }.toTypedArray()
                model.addColumn(lang, colData)
                project.translations[lang] = mutableMapOf()
            }
        }
        val save = JButton("Save")
        save.addActionListener {
            keys.clear()
            for (row in 0 until model.rowCount) {
                val key = model.getValueAt(row, 0)?.toString() ?: continue
                keys.add(key)
                for (col in 1 until model.columnCount) {
                    val lang = model.getColumnName(col)
                    val value = model.getValueAt(row, col)?.toString() ?: ""
                    project.translations.getOrPut(lang) { mutableMapOf() }[key] = value
                }
            }
            isVisible = false
        }
        val bottom = JPanel().apply { add(addLang); add(save) }
        add(bottom, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(null)
    }
}
