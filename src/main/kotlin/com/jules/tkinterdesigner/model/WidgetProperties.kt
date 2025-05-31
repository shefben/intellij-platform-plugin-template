package com.jules.tkinterdesigner.model

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
            // Width and Height are often managed by layout managers or direct geometry,
            // but can be exposed as properties if needed for specific Tkinter settings.
            // For now, we'll rely on the DesignedWidget's width/height for direct manipulation.
            // PropertyDefinition("width", Int::class.java, 0),
            // PropertyDefinition("height", Int::class.java, 0)
        )

        propertiesForType["ttk.Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button")
        )
        propertiesForType["Button"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Button")
        )

        propertiesForType["ttk.Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label")
        )
        propertiesForType["Label"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Label")
        )

        propertiesForType["ttk.Entry"] = commonProperties + listOf(
            // Entry usually doesn't have a "text" property, but a "textvariable"
            // For simplicity, we might map "text" to its initial content or a variable later.
            // PropertyDefinition("textvariable", String::class.java, "")
        )
        propertiesForType["Entry"] = commonProperties + listOf()


        propertiesForType["ttk.Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat",
                options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove"))
        )
        propertiesForType["Frame"] = commonProperties + listOf(
            PropertyDefinition("borderwidth", Int::class.java, 0),
            PropertyDefinition("relief", String::class.java, "flat",
                options = listOf("flat", "raised", "sunken", "solid", "ridge", "groove"))
        )
    }
}
