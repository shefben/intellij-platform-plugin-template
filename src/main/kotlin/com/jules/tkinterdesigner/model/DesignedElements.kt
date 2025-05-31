package com.jules.tkinterdesigner.model

data class DesignedWidget(
    var id: String,
    var type: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var properties: MutableMap<String, Any> = mutableMapOf()
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
        "Button", "Label", "Entry", "Frame" // Classic Tk for comparison
    )
}
