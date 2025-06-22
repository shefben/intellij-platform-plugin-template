package org.jetbrains.plugins.template.tkdesigner.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue

class TkPreviewRunConfigurationType : ConfigurationTypeBase(
    ID,
    "Tkinter Preview",
    "Run generated Tkinter dialog",
    NotNullLazyValue.createValue { AllIcons.Actions.Execute }
) {
    init { addFactory(TkPreviewConfigurationFactory(this)) }

    companion object {
        const val ID = "TkPreviewConfiguration"
    }
}
