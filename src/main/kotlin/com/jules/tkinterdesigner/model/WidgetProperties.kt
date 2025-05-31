package com.jules.tkinterdesigner.model

import java.awt.Color // Import Color at the top

data class PropertyDefinition(
    val name: String,
    val type: Class<*>,
    val defaultValue: Any,
    val options: List<String>? = null // For String types with predefined choices (e.g., relief)
)

object WidgetPropertyRegistry {
    val propertiesForType: MutableMap<String, List<PropertyDefinition>> = mutableMapOf()

    init {
        // Common properties for all widgets
        val commonProperties = listOf(
            PropertyDefinition("name", String::class.java, "defaultName")
        )

        propertiesForType["ttk.Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("takefocus", Boolean::class.java, true) // Added boolean
        )
        propertiesForType["Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, ""),
            PropertyDefinition("takefocus", Boolean::class.java, true) // Added boolean
        )

        propertiesForType["ttk.Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, "")
        )
        propertiesForType["Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label"),
            PropertyDefinition("font", String::class.java, "TkDefaultFont"),
            PropertyDefinition("image", String::class.java, "")
        )

        propertiesForType["ttk.Entry"] = commonProperties + listOf()
        propertiesForType["Entry"] = commonProperties + listOf()

        propertiesForType["ttk.Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat",
                options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove"))
        )
        propertiesForType["Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat",
                options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove")),
            PropertyDefinition("background", Color::class.java, Color.LIGHT_GRAY)
        )

        propertiesForType["ttk.Notebook"] = commonProperties + listOf(
            PropertyDefinition("width", Int::class.java, 200),
            PropertyDefinition("height", Int::class.java, 200)
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
            PropertyDefinition("undo", Boolean::class.java, false), // Existing boolean
            PropertyDefinition("state", String::class.java, "normal", options = listOf("normal", "disabled")),
            PropertyDefinition("width", Int::class.java, 80),
            PropertyDefinition("height", Int::class.java, 24)
        )

        propertiesForType["Scrollbar"] = commonProperties + listOf(
            PropertyDefinition("orient", String::class.java, "vertical", options = listOf("horizontal", "vertical")),
            PropertyDefinition("command", String::class.java, "")
        )

        // Added ttk.Checkbutton
        propertiesForType["ttk.Checkbutton"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Checkbutton"),
            PropertyDefinition("variable", String::class.java, ""),
            PropertyDefinition("onvalue", Any::class.java, 1),
            PropertyDefinition("offvalue", Any::class.java, 0),
            PropertyDefinition("indicatoron", Boolean::class.java, true) // Boolean property
        )
    }
}
