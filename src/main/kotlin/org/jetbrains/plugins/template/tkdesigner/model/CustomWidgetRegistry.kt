package org.jetbrains.plugins.template.tkdesigner.model

import javax.swing.JComponent

object CustomWidgetRegistry {
    private val factories = mutableMapOf<String, () -> JComponent>()

    init {
        CustomWidgetExtensionLoader.load(this)
    }

    fun register(name: String, factory: () -> JComponent) {
        factories[name] = factory
    }

    fun create(name: String): JComponent? = factories[name]?.invoke()
}
