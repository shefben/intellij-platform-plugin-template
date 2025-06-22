package org.jetbrains.plugins.template.tkdesigner.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Model representing a dialog composed of Tkinter widgets.
 */
data class DialogModel(
    var width: Int = 400,
    var height: Int = 300,
    var layout: String = "place",
    val widgets: MutableList<WidgetModel> = mutableListOf()
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): DialogModel =
            Gson().fromJson(json, object : TypeToken<DialogModel>() {}.type)
    }
}
