package com.cylorun.gui.editor;

import com.cylorun.gui.components.ColorPicker;
import com.cylorun.gui.components.MultiChoiceOptionField;
import com.cylorun.gui.components.TextEditor;
import com.cylorun.gui.components.TextOptionField;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.JSONUtil;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BasicEditorView extends EditorView {

    private final MultiChoiceOptionField columnField;
    private TextOptionField valueField;
    private TextEditor textEditorField;
    private ColorPicker colorChooser;
    private JsonObject runData;
    private Consumer<Boolean> onChange;
    private Color prevColor = Color.WHITE;

    // the record won't be null, while the runData will need to be fetched later
    public BasicEditorView(JsonObject runData, JsonObject runRecord, Consumer<Boolean> onChange) {
        TrackerOptions options = TrackerOptions.getInstance();

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.onChange = onChange;
        this.runData = runData;
        this.setLayout(new BorderLayout());

        this.colorChooser = new ColorPicker();
        this.textEditorField = new TextEditor((val) -> this.checkForChanges());
        this.colorChooser.addConsumer((newCol -> this.checkForChanges()));

            this.columnField = new MultiChoiceOptionField(new String[]{}, "run_id", "Column", (val) -> {
                this.valueField.setVisible(false);
                this.textEditorField.setVisible(false);
                if (val.equals("notes")) {
                    this.textEditorField.setValue(JSONUtil.getOptionalString(this.runData, val).orElse(""));
                    this.textEditorField.setVisible(true);
                } else {
                    this.valueField.setValue(JSONUtil.getOptionalString(this.runData, val).orElse(""));
                    this.valueField.setVisible(true);
                }
                this.checkForChanges();
            });

            this.valueField = new TextOptionField("Value", this.runData.get("run_id").getAsString(), (val) -> {
                if (this.runData == null) {
                    return;
                }
                this.checkForChanges();
            });


        this.add(this.columnField);
        this.add(this.valueField);
        this.add(this.textEditorField);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        this.add(new JLabel("Run Color"));
        this.add(this.colorChooser);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        this.add(Box.createVerticalStrut(10));


    }

    public void setRunData(JsonObject runData) {
        this.runData = runData;
        List<String> keys = new ArrayList<>();
        RunEditor.extractKeys(this.runData, keys);
        String[] values = keys.toArray(new String[0]);
        this.columnField.setOptions(values);
    }

    private void checkForChanges() {
        boolean hasColorChanged = !this.prevColor.equals(this.colorChooser.getCurrentColor());
        boolean hasValueChanged = !this.valueField.getValue().equals(JSONUtil.getOptionalString(this.runData, this.columnField.getValue()).orElse(""));
        this.onChange.accept(hasColorChanged || hasValueChanged); // returns if the save button should be enabled
    }
}
