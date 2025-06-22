package org.jetbrains.plugins.template.tkdesigner.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class TkPreviewRunConfiguration(project: Project, factory: ConfigurationFactory, name: String)
    : RunConfigurationBase<TkPreviewRunConfigurationOptions>(project, factory, name) {

    override fun getOptions(): TkPreviewRunConfigurationOptions = super.getOptions() as TkPreviewRunConfigurationOptions

    var script: String
        get() = options.getScript()
        set(value) { options.setScript(value) }

    var interpreter: String
        get() = options.getInterpreter()
        set(value) { options.setInterpreter(value) }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = TkPreviewSettingsEditor()

    override fun getState(@NotNull executor: Executor, @NotNull environment: ExecutionEnvironment): RunProfileState? {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val cmd = GeneralCommandLine(interpreter, script)
                val handler: OSProcessHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }
}
