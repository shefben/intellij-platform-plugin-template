package org.jetbrains.plugins.template.tkdesigner

import org.junit.Assert.*
import org.junit.Test

class DiffTest {
    @Test
    fun basicDiff() {
        val left = """{"width":100,"height":200}"""
        val right = """{"width":120,"height":200}"""
        val tool = TkdesignDiffTool()
        val entries = tool.collectDiff(left, right)
        assertEquals(1, entries.size)
        assertEquals("width", entries[0].path)
        assertEquals(100.0, entries[0].left)
        assertEquals(120.0, entries[0].right)
    }
}
