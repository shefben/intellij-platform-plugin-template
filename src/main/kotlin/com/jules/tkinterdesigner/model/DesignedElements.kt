package com.jules.tkinterdesigner.model

data class DesignedWidget(
    var id: String,
    var type: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var properties: MutableMap<String, Any> = mutableMapOf(),
    var parentId: String? = null // Added for parenting
)

data class DesignedDialog(
    var title: String = "My Dialog",
    var width: Int = 400,
    var height: Int = 300,
    val widgets: MutableList<DesignedWidget> = mutableListOf()
)

object WidgetPaletteInfo {
    val WIDGETS = listOf(
        "ttk.Button", "ttk.Label", "ttk.Entry", "ttk.Frame",
        "Button", "Label", "Entry", "Frame", // Classic Tk for comparison
        "ttk.Notebook", "ttk.Progressbar", "ttk.Separator",
        "Canvas", "Text", "Scrollbar" // tk.Scrollbar
    ).distinct() // Ensure no duplicates if manually adding more later
}

// UserData key for storing DesignedDialog in VirtualFile
val DESIGNED_DIALOG_KEY = com.intellij.openapi.util.Key.create<DesignedDialog>("com.jules.tkinterdesigner.designedDialog")
