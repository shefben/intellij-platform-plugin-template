package org.jetbrains.plugins.template.tkdesigner.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Root of a .tkdesign project containing multiple dialogs and resources. */
data class DesignProject(
    val dialogs: MutableList<DialogModel> = mutableListOf(DialogModel()),
    var current: Int = 0,
    val resources: MutableSet<String> = mutableSetOf(),
    var pythonInterpreter: String = "python",
    val translations: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    @Transient var basePath: String = ""
) {
    fun toJson(): String = Gson().toJson(this)

    val activeDialog: DialogModel
        get() = dialogs[current]

    companion object {
        fun fromJson(json: String): DesignProject =
            Gson().fromJson(json, object : TypeToken<DesignProject>() {}.type)
    }
}
