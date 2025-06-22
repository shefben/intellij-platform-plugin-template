package org.jetbrains.plugins.template.tkdesigner.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class TkPreviewConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "TkPreviewConfiguration"

    override fun createTemplateConfiguration(@NotNull project: Project): RunConfiguration {
        return TkPreviewRunConfiguration(project, this, "Tkinter Preview")
    }

    override fun getOptionsClass(): Class<out BaseState> = TkPreviewRunConfigurationOptions::class.java
}
