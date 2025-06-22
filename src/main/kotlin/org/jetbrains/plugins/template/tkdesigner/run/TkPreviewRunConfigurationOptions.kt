package org.jetbrains.plugins.template.tkdesigner.run

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class TkPreviewRunConfigurationOptions : RunConfigurationOptions() {
    private val script: StoredProperty<String?> = string("").provideDelegate(this, "script")
    private val interpreter: StoredProperty<String?> = string("python").provideDelegate(this, "interpreter")

    fun getScript(): String = script.getValue(this) ?: ""
    fun setScript(value: String) { script.setValue(this, value) }
    fun getInterpreter(): String = interpreter.getValue(this) ?: "python"
    fun setInterpreter(value: String) { interpreter.setValue(this, value) }
}
