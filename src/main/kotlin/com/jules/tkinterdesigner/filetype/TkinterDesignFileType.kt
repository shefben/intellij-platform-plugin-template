package com.jules.tkinterdesigner.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.IconLoader // For potential icon loading
import javax.swing.Icon

object TkinterDesignFileType : FileType {
    override fun getName(): String = "TkinterDesign"

    override fun getDescription(): @NlsContexts.Label String = "Tkinter Visual Design File"

    override fun getDefaultExtension(): String = "tkdesign"

    override fun getIcon(): Icon? {
        // Placeholder: replace with actual icon later
        // return IconLoader.getIcon("/icons/myFileTypeIcon.svg", TkinterDesignFileType::class.java)
        return null
    }

    override fun isBinary(): Boolean = false // It's a JSON text file

    override fun isReadOnly(): Boolean = false

    override fun getCharset(file: VirtualFile, content: ByteArray): String = Charsets.UTF_8.name()
}
