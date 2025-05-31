package com.jules.tkinterdesigner.model

import java.awt.Color // Import Color at the top

data class PropertyDefinition(
    val name: String,
    val type: Class<*>, // For the primary value (e.g., name of the Tk variable)
    val defaultValue: Any,
    val options: List<String>? = null,
    val tkVariableTypeOptions: List<String>? = null,
    val tkVariableDefaultValueProperty: String? = null,
    val isCommandCallback: Boolean = false // New field for command properties
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

        val tkVarTypes = listOf("StringVar", "IntVar", "DoubleVar", "BooleanVar")

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
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text") // Assuming "text" could be a placeholder for initial value
        )
        propertiesForType["Entry"] = commonProperties + listOf(
            PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text")
        )

        // Example for ttk.Combobox (if added to palette later)
        // propertiesForType["ttk.Combobox"] = commonProperties + listOf(
        //     PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text"),
        //     PropertyDefinition("values", String::class.java, "") // Typically a list or string of space-separated values
        // )

        // Example for ttk.Spinbox (if added to palette later)
        // propertiesForType["ttk.Spinbox"] = commonProperties + listOf(
        //    PropertyDefinition("textvariable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text"),
        //     PropertyDefinition("from", Double::class.java, 0.0), // Note: type is Double for from_
        //     PropertyDefinition("to", Double::class.java, 100.0),
        //     PropertyDefinition("values", String::class.java, "") // Alternative to from/to
        // )


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
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )

        // Added ttk.Checkbutton
        propertiesForType["ttk.Checkbutton"] = commonProperties + listOf(
            PropertyDefinition("text", String::class.java, "Checkbutton"),
            PropertyDefinition("variable", String::class.java, "", tkVariableTypeOptions = tkVarTypes.filter { it == "IntVar" || it == "BooleanVar" || it == "StringVar" }, tkVariableDefaultValueProperty = "text"),
            PropertyDefinition("onvalue", Any::class.java, 1),
            PropertyDefinition("offvalue", Any::class.java, 0),
            PropertyDefinition("indicatoron", Boolean::class.java, true),
            PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        )

        // Example for ttk.Radiobutton (add to palette if desired)
        // propertiesForType["ttk.Radiobutton"] = commonProperties + listOf(
        //     PropertyDefinition("text", String::class.java, "Radiobutton"),
        //     PropertyDefinition("variable", String::class.java, "", tkVariableTypeOptions = tkVarTypes, tkVariableDefaultValueProperty = "text"),
        //     PropertyDefinition("value", Any::class.java, "radioButtonValue"), // This specific value for this radio
        //     PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        // )

        // Example for tk.Scale (if added to palette later)
        // propertiesForType["Scale"] = commonProperties + listOf(
        //    PropertyDefinition("variable", String::class.java, "", tkVariableTypeOptions = tkVarTypes.filter { it == "IntVar" || it == "DoubleVar" }),
        //    PropertyDefinition("from_", Double::class.java, 0.0), // Note: Tk uses "from_" due to Python keyword
        //    PropertyDefinition("to", Double::class.java, 100.0),
        //    PropertyDefinition("orient", String::class.java, "horizontal", options=listOf("horizontal", "vertical")),
        //    PropertyDefinition("command", String::class.java, "", isCommandCallback = true)
        // )

        // Ensure Listbox example also has command if applicable (though less common for Listbox itself)
        // propertiesForType["Listbox"] = commonProperties + listOf(
        //    PropertyDefinition("listvariable", String::class.java, "", tkVariableTypeOptions = listOf("StringVar")),
        //    PropertyDefinition("command", String::class.java, "", isCommandCallback = true) // e.g. for double-click
        // )

    }
}
