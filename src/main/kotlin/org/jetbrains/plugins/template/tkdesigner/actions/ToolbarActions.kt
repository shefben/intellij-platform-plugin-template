package org.jetbrains.plugins.template.tkdesigner.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.Messages
import com.intellij.icons.AllIcons
import org.jetbrains.plugins.template.TkDesignerBundle
import org.jetbrains.plugins.template.tkdesigner.TkinterGenerator
import org.jetbrains.plugins.template.tkdesigner.TkinterImporter
import org.jetbrains.plugins.template.tkdesigner.model.DesignProject
import org.jetbrains.plugins.template.tkdesigner.ui.DesignAreaPanel
import org.jetbrains.plugins.template.tkdesigner.ui.PropertyPanel
import org.jetbrains.plugins.template.tkdesigner.ui.TranslationManagerDialog
import org.jetbrains.plugins.template.tkdesigner.DesignerSettings
import kotlin.io.path.createTempFile

class DesignerToolbar(
    private val design: DesignAreaPanel,
    private val projectModel: DesignProject,
    private val properties: PropertyPanel,
    private val project: com.intellij.openapi.project.Project,
    private val dialogChooser: javax.swing.JComboBox<String>,
    private val pythonField: com.intellij.ui.components.JBTextField
) {
    val toolbar = run {
        val group = DefaultActionGroup().apply {
            add(LoadAction())
            add(SaveAction())
            add(GenerateAction())
            add(PreviewAction())
            add(ImportAction())
            addSeparator()
            add(UndoAction()); add(RedoAction())
            addSeparator()
            add(AlignLeftAction()); add(AlignRightAction()); add(AlignTopAction()); add(AlignBottomAction())
            addSeparator(); add(GroupAction())
            addSeparator(); add(EditTranslationsAction()); add(ExportTranslationsAction())
        }
        ActionManager.getInstance().createActionToolbar("TkDesignerToolbar", group, true).apply {
            targetComponent = design
            setLayoutPolicy(com.intellij.openapi.actionSystem.ActionToolbar.NOWRAP_LAYOUT_POLICY)
        }
    }

    inner class LoadAction : AnAction("Load", "Load design", AllIcons.Actions.MenuOpen) {
        override fun actionPerformed(e: AnActionEvent) {
            val path = Messages.showInputDialog(project, "Enter path to load", TkDesignerBundle.message("message.load.title"), null)
            path?.let {
                val file = java.io.File(it)
                if (file.exists()) {
                    val text = file.readText()
                    val proj = kotlin.runCatching { DesignProject.fromJson(text) }.getOrNull()
                    if (proj != null) {
                        projectModel.dialogs.clear(); projectModel.dialogs.addAll(proj.dialogs)
                        projectModel.resources.clear(); projectModel.resources.addAll(proj.resources)
                    } else {
                        val dlg = TkinterImporter.importScript(text)
                        projectModel.dialogs.clear(); projectModel.dialogs.add(dlg)
                        if (TkinterImporter.lastWarnings.isNotEmpty()) {
                            Messages.showWarningDialog(project, TkinterImporter.lastWarnings.joinToString("\n"), TkDesignerBundle.message("message.warning.import"))
                        }
                    }
                    dialogChooser.removeAllItems();
                    projectModel.dialogs.indices.forEach { dialogChooser.addItem("Dialog ${it + 1}") }
                    design.loadModel(projectModel.activeDialog)
                }
            }
        }
    }

    inner class SaveAction : AnAction("Save", "Save design", AllIcons.Actions.MenuSaveall) {
        override fun actionPerformed(e: AnActionEvent) {
            properties.applyChanges()
            val path = Messages.showInputDialog(project, "Enter path to save", TkDesignerBundle.message("message.save.title"), null)
            path?.let {
                val file = java.io.File(if (it.endsWith(".tkdesign")) it else "$it.tkdesign")
                file.writeText(projectModel.toJson())
            }
        }
    }

    inner class GenerateAction : AnAction("Generate", "Copy Python", AllIcons.Actions.Diff) {
        override fun actionPerformed(e: AnActionEvent) {
            properties.applyChanges()
            val code = TkinterGenerator.generate(design.model, projectModel.translations)
            com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection(code))
            Messages.showInfoMessage(project, TkDesignerBundle.message("message.generated"), "Generate")
        }
    }

    inner class PreviewAction : AnAction("Preview", "Run dialog", AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            properties.applyChanges()
            projectModel.pythonInterpreter = pythonField.text
            DesignerSettings.instance().state.interpreter = pythonField.text
            val code = TkinterGenerator.generate(design.model, projectModel.translations)
            val tmp = createTempFile("preview", ".py").toFile().apply { writeText(code) }
            val runManager = com.intellij.execution.RunManager.getInstance(project)
            val type = com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType(
                org.jetbrains.plugins.template.tkdesigner.run.TkPreviewRunConfigurationType::class.java)
            val factory = type.configurationFactories[0]
            val settings = runManager.findConfigurationByName("Tkinter Preview")
                ?: runManager.createConfiguration("Tkinter Preview", factory).also { runManager.addConfiguration(it) }
            val cfg = settings.configuration as org.jetbrains.plugins.template.tkdesigner.run.TkPreviewRunConfiguration
            cfg.script = tmp.absolutePath
            cfg.interpreter = projectModel.pythonInterpreter
            val executor = com.intellij.execution.ExecutorRegistry.getInstance().getExecutorById("Run")
            if (executor != null) com.intellij.execution.ProgramRunnerUtil.executeConfiguration(settings, executor)
        }
    }

    inner class ImportAction : AnAction("Import", "Import Python", AllIcons.Actions.Download) {
        override fun actionPerformed(e: AnActionEvent) {
            val path = Messages.showInputDialog(project, "Python file to import", TkDesignerBundle.message("message.import.title"), null)
            path?.let {
                val file = java.io.File(it)
                if (file.exists()) {
                    val loaded = TkinterImporter.importScript(file.readText())
                    if (TkinterImporter.lastWarnings.isNotEmpty()) {
                        Messages.showWarningDialog(project, TkinterImporter.lastWarnings.joinToString("\n"), TkDesignerBundle.message("message.warning.import"))
                    }
                    projectModel.dialogs.clear(); projectModel.dialogs.add(loaded)
                    dialogChooser.removeAllItems(); dialogChooser.addItem("Dialog 1")
                    design.loadModel(loaded)
                }
            }
        }
    }

    inner class UndoAction : AnAction("Undo", "Undo", AllIcons.Actions.Undo) { override fun actionPerformed(e: AnActionEvent) = design.undo() }
    inner class RedoAction : AnAction("Redo", "Redo", AllIcons.Actions.Redo) { override fun actionPerformed(e: AnActionEvent) = design.redo() }
    inner class AlignLeftAction : AnAction(AllIcons.General.ArrowLeft) { override fun actionPerformed(e: AnActionEvent) = design.alignLeft() }
    inner class AlignRightAction : AnAction(AllIcons.General.ArrowRight) { override fun actionPerformed(e: AnActionEvent) = design.alignRight() }
    inner class AlignTopAction : AnAction(AllIcons.General.ArrowUp) { override fun actionPerformed(e: AnActionEvent) = design.alignTop() }
    inner class AlignBottomAction : AnAction(AllIcons.General.ArrowDown) { override fun actionPerformed(e: AnActionEvent) = design.alignBottom() }
    inner class GroupAction : AnAction("Group", "Group", AllIcons.Actions.GroupByModule) { override fun actionPerformed(e: AnActionEvent) = design.groupSelected() }
    inner class EditTranslationsAction : AnAction("Translations", "Manage translations", AllIcons.Actions.Edit) {
        override fun actionPerformed(e: AnActionEvent) {
            TranslationManagerDialog(projectModel).apply { isModal = true }.isVisible = true
        }
    }
    inner class ExportTranslationsAction : AnAction("Export", "Export", AllIcons.Actions.MenuSaveall) {
        override fun actionPerformed(e: AnActionEvent) {
            val path = Messages.showInputDialog(project, "Save translations to", TkDesignerBundle.message("message.export.title"), null)
            if (!path.isNullOrBlank()) {
                java.io.File(path).writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(projectModel.translations))
            }
        }
    }
}
