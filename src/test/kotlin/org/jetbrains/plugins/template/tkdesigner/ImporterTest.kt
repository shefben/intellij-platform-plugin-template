package org.jetbrains.plugins.template.tkdesigner

import org.junit.Assert.*
import org.junit.Test

class ImporterTest {
    @Test
    fun importPlace() {
        val script = """
import tkinter as tk
root = tk.Tk()
btn = tk.Button(root); btn.place(x=5, y=10, width=40, height=20)
"""
        val dlg = TkinterImporter.importScript(script)
        assertEquals(1, dlg.widgets.size)
        val btn = dlg.widgets[0]
        assertEquals("Button", btn.type)
        assertEquals(5, btn.x)
        assertEquals(10, btn.y)
        assertEquals(40, btn.width)
        assertEquals(20, btn.height)
        assertEquals("place", btn.layout)
    }

    @Test
    fun importPackAndGrid() {
        val script = """
import tkinter as tk
root = tk.Tk()
frm = tk.Frame(root); frm.pack(side='left')
btn = tk.Button(frm); btn.grid(row=1, column=2)
"""
        val dlg = TkinterImporter.importScript(script)
        assertEquals(2, dlg.widgets.size)
        assertEquals("Frame", dlg.widgets[0].type)
        assertEquals("pack", dlg.widgets[0].layout)
        assertEquals("Button", dlg.widgets[1].type)
        assertEquals("grid", dlg.widgets[1].layout)
    }
}
