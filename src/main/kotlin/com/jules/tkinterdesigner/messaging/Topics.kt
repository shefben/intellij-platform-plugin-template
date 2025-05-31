package com.jules.tkinterdesigner.messaging

import com.intellij.util.messages.Topic
import com.jules.tkinterdesigner.model.DesignedWidget
import com.jules.tkinterdesigner.model.DesignedDialog // Potentially needed if events carry dialog context

/**
 * Listener for when a widget is selected or deselected in the VisualCanvasPanel.
 * If widget is null, it means selection was cleared.
 */
interface WidgetSelectionListener {
    fun widgetSelected(widget: DesignedWidget?, dialog: DesignedDialog?) // Added dialog context
}

val WIDGET_SELECTION_TOPIC = Topic.create("TkinterDesigner.WidgetSelected", WidgetSelectionListener::class.java)

/**
 * Listener for when a widget's properties have been modified (e.g., by the PropertyEditor).
 */
interface WidgetPropertyListener {
    fun propertyChanged(widget: DesignedWidget, dialog: DesignedDialog?) // Added dialog context
}

val WIDGET_MODIFIED_TOPIC = Topic.create("TkinterDesigner.WidgetModified", WidgetPropertyListener::class.java)
