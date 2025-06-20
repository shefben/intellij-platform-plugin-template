package org.jetbrains.plugins.template.tkdesigner.model

/**
 * Represents a single widget in the designer.
 */
data class WidgetModel(
    val type: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var layout: String = "place",
    val properties: MutableMap<String, String> = mutableMapOf(),
    val events: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<WidgetModel> = mutableListOf(),
    @Transient var parent: WidgetModel? = null
)
