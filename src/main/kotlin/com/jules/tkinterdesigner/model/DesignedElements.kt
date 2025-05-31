package com.jules.tkinterdesigner.model

import kotlinx.serialization.Serializable
// import kotlinx.serialization.Transient // Not used yet, but good for future exclusion

@Serializable
data class DesignedWidget(
    var id: String,
    var type: String,
    var parentId: String? = null,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var properties: MutableMap<String, String> = mutableMapOf() // CHANGED to Map<String, String>
)

@Serializable
data class DesignedDialog(
    var title: String = "My Dialog",
    var width: Int = 400,
    var height: Int = 300,
    val widgets: MutableList<DesignedWidget> = mutableListOf(),
    var version: String = "1.0" // Added version field
)

object WidgetPaletteInfo {
    val WIDGETS = listOf(
        "ttk.Button", "ttk.Label", "ttk.Entry", "ttk.Frame",
        "Button", "Label", "Entry", "Frame",
        "ttk.Notebook", "ttk.Progressbar", "ttk.Separator",
        "Canvas", "Text", "Scrollbar", "ttk.Checkbutton" // Added Checkbutton here as well
    ).distinct()
}

// UserData key for storing DesignedDialog in VirtualFile
val DESIGNED_DIALOG_KEY = com.intellij.openapi.util.Key.create<DesignedDialog>("com.jules.tkinterdesigner.designedDialog")
