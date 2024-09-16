package com.cylorun.gui.editor;

import com.cylorun.io.TrackerOptions;
import com.cylorun.io.dto.RunRecord;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.function.Consumer;

public class EditorView extends JPanel {
    public EditorView() {

    }

    public static EditorView getView(RunRecord runRecord,, Consumer<Boolean> onChange) {
        return TrackerOptions.getInstance().advanced_editor_view ? new AdvancedEditorView(runRecord, onChange) : new BasicEditorView(runRecord, onChange);
    }
}
