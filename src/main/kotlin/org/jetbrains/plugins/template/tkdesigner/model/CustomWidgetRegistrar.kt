package org.jetbrains.plugins.template.tkdesigner.model

import com.intellij.openapi.extensions.ExtensionPointName

interface CustomWidgetRegistrar {
    fun register(registry: CustomWidgetRegistry)
}

object CustomWidgetExtensionLoader {
    private val EP = ExtensionPointName<CustomWidgetRegistrar>("org.jetbrains.plugins.tkdesigner.customWidget")
    fun load(registry: CustomWidgetRegistry) {
        EP.extensionList.forEach { it.register(registry) }
    }
}
