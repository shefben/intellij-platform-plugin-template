package com.jules.tkinterdesigner.codegen

import com.jules.tkinterdesigner.model.DesignedDialog
import com.jules.tkinterdesigner.model.DesignedWidget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color // For Color property test

class TkinterCodeGeneratorTest {

    // Helper function for cleaner assertions
    private fun assertCodeContains(code: String, vararg snippets: String) {
        for (snippet in snippets) {
            assertTrue(code.contains(snippet), "Code should contain: '$snippet'\nActual code:\n$code")
        }
    }

    private fun assertCodeDoesNotContain(code: String, vararg snippets: String) {
        for (snippet in snippets) {
            assertFalse(code.contains(snippet), "Code should NOT contain: '$snippet'\nActual code:\n$code")
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
            id = "btn1", type = "ttk.Button", x = 10, y = 20, width = 100, height = 30,
            properties = mutableMapOf("name" to "myButton", "text" to "Click Me")
        )
        dialog.widgets.add(button)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "myButton = ttk.Button(root, text=\"Click Me\")",
            "myButton.place(x=10, y=20, width=100, height=30)")
    }

    @Test
    fun testWidgetPropertyFormatting_IntegerAndRelief() {
        val dialog = DesignedDialog(title = "Frame Test", width = 200, height = 200)
        val frame = DesignedWidget(
            id = "frm1", type = "Frame", x = 5, y = 5, width = 150, height = 150,
            properties = mutableMapOf("name" to "infoFrame", "borderwidth" to 2, "relief" to "sunken")
        )
        dialog.widgets.add(frame)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "infoFrame.place(x=5, y=5, width=150, height=150)")
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
            id = "lbl1", type = "ttk.Label", x = 5, y = 5, width = 80, height = 25,
            properties = mutableMapOf("name" to "userLabel", "text" to "Username:")
        )
        val entry = DesignedWidget(
            id = "ent1", type = "ttk.Entry", x = 90, y = 5, width = 100, height = 25,
            properties = mutableMapOf("name" to "userInput")
        )
        dialog.widgets.addAll(listOf(label, entry))
        val code = TkinterCodeGenerator.generateCode(dialog)

        val labelCodeIndex = code.indexOf("userLabel = ttk.Label(root, text=\"Username:\")")
        val labelPlaceIndex = code.indexOf("userLabel.place(x=5, y=5, width=80, height=25)")
        val entryCodeIndex = code.indexOf("userInput = ttk.Entry(root)")
        val entryPlaceIndex = code.indexOf("userInput.place(x=90, y=5, width=100, height=25)")

        assertTrue(labelCodeIndex != -1 && labelPlaceIndex != -1 && entryCodeIndex != -1 && entryPlaceIndex != -1, "A widget part was not found.")
        assertTrue(labelCodeIndex < entryCodeIndex, "Label should be defined before Entry.")
        assertTrue(labelPlaceIndex < entryPlaceIndex, "Label should be placed before Entry.")
        assertTrue(labelCodeIndex < labelPlaceIndex, "Label should be defined before placed.")
        assertTrue(entryCodeIndex < entryPlaceIndex, "Entry should be defined before placed.")
    }

    @Test
    fun testNotebookGeneration() {
        val dialog = DesignedDialog()
        val notebook = DesignedWidget("nb1", "ttk.Notebook", 10, 10, 200, 150, mutableMapOf("name" to "myNotebook"))
        dialog.widgets.add(notebook)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "myNotebook = ttk.Notebook(root)", "myNotebook.place(x=10, y=10, width=200, height=150)")
    }

    @Test
    fun testProgressbarGeneration() {
        val dialog = DesignedDialog()
        val pb = DesignedWidget("pb1", "ttk.Progressbar", 5, 5, 150, 20, mutableMapOf(
            "name" to "downloadProgress", "orient" to "horizontal", "mode" to "determinate", "value" to 75, "maximum" to 100))
        dialog.widgets.add(pb)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "downloadProgress = ttk.Progressbar(root, orient=\"horizontal\", mode=\"determinate\", value=75, maximum=100)")
    }

    @Test
    fun testSeparatorGeneration() {
        val dialog = DesignedDialog()
        val sep = DesignedWidget("sep1", "ttk.Separator", 0, 50, 200, 2, mutableMapOf("name" to "hLine", "orient" to "horizontal"))
        dialog.widgets.add(sep)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "hLine = ttk.Separator(root, orient=\"horizontal\")")
    }

    @Test
    fun testCanvasGeneration() {
        val dialog = DesignedDialog()
        val canvas = DesignedWidget("cv1", "Canvas", 20, 20, 100, 80, mutableMapOf(
            "name" to "myDrawingArea", "background" to "#AABBCC", "borderwidth" to 1, "relief" to "ridge"))
        dialog.widgets.add(canvas)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "myDrawingArea = Canvas(root, background=\"#AABBCC\", borderwidth=1, relief=\"ridge\")")
    }

    @Test
    fun testTextGeneration() {
        val dialog = DesignedDialog()
        val textWidget = DesignedWidget("txt1", "Text", 5, 5, 180, 100, mutableMapOf(
            "name" to "editorArea", "wrap" to "word", "undo" to true, "state" to "normal"))
        dialog.widgets.add(textWidget)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "editorArea = Text(root, wrap=\"word\", undo=True, state=\"normal\")")
    }

    @Test
    fun testScrollbarGeneration() {
        val dialog = DesignedDialog()
        val scroll = DesignedWidget("scr1", "Scrollbar", 190, 5, 10, 100, mutableMapOf(
            "name" to "myScroll", "orient" to "vertical", "command" to "myListbox.yview"))
        dialog.widgets.add(scroll)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "myScroll = Scrollbar(root, orient=\"vertical\", command=\"myListbox.yview\")")
    }

    @Test
    fun testCheckbuttonGeneration() {
        val dialog = DesignedDialog()
        val chk = DesignedWidget("chk1", "ttk.Checkbutton", 10, 120, 150, 25, mutableMapOf(
            "name" to "agreeChk", "text" to "Agree to terms", "variable" to "varAgree", "indicatoron" to false ))
        dialog.widgets.add(chk)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "agreeChk = ttk.Checkbutton(root, text=\"Agree to terms\", variable=\"varAgree\", indicatoron=False)")
    }

    @Test
    fun testFontPropertyGeneration() {
        val dialog = DesignedDialog()
        val label = DesignedWidget("lblFont", "ttk.Label", 5, 5, 100, 25, mutableMapOf(
            "name" to "fontLabel", "text" to "Styled Text", "font" to "Arial 12 bold italic"))
        dialog.widgets.add(label)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "fontLabel = ttk.Label(root, text=\"Styled Text\", font=\"Arial 12 bold italic\")")
    }

    @Test
    fun testColorPropertyGeneration() {
        val dialog = DesignedDialog()
        val frame = DesignedWidget("frmColor", "Frame", 10, 10, 50, 50, mutableMapOf(
            "name" to "colorFrame", "background" to Color(255, 0, 0) )) // Red
        dialog.widgets.add(frame)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "colorFrame = Frame(root, background=\"#ff0000\")")
    }

    @Test
    fun testImagePathPropertyGeneration() {
        val dialog = DesignedDialog()
        val button = DesignedWidget("btnImg", "ttk.Button", 20, 20, 100, 40, mutableMapOf(
            "name" to "imageButton", "image" to "path/to/my_image.png"))
        dialog.widgets.add(button)
        val code = TkinterCodeGenerator.generateCode(dialog)
        val photoImageCreation = "imageButton_image_img = tk.PhotoImage(file=\"path/to/my_image.png\")"
        val buttonInstantiation = "imageButton = ttk.Button(root, image=imageButton_image_img)"
        assertCodeContains(code, photoImageCreation, buttonInstantiation)
        assertTrue(code.indexOf(photoImageCreation) < code.indexOf(buttonInstantiation),
            "PhotoImage should be defined before the widget uses it.")
    }

    @Test
    fun testBooleanPropertyGeneration() {
        val dialog = DesignedDialog()
        val btn = DesignedWidget("btnBool", "ttk.Button", 5, 5, 100, 30, mutableMapOf(
            "name" to "boolButton", "takefocus" to false))
        dialog.widgets.add(btn)
        val code = TkinterCodeGenerator.generateCode(dialog)
        assertCodeContains(code, "boolButton = ttk.Button(root, takefocus=False)")
    }

    @Test
    fun testNestedWidgets_FrameInRoot_ButtonInFrame() {
        val dialog = DesignedDialog(title = "Nested Test", width = 250, height = 150)
        val frame = DesignedWidget(
            id = "frmOuter", type = "Frame", x = 10, y = 10, width = 200, height = 100,
            properties = mutableMapOf("name" to "outerFrame", "relief" to "sunken", "borderwidth" to 2)
        )
        val button = DesignedWidget(
            id = "btnInner", type = "Button", x = 5, y = 5, width = 80, height = 25,
            properties = mutableMapOf("name" to "innerButton", "text" to "Hi"),
            parentId = "frmOuter" // Crucial: button is child of frame
        )
        dialog.widgets.addAll(listOf(frame, button))
        val code = TkinterCodeGenerator.generateCode(dialog)

        // Frame is child of root
        assertCodeContains(code, "outerFrame = Frame(root, relief=\"sunken\", borderwidth=2)")
        assertCodeContains(code, "outerFrame.place(x=10, y=10, width=200, height=100)")

        // Button is child of outerFrame
        assertCodeContains(code, "innerButton = Button(outerFrame, text=\"Hi\")")
        assertCodeContains(code, "innerButton.place(x=5, y=5, width=80, height=25)")

        // Check order
        val frameDefIndex = code.indexOf("outerFrame = Frame(root")
        val buttonDefIndex = code.indexOf("innerButton = Button(outerFrame")
        assertTrue(frameDefIndex != -1 && buttonDefIndex != -1, "Widget definitions not found")
        assertTrue(frameDefIndex < buttonDefIndex, "Parent frame should be defined before child button.")
    }

    @Test
    fun testDeeplyNestedWidgets() {
        val dialog = DesignedDialog(title = "Deep Nested Test", width = 300, height = 200)
        val frame1 = DesignedWidget("f1", "Frame", 10, 10, 280, 180, mutableMapOf("name" to "frame1"))
        val frame2 = DesignedWidget("f2", "Frame", 5, 5, 270, 170, mutableMapOf("name" to "frame2"), parentId = "f1")
        val button = DesignedWidget("b1", "Button", 20, 20, 100, 30, mutableMapOf("name" to "button1", "text" to "Deep"), parentId = "f2")

        dialog.widgets.addAll(listOf(frame1, frame2, button))
        val code = TkinterCodeGenerator.generateCode(dialog)

        assertCodeContains(code, "frame1 = Frame(root)")
        assertCodeContains(code, "frame1.place(x=10, y=10, width=280, height=180)")
        assertCodeContains(code, "frame2 = Frame(frame1)") // frame2 child of frame1
        assertCodeContains(code, "frame2.place(x=5, y=5, width=270, height=170)")
        assertCodeContains(code, "button1 = Button(frame2, text=\"Deep\")") // button1 child of frame2
        assertCodeContains(code, "button1.place(x=20, y=20, width=100, height=30)")

        val f1Idx = code.indexOf("frame1 = Frame(root)")
        val f2Idx = code.indexOf("frame2 = Frame(frame1)")
        val b1Idx = code.indexOf("button1 = Button(frame2")

        assertTrue(f1Idx < f2Idx, "frame1 should be defined before frame2.")
        assertTrue(f2Idx < b1Idx, "frame2 should be defined before button1.")
    }
}
