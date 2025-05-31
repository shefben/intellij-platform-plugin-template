package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TkinterCodeGeneratorTest {

    // Helper function for cleaner assertions
    private fun assertCodeContains(code: String, vararg snippets: String) {
        for (snippet in snippets) {
            assertTrue(code.contains(snippet), "Code should contain: $snippet\nActual code:\n$code")
        }
    }

    private fun assertCodeDoesNotContain(code: String, vararg snippets: String) {
        for (snippet in snippets) {
            assertFalse(code.contains(snippet), "Code should NOT contain: $snippet\nActual code:\n$code")
        }
    }

    @Test
    fun testEmptyDialog() {
        val dialog = DesignedDialog(title = "Test Dialog", width = 200, height = 100)
        val code = TkinterCodeGenerator.generateCode(dialog)

        assertCodeContains(
            code,
            "import tkinter as tk",
            "from tkinter import ttk",
            "root = tk.Tk()",
            "root.title(\"Test Dialog\")",
            "root.geometry(\"200x100\")",
            "root.mainloop()"
        )
        // Check that no widget-specific lines are present if a common pattern can be identified
        // For example, check if ".place(" is absent, or if specific variable names are absent.
        // A simple check is to see if the lines between geometry and mainloop are empty or just newlines.
        val coreSetupEnd = code.indexOf("root.geometry(\"200x100\")") + "root.geometry(\"200x100\")".length
        val mainLoopStart = code.indexOf("root.mainloop()")
        if (coreSetupEnd != -1 && mainLoopStart != -1 && mainLoopStart > coreSetupEnd) {
            val between = code.substring(coreSetupEnd, mainLoopStart).trim()
            assertTrue(between.isEmpty(), "Should be no widget code for an empty dialog. Found: '$between'")
        } else {
            fail("Could not properly segment code to check for widget absence.")
        }
    }

    @Test
    fun testSingleButton() {
        val dialog = DesignedDialog(title = "Button Test", width = 300, height = 200)
        val button = DesignedWidget(
            id = "btn1",
            type = "ttk.Button",
            x = 10,
            y = 20,
            width = 100,
            height = 30,
            properties = mutableMapOf("name" to "myButton", "text" to "Click Me")
        )
        dialog.widgets.add(button)
        val code = TkinterCodeGenerator.generateCode(dialog)

        assertCodeContains(
            code,
            "myButton = ttk.Button(root, text=\"Click Me\")",
            "myButton.place(x=10, y=20, width=100, height=30)"
        )
    }

    @Test
    fun testWidgetPropertyFormatting_IntegerAndRelief() {
        val dialog = DesignedDialog(title = "Frame Test", width = 200, height = 200)
        val frame = DesignedWidget(
            id = "frm1",
            type = "Frame", // Using classic Tk Frame for this example
            x = 5,
            y = 5,
            width = 150,
            height = 150,
            properties = mutableMapOf(
                "name" to "infoFrame",
                "borderwidth" to 2,
                "relief" to "sunken"
            )
        )
        dialog.widgets.add(frame)
        val code = TkinterCodeGenerator.generateCode(dialog)

        // Property order can vary in maps, so check for parts or use a regex if order is strict.
        // For now, checking for individual parts is safer.
        assertCodeContains(code, "infoFrame = Frame(root, ") // Start of constructor
        assertCodeContains(code, "borderwidth=2")
        assertCodeContains(code, "relief=\"sunken\"")
        assertCodeContains(code, "infoFrame.place(x=5, y=5, width=150, height=150)")

        // Ensure correct argument separation and closing parenthesis for the constructor
        // This is a bit more fragile, but tests the overall structure.
        // A more robust way would be to parse the generated line if it becomes complex.
        assertTrue(
            code.contains("infoFrame = Frame(root, borderwidth=2, relief=\"sunken\")") ||
            code.contains("infoFrame = Frame(root, relief=\"sunken\", borderwidth=2)"),
            "Frame constructor not formatted as expected. Code:\n$code"
        )
    }

    @Test
    fun testMultipleWidgets_OrderAndNaming() {
        val dialog = DesignedDialog(title = "Multi Widget Test", width = 400, height = 300)
        val label = DesignedWidget(
            id = "lbl1",
            type = "ttk.Label",
            x = 5,
            y = 5,
            width = 80,
            height = 25,
            properties = mutableMapOf("name" to "userLabel", "text" to "Username:")
        )
        val entry = DesignedWidget(
            id = "ent1",
            type = "ttk.Entry",
            x = 90,
            y = 5,
            width = 100,
            height = 25,
            properties = mutableMapOf("name" to "userInput") // Entry might not have 'text' property directly
        )
        dialog.widgets.addAll(listOf(label, entry)) // Order of addition matters for generation order
        val code = TkinterCodeGenerator.generateCode(dialog)

        val labelCodeIndex = code.indexOf("userLabel = ttk.Label(root, text=\"Username:\")")
        val labelPlaceIndex = code.indexOf("userLabel.place(x=5, y=5, width=80, height=25)")
        val entryCodeIndex = code.indexOf("userInput = ttk.Entry(root)") // No extra props for this Entry
        val entryPlaceIndex = code.indexOf("userInput.place(x=90, y=5, width=100, height=25)")

        assertTrue(labelCodeIndex != -1, "Label instantiation not found.")
        assertTrue(labelPlaceIndex != -1, "Label placement not found.")
        assertTrue(entryCodeIndex != -1, "Entry instantiation not found.")
        assertTrue(entryPlaceIndex != -1, "Entry placement not found.")

        // Check order: label stuff should appear before entry stuff
        assertTrue(labelCodeIndex < entryCodeIndex, "Label should be defined before Entry if added first.")
        assertTrue(labelPlaceIndex < entryPlaceIndex, "Label should be placed before Entry if added first.")
        assertTrue(labelCodeIndex < labelPlaceIndex, "Label should be defined before it's placed.")
        assertTrue(entryCodeIndex < entryPlaceIndex, "Entry should be defined before it's placed.")
    }
}
