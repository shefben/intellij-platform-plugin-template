package com.jules.tkinterdesigner.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jules.tkinterdesigner.TkinterDesignerBundle

@Service(Service.Level.PROJECT)
class TkinterProjectService(project: Project) {

    init {
        thisLogger().info(TkinterDesignerBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
}
