package com.cylorun.gui.editor;

import com.cylorun.io.TrackerOptions;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.function.Consumer;

public class EditorView extends JPanel {
    public EditorView() {

    }

    public static EditorView getView(JsonObject runData, Consumer<Boolean> onChange) {
        return TrackerOptions.getInstance().advanced_editor_view ? new AdvancedEditorView(runData, onChange) : new BasicEditorView(runData, onChange);
    }
}
