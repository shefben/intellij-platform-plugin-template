package com.jules.tkinterdesigner.model

import java.awt.Color // Import Color at the top

data class PropertyDefinition(
    val name: String,
    val type: Class<*>,
    val defaultValue: Any,
    val options: List<String>? = null,
    val tkVariableTypeOptions: List<String>? = null,
    val tkVariableDefaultValueProperty: String? = null,
    val isCommandCallback: Boolean = false,
    val isPaneOption: Boolean = false // New field for PanedWindow pane options
)

object WidgetPropertyRegistry {
    val propertiesForType: MutableMap<String, List<PropertyDefinition>> = mutableMapOf()

    init {
        val commonProperties = listOf(PropertyDefinition("name", String::class.java, "defaultName"))
        val tkVarTypes = listOf("StringVar", "IntVar", "DoubleVar", "BooleanVar")

        propertiesForType["ttk.Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("takefocus", Boolean::class.java, true),
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )
        propertiesForType["Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("takefocus", Boolean::class.java, true),
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )
        propertiesForType["ttk.Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text")
        )
        propertiesForType["Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text")
        )
        propertiesForType["ttk.Entry"] = commonProperties + listOf(
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text")
        )
        propertiesForType["Entry"] = commonProperties + listOf(
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text")
        )
        propertiesForType["ttk.Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat", options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove"))
        )
        propertiesForType["Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat", options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove")),
            PropertyDefinition("background", Color::class.java, Color.LIGHT_GRAY)
        )
        propertiesForType["ttk.Notebook"] = commonProperties + listOf(
            PropertyDefinition("width", Int::class.java, 200),
            PropertyDefinition("height", Int::class.java, 200),
            PropertyDefinition("activeTabIndex", Int::class.java, -1)
        )
        propertiesForType["ttk.Progressbar"] = commonProperties + listOf(
            PropertyDefinition("orient", String::class.java, "horizontal", options = listOf("horizontal", "vertical")),
            PropertyDefinition("length", Int::class.java, 100),
            PropertyDefinition("mode", String::class.java, "determinate", options = listOf("determinate", "indeterminate")),
            PropertyDefinition("maximum", Int::class.java, 100),
            PropertyDefinition("value", Int::class.java, 0)
        )
        propertiesForType["ttk.Separator"] = commonProperties + listOf(
            PropertyDefinition("orient", String::class.java, "horizontal", options = listOf("horizontal", "vertical"))
        )
        propertiesForType["Canvas"] = commonProperties + listOf(
            PropertyDefinition("background", Color::class.java, Color.WHITE),
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat", options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove"))
        )
        propertiesForType["Text"] = commonProperties + listOf(
            PropertyDefinition("wrap", String::class.java, "char", options = listOf("none", "char", "word")),
            PropertyDefinition("undo", Boolean::class.java, false),
            PropertyDefinition("state", String::class.java, "normal", options = listOf("normal", "disabled")),
            PropertyDefinition("width", Int::class.java, 80),
            PropertyDefinition("height", Int::class.java, 24)
        )
        propertiesForType["Scrollbar"] = commonProperties + listOf(
            PropertyDefinition("orient", String::class.java, "vertical", options = listOf("horizontal", "vertical")),
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )
        propertiesForType["ttk.Checkbutton"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Checkbutton"),
            PropertyDefinition("variable", String::class.java, "", tkVariableTypeOptions = tkVarTypes.filter { it == "IntVar" || it == "BooleanVar" || it == "StringVar" }, tkVariableDefaultValueProperty = "text"),
            PropertyDefinition("onvalue", Any::class.java, 1),
            PropertyDefinition("offvalue", Any::class.java, 0),
            PropertyDefinition("indicatoron", Boolean::class.java, true),
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )

        val panedWindowProperties = commonProperties + listOf(
            PropertyDefinition("orient", String::class.java, "horizontal", options = listOf("horizontal", "vertical")),
            PropertyDefinition("sashwidth", Int::class.java, 2), // Default in Tk for tk.Panedwindow is 2
            PropertyDefinition("sashrelief", String::class.java, "flat", options = listOf("flat", "raised", "sunken", "ridge", "groove")),
            PropertyDefinition("showhandle", Boolean::class.java, false) // For tk.PanedWindow
        )
        propertiesForType["tk.PanedWindow"] = panedWindowProperties // Add tk.PanedWindow
        propertiesForType["ttk.PanedWindow"] = panedWindowProperties // ttk.PanedWindow might ignore showhandle

        // Pane-specific options
        val paneSpecificProperties = listOf(
            PropertyDefinition("weight", Int::class.java, 1, isPaneOption = true)
            // Add other common pane options like 'sticky', 'padx', 'pady' if desired for PanedWindow children.
            // These would also need isPaneOption = true.
        )
        propertiesForType["_PaneOptions"] = paneSpecificProperties // Special key for these options
    }
}

// Also add "tk.PanedWindow" and "ttk.PanedWindow" to WidgetPaletteInfo
object WidgetPaletteInfo {
    val WIDGETS = listOf(
        "ttk.Button", "ttk.Label", "ttk.Entry", "ttk.Frame",
        "Button", "Label", "Entry", "Frame",
        "ttk.Notebook", "ttk.Progressbar", "ttk.Separator",
        "Canvas", "Text", "Scrollbar", "ttk.Checkbutton",
        "tk.PanedWindow", "ttk.PanedWindow" // Added PanedWindows
    ).distinct()
}
