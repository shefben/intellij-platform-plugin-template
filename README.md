# Visual Tkinter Designer

<!-- Plugin description -->
Visual Tkinter Designer is an IntelliJ IDEA plugin providing a WYSIWYG editor for creating Tkinter dialogs. Drag widgets from the floating palette onto the design surface and adjust their properties in the sidebar. Designs are stored in `.tkdesign` files and can be exported to pure Python.
<!-- Plugin description end -->

## Features

- Drag-and-drop placement with resize handles
- Compact palette showing common Tkinter widgets with icons
- IntelliJ-style toolbar with actions for loading, saving and previewing dialogs
- Sidebar editor for widget and dialog attributes
- Multi-selection with alignment and grouping commands
- Undo/redo history and keyboard shortcuts
- Import existing Tkinter scripts
- Preview dialogs with a chosen Python interpreter
- Version-control diff viewer for `.tkdesign` files highlighting changed properties with color-coded cells
- Palette customization and support for ttk widgets
- Property panel with color pickers, font choosers, and file browsers
- Property panel categories with search filtering and collapsible sections
- Start from predefined templates when adding a dialog
- Drag-and-drop hierarchy tree with rename and delete
- Translation manager for editing localized strings
- Settings dialog to adjust grid size, palette columns and keyboard shortcuts

## Building

Use the Gradle wrapper to build the plugin:

```bash
./gradlew buildPlugin -x signPlugin --no-daemon
```

On Windows you can run `compilePlugin.bat` instead.

The plugin archive will be created in `build/distributions`.

## Usage

Install the plugin and choose **Tools | Open Tkinter Designer**.  A blank design surface appears.

1. Drag a widget type from the floating palette onto the canvas and drag to size it.
2. Select the widget to edit its attributes in the right-hand sidebar.
3. Use the toolbar to load/save `.tkdesign` files, align widgets, undo/redo, and preview.
4. Click **Generate** when you are ready to obtain the Python code.

The **Preview** action creates a temporary *Tkinter Preview* run configuration so the dialog can be executed or debugged immediately.  Global options such as grid size or palette columns are available in **Settings | Tools | Tkinter Designer**.
