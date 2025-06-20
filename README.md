# Visual Tkinter Designer

<!-- Plugin description -->
Visual Tkinter Designer is an IntelliJ IDEA plugin providing a WYSIWYG editor for creating Tkinter dialogs. Drag widgets from the floating palette onto the design surface and adjust their properties in the sidebar. Designs are stored in `.tkdesign` files and can be exported to pure Python.
<!-- Plugin description end -->

## Features

- Drag-and-drop placement with resize handles
- Compact palette showing common Tkinter widgets with icons
- Sidebar editor for widget and dialog attributes
- Multi-selection with alignment and grouping commands
- Undo/redo history and keyboard shortcuts
- Import existing Tkinter scripts
- Preview dialogs with a chosen Python interpreter
- Version-control diff viewer for `.tkdesign` files

## Building

Use the Gradle wrapper to build the plugin:

```bash
./gradlew buildPlugin -x signPlugin --no-daemon
```

On Windows you can run `compilePlugin.bat` instead.

The plugin archive will be created in `build/distributions`.

## Usage

Install the plugin and select **Tools | Open Tkinter Designer** to open the tool window. Create or load a `.tkdesign` file, design your dialog, then click **Generate** to copy the Python code equivalent.
